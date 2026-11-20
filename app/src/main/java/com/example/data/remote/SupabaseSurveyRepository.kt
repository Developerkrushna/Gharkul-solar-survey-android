package com.example.data.remote

import android.content.Context
import com.example.model.CustomerSurvey
import com.example.model.ExecutiveUser
import com.example.model.InstallationReport
import com.example.model.LoginRole
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class AccessRevokedException(message: String) : IllegalStateException(message)

data class CloudSyncSnapshot(
  val customVillages: List<CustomVillageRecord>,
  val surveys: List<CustomerSurvey>,
  val installationReports: List<InstallationReport>,
  val refreshedExecutive: ExecutiveUser? = null,
)

sealed class SessionRestoreResult {
  data object NoSession : SessionRestoreResult()

  data class RestoredOnline(
    val session: PersistedSupabaseSession,
    val snapshot: CloudSyncSnapshot,
  ) : SessionRestoreResult()

  data class RestoredOfflineFallback(
    val session: PersistedSupabaseSession,
    val offlineMessageMr: String,
  ) : SessionRestoreResult()
}

class SupabaseSurveyRepository(
  context: Context,
  val settings: SupabaseSettings,
  httpClient: OkHttpClient? = null,
) {
  companion object {
    const val ACCESS_REVOKED_MESSAGE_MR =
      "तुमचा मोबाईल प्रवेश ऑफिस अ‍ॅडमिनने बंद केला आहे किंवा तुमची भूमिका बदलली आहे. कृपया ऑफिसशी संपर्क साधा."
    const val ADMIN_MOBILE_BLOCKED_MESSAGE_MR =
      "ऑफिस अ‍ॅडमिन (ADMIN) खाते मोबाईल अ‍ॅपमध्ये लॉगिन करता येत नाही. कृपया ऑफिस वेब डॅशबोर्ड वापरा."
    const val OFFLINE_RESTORE_MESSAGE_MR =
      "इंटरनेट उपलब्ध नसल्याने मागील लॉगिन सुरू ठेवले आहे. इंटरनेट जोडल्यावर वरून खाली स्वाइप करून सिंक करा."
  }

  private val client: OkHttpClient =
    httpClient
      ?: OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(
          HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        )
        .build()

  private val sessionStore = SupabaseSessionStore(context.applicationContext)
  private val api = SupabaseApiClient(settings, client)
  private val r2Uploader = CloudflareR2Uploader(context.applicationContext, settings, client)

  @Volatile
  private var currentSession: PersistedSupabaseSession? = null

  val isEnabled: Boolean
    get() = settings.isConfigured

  fun hasSavedSessionFast(): Boolean = sessionStore.hasSavedSessionFast()

  suspend fun saveOfflineDemoSession(role: LoginRole, executive: ExecutiveUser) {
    val saved =
      PersistedSupabaseSession(
        accessToken = "",
        refreshToken = "",
        loginRole = role,
        executive = executive,
        isOfflineDemo = true,
      )
    currentSession = saved
    sessionStore.saveSession(saved)
  }

  suspend fun loadOfflineDemoSession(): PersistedSupabaseSession? {
    val saved = sessionStore.loadSession() ?: return null
    if (!saved.isOfflineDemo) return null
    currentSession = saved
    return saved
  }

  suspend fun saveLastSelectedLocation(
    districtId: String,
    talukaId: String,
    villageId: String,
  ) {
    if (districtId.isBlank() || talukaId.isBlank() || villageId.isBlank()) return
    currentSession =
      currentSession?.copy(
        lastDistrictId = districtId,
        lastTalukaId = talukaId,
        lastVillageId = villageId,
      )
    sessionStore.saveLastSelectedLocation(districtId, talukaId, villageId)
  }

  private fun expectedDbRole(loginRole: LoginRole): String =
    when (loginRole) {
      LoginRole.SURVEY_EXECUTIVE -> "SURVEY_EXECUTIVE"
      LoginRole.SOLAR_INSTALLATION -> "SOLAR_INSTALLATION"
    }

  suspend fun signInAndVerifyExecutive(
    email: String,
    password: String,
    requestedRole: LoginRole,
  ): Pair<ExecutiveUser, CloudSyncSnapshot> {
    val authResp = api.signInWithPassword(email, password)
    val exec =
      try {
        api.fetchExecutiveProfile(
          accessToken = authResp.accessToken,
          authUserId = authResp.authUserId,
        )
      } catch (t: Throwable) {
        api.signOut(authResp.accessToken)
        clearLocalSession()
        throw t
      }

    if (exec == null) {
      api.signOut(authResp.accessToken)
      clearLocalSession()
      throw IllegalStateException(
        "${authResp.email} साठी एक्झिक्युटिव्ह प्रोफाइल आढळले नाही. कृपया ऑफिस अ‍ॅडमिनशी संपर्क साधा."
      )
    }

    if (exec.role == "ADMIN") {
      api.signOut(authResp.accessToken)
      clearLocalSession()
      throw IllegalStateException(ADMIN_MOBILE_BLOCKED_MESSAGE_MR)
    }

    if (!exec.isActive) {
      api.signOut(authResp.accessToken)
      clearLocalSession()
      throw AccessRevokedException(ACCESS_REVOKED_MESSAGE_MR)
    }

    val expectedRole = expectedDbRole(requestedRole)
    if (exec.role != expectedRole) {
      api.signOut(authResp.accessToken)
      clearLocalSession()
      val msg =
        if (requestedRole == LoginRole.SOLAR_INSTALLATION) {
          "हे खाते सोलर इन्स्टॉलेशन लॉगिनसाठी अधिकृत नाही (भूमिका: ${exec.roleMr.ifBlank { exec.role }}). कृपया योग्य लॉगिन मोड निवडा."
        } else {
          "हे खाते सर्व्हे एक्झिक्युटिव्ह लॉगिनसाठी अधिकृत नाही (भूमिका: ${exec.roleMr.ifBlank { exec.role }}). कृपया योग्य लॉगिन मोड निवडा."
        }
      throw IllegalStateException(msg)
    }

    // Immediately record last_login_at (ISO-8601 UTC) per Section 1
    api.touchExecutiveLogin(authResp.accessToken, exec.id)

    val savedSession =
      PersistedSupabaseSession(
        accessToken = authResp.accessToken,
        refreshToken = authResp.refreshToken,
        loginRole = requestedRole,
        executive = exec,
      )
    currentSession = savedSession
    sessionStore.saveSession(savedSession)

    val snapshot = syncForRole(authResp.accessToken, exec, requestedRole)
    return exec to snapshot
  }

  /**
   * Restores a previously saved session on App Launch and re-verifies `public.executives`
   * (`is_active == true` and matching `role`).
   * - Throws [AccessRevokedException] if the executive was deactivated or role-changed by Admin.
   * - Keeps the user logged in offline ([SessionRestoreResult.RestoredOfflineFallback]) on
   *   transient network/timeout failures so field users are never forced to re-enter credentials
   *   every time they open the app.
   */
  suspend fun tryRestoreSession(): SessionRestoreResult {
    if (!isEnabled) return SessionRestoreResult.NoSession
    val saved = sessionStore.loadSession()
    if (saved == null || saved.isOfflineDemo) {
      clearLocalSession()
      return SessionRestoreResult.NoSession
    }

    var token = saved.accessToken
    var refreshToken = saved.refreshToken
    var authUserId = saved.executive.authUserId
    var refreshRejectedByServer = false

    if (refreshToken.isNotBlank()) {
      runCatching { api.refreshSession(refreshToken) }
        .onSuccess { refreshed ->
          token = refreshed.accessToken
          refreshToken = refreshed.refreshToken
          if (refreshed.authUserId.isNotBlank()) authUserId = refreshed.authUserId
          // Persist rotated tokens immediately so a later network drop never invalidates them
          val rotated =
            saved.copy(
              accessToken = token,
              refreshToken = refreshToken,
              executive = saved.executive.copy(authUserId = authUserId),
            )
          currentSession = rotated
          sessionStore.saveSession(rotated)
        }
        .onFailure { err ->
          if (err is SupabaseHttpException && (err.statusCode == 400 || err.statusCode == 401)) {
            refreshRejectedByServer = true
          }
        }
    }

    val verifiedExec =
      try {
        verifyExecutiveOrRevoke(token, authUserId, saved.loginRole)
      } catch (revoked: AccessRevokedException) {
        throw revoked
      } catch (httpErr: SupabaseHttpException) {
        if (httpErr.statusCode == 401 || httpErr.statusCode == 403) {
          if (refreshRejectedByServer || refreshToken.isBlank()) {
            clearLocalSession()
            return SessionRestoreResult.NoSession
          }
        }
        currentSession = saved.copy(accessToken = token, refreshToken = refreshToken)
        return SessionRestoreResult.RestoredOfflineFallback(
          session = currentSession!!,
          offlineMessageMr = OFFLINE_RESTORE_MESSAGE_MR,
        )
      } catch (_: Throwable) {
        // Offline / timeout / DNS hiccup -> keep user logged in and allow pull-to-refresh later
        currentSession = saved.copy(accessToken = token, refreshToken = refreshToken)
        return SessionRestoreResult.RestoredOfflineFallback(
          session = currentSession!!,
          offlineMessageMr = OFFLINE_RESTORE_MESSAGE_MR,
        )
      }

    val activeSession =
      saved.copy(
        accessToken = token,
        refreshToken = refreshToken,
        executive = verifiedExec,
      )
    currentSession = activeSession
    sessionStore.saveSession(activeSession)

    return try {
      val snapshot = syncForRole(token, verifiedExec, saved.loginRole)
      SessionRestoreResult.RestoredOnline(activeSession, snapshot)
    } catch (_: Throwable) {
      SessionRestoreResult.RestoredOfflineFallback(
        session = activeSession,
        offlineMessageMr = OFFLINE_RESTORE_MESSAGE_MR,
      )
    }
  }

  /**
   * Re-verifies `public.executives` (`is_active == true` and matching `role`) on Foreground Resume
   * (`onResume`). Throws [AccessRevokedException] if revoked (after signing out & clearing session).
   */
  suspend fun verifyCurrentSessionOnResume(): ExecutiveUser? {
    if (!isEnabled) return null
    val saved = currentSession ?: sessionStore.loadSession()?.also { currentSession = it } ?: return null
    if (saved.isOfflineDemo) return null
    val (token, exec) = ensureFreshVerifiedSession(saved)
    val updated = saved.copy(accessToken = token, executive = exec)
    currentSession = updated
    sessionStore.saveSession(updated)
    return exec
  }

  suspend fun syncAll(): CloudSyncSnapshot {
    val session = requireActiveSession()
    return syncForRole(session.accessToken, session.executive, session.loginRole)
  }

  private suspend fun syncForRole(
    accessToken: String,
    exec: ExecutiveUser,
    role: LoginRole,
  ): CloudSyncSnapshot {
    return when (role) {
      LoginRole.SURVEY_EXECUTIVE -> {
        val customVillages = api.fetchCustomVillages(accessToken)
        val ownSurveys = api.fetchSurveysForExecutive(accessToken, exec.id)
        CloudSyncSnapshot(
          customVillages = customVillages,
          surveys = ownSurveys,
          installationReports = emptyList(),
          refreshedExecutive = exec,
        )
      }

      LoginRole.SOLAR_INSTALLATION -> {
        val pendingCat1 = api.fetchPendingInstallationSurveys(accessToken)
        val ownReports = api.fetchInstallationReportsForExecutive(accessToken, exec.id)
        val completedLinkedIds = ownReports.mapNotNull { it.linkedSurveyId }.toSet()
        val filteredPending = pendingCat1.filterNot { it.id in completedLinkedIds }
        CloudSyncSnapshot(
          customVillages = emptyList(),
          surveys = filteredPending,
          installationReports = ownReports,
          refreshedExecutive = exec,
        )
      }
    }
  }

  suspend fun saveCustomVillage(
    districtId: String,
    districtNameMr: String,
    talukaId: String,
    talukaNameMr: String,
    villageId: String,
    villageNameMr: String,
  ): CustomVillageRecord {
    val session = requireActiveSession(expectedRole = LoginRole.SURVEY_EXECUTIVE)
    val record =
      CustomVillageRecord(
        id = villageId,
        districtId = districtId,
        districtNameMr = districtNameMr,
        talukaId = talukaId,
        talukaNameMr = talukaNameMr,
        villageNameMr = villageNameMr,
        villageNameEn = villageNameMr,
      )
    api.upsertCustomVillage(session.accessToken, record, session.executive.id)
    return record
  }

  suspend fun saveSurveyWithUploads(
    survey: CustomerSurvey,
    previousSurvey: CustomerSurvey? = null,
  ): CustomerSurvey {
    val session = requireActiveSession(expectedRole = LoginRole.SURVEY_EXECUTIVE)
    val token = session.accessToken
    val execId = session.executive.id

    // 0. Delete any R2 keys that were explicitly cleared/removed during edit
    if (previousSurvey != null) {
      val removedKeys = buildList {
        previousSurvey.documents.forEach { (docType, prevEntry) ->
          val nextEntry = survey.documents[docType]
          val prevKey = prevEntry.r2ObjectKey
          if (!prevKey.isNullOrBlank() && nextEntry?.photoUri.isNullOrBlank()) {
            add(prevKey)
          }
        }
        val prevRoofKey = previousSurvey.siteSurveyDetails.noteCamRoofR2Key
        if (!prevRoofKey.isNullOrBlank() && survey.siteSurveyDetails.noteCamRoofBeforePhotoUri.isNullOrBlank()) {
          add(prevRoofKey)
        }
        val prevEarthingKey = previousSurvey.siteSurveyDetails.noteCamEarthingR2Key
        if (!prevEarthingKey.isNullOrBlank() && survey.siteSurveyDetails.noteCamEarthingWithOwnerPhotoUri.isNullOrBlank()) {
          add(prevEarthingKey)
        }
      }
      if (removedKeys.isNotEmpty()) {
        r2Uploader.deleteObjects(token, removedKeys)
      }
    }

    // 1. Upload any local document photos to Cloudflare R2 (deletes old r2_object_key first if replacing)
    val uploadedDocs =
      survey.documents.mapValues { (docType, entry) ->
        val oldKey = entry.r2ObjectKey ?: previousSurvey?.documents?.get(docType)?.r2ObjectKey
        if (entry.photoUri.isNullOrBlank()) {
          entry.copy(r2ObjectKey = null, photoSizeKb = null, isSizeValid = true)
        } else {
          val fileName = "${docType.name.lowercase()}_${System.currentTimeMillis()}.jpg"
          val uploaded =
            r2Uploader.uploadImageIfLocal(
              accessToken = token,
              folder = CloudflareR2Uploader.FOLDER_SURVEY_DOCS,
              recordId = survey.id,
              fileName = fileName,
              uriString = entry.photoUri,
              existingObjectKey = oldKey,
            )
          if (uploaded != null) {
            entry.copy(
              photoUri = uploaded.publicUrl,
              r2ObjectKey = uploaded.objectKey.ifBlank { oldKey },
              photoSizeKb = uploaded.sizeKb ?: entry.photoSizeKb,
              isSizeValid = true,
            )
          } else {
            entry.copy(isSizeValid = true)
          }
        }
      }

    // 2. Upload NoteCam Roof & Earthing photos to Cloudflare R2 (deletes old keys first if replacing)
    val site = survey.siteSurveyDetails
    val prevRoofKey = site.noteCamRoofR2Key ?: previousSurvey?.siteSurveyDetails?.noteCamRoofR2Key
    val prevEarthingKey =
      site.noteCamEarthingR2Key ?: previousSurvey?.siteSurveyDetails?.noteCamEarthingR2Key

    val roofUploaded =
      r2Uploader.uploadImageIfLocal(
        accessToken = token,
        folder = CloudflareR2Uploader.FOLDER_SURVEY_NOTECAM,
        recordId = survey.id,
        fileName = "notecam_roof_${System.currentTimeMillis()}.jpg",
        uriString = site.noteCamRoofBeforePhotoUri,
        existingObjectKey = prevRoofKey,
      )
    val earthingUploaded =
      r2Uploader.uploadImageIfLocal(
        accessToken = token,
        folder = CloudflareR2Uploader.FOLDER_SURVEY_NOTECAM,
        recordId = survey.id,
        fileName = "notecam_earthing_${System.currentTimeMillis()}.jpg",
        uriString = site.noteCamEarthingWithOwnerPhotoUri,
        existingObjectKey = prevEarthingKey,
      )

    val uploadedSite =
      site.copy(
        noteCamRoofBeforePhotoUri = roofUploaded?.publicUrl ?: site.noteCamRoofBeforePhotoUri,
        noteCamRoofR2Key =
          if (site.noteCamRoofBeforePhotoUri.isNullOrBlank()) null
          else roofUploaded?.objectKey?.ifBlank { prevRoofKey } ?: prevRoofKey,
        noteCamRoofBeforePhotoKb =
          if (site.noteCamRoofBeforePhotoUri.isNullOrBlank()) null
          else roofUploaded?.sizeKb ?: site.noteCamRoofBeforePhotoKb,
        noteCamEarthingWithOwnerPhotoUri =
          earthingUploaded?.publicUrl ?: site.noteCamEarthingWithOwnerPhotoUri,
        noteCamEarthingR2Key =
          if (site.noteCamEarthingWithOwnerPhotoUri.isNullOrBlank()) null
          else earthingUploaded?.objectKey?.ifBlank { prevEarthingKey } ?: prevEarthingKey,
        noteCamEarthingWithOwnerPhotoKb =
          if (site.noteCamEarthingWithOwnerPhotoUri.isNullOrBlank()) null
          else earthingUploaded?.sizeKb ?: site.noteCamEarthingWithOwnerPhotoKb,
      )

    val readySurvey =
      survey.copy(
        executiveId = execId,
        siteSurveyDetails = uploadedSite,
        documents = uploadedDocs,
      )

    // 3. Upsert relational + JSONB records into Supabase PostgreSQL
    api.upsertSurvey(token, readySurvey)
    return readySurvey
  }

  suspend fun deleteSurveyWithUploads(survey: CustomerSurvey) {
    val session = requireActiveSession(expectedRole = LoginRole.SURVEY_EXECUTIVE)
    val token = session.accessToken
    val keysToDelete = buildList {
      survey.documents.values.forEach { entry ->
        entry.r2ObjectKey?.takeIf { it.isNotBlank() }?.let(::add)
      }
      survey.siteSurveyDetails.noteCamRoofR2Key?.takeIf { it.isNotBlank() }?.let(::add)
      survey.siteSurveyDetails.noteCamEarthingR2Key?.takeIf { it.isNotBlank() }?.let(::add)
    }
    api.deleteSurvey(token, survey.id)
    if (keysToDelete.isNotEmpty()) {
      r2Uploader.deleteObjects(token, keysToDelete)
    }
  }

  suspend fun saveInstallationReportWithUploads(
    report: InstallationReport,
    previousReport: InstallationReport? = null,
  ): InstallationReport {
    val session = requireActiveSession(expectedRole = LoginRole.SOLAR_INSTALLATION)
    val token = session.accessToken
    val execId = session.executive.id

    // 0. Delete any R2 keys from slots that were removed or cleared during edit
    if (previousReport != null) {
      val currentPhotosById = report.afterInstallationPhotos.associateBy { it.id }
      val removedKeys =
        previousReport.afterInstallationPhotos.mapNotNull { prevPhoto ->
          val prevKey = prevPhoto.r2ObjectKey?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
          val nextPhoto = currentPhotosById[prevPhoto.id]
          if (nextPhoto == null || nextPhoto.photoUri.isNullOrBlank()) prevKey else null
        }
      if (removedKeys.isNotEmpty()) {
        r2Uploader.deleteObjects(token, removedKeys)
      }
    }

    val prevPhotosById = previousReport?.afterInstallationPhotos?.associateBy { it.id }.orEmpty()
    val uploadedPhotos =
      report.afterInstallationPhotos.mapIndexed { idx, photo ->
        val oldKey = photo.r2ObjectKey ?: prevPhotosById[photo.id]?.r2ObjectKey
        if (photo.photoUri.isNullOrBlank()) {
          photo.copy(r2ObjectKey = null, photoSizeKb = null)
        } else {
          val safeId = photo.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
          val fileName = "${safeId}_${idx}_${System.currentTimeMillis()}.jpg"
          val uploaded =
            r2Uploader.uploadImageIfLocal(
              accessToken = token,
              folder = CloudflareR2Uploader.FOLDER_INSTALLATION_PHOTOS,
              recordId = report.id,
              fileName = fileName,
              uriString = photo.photoUri,
              existingObjectKey = oldKey,
            )
          if (uploaded != null) {
            photo.copy(
              photoUri = uploaded.publicUrl,
              r2ObjectKey = uploaded.objectKey.ifBlank { oldKey },
              photoSizeKb = uploaded.sizeKb ?: photo.photoSizeKb,
            )
          } else {
            photo
          }
        }
      }

    val readyReport =
      report.copy(
        installerExecutiveId = execId,
        afterInstallationPhotos = uploadedPhotos,
      )
    api.upsertInstallationReport(token, readyReport)
    return readyReport
  }

  suspend fun signOut() {
    val token = currentSession?.accessToken.orEmpty()
    if (token.isNotBlank()) {
      api.signOut(token)
    }
    clearLocalSession()
  }

  private suspend fun clearLocalSession() {
    currentSession = null
    sessionStore.clearSession()
  }

  /**
   * Enforces live `public.executives` verification (`is_active == true` and matching `role`)
   * before every Sync / Save / Upload / Delete. Automatically attempts a token refresh on 401.
   */
  private suspend fun requireActiveSession(expectedRole: LoginRole? = null): PersistedSupabaseSession {
    val saved =
      currentSession ?: sessionStore.loadSession()?.also { currentSession = it }
        ?: throw IllegalStateException("कृपया प्रथम लॉगिन करा.")

    val roleToCheck = expectedRole ?: saved.loginRole
    val (freshToken, freshExec) = ensureFreshVerifiedSession(saved, roleToCheck)
    val updated = saved.copy(accessToken = freshToken, executive = freshExec)
    currentSession = updated
    sessionStore.saveSession(updated)
    return updated
  }

  private suspend fun ensureFreshVerifiedSession(
    saved: PersistedSupabaseSession,
    roleToCheck: LoginRole = saved.loginRole,
  ): Pair<String, ExecutiveUser> {
    return try {
      val exec = verifyExecutiveOrRevoke(saved.accessToken, saved.executive.authUserId, roleToCheck)
      saved.accessToken to exec
    } catch (httpErr: SupabaseHttpException) {
      if (httpErr.statusCode == 401 && saved.refreshToken.isNotBlank()) {
        val refreshed = api.refreshSession(saved.refreshToken)
        val authUid = refreshed.authUserId.ifBlank { saved.executive.authUserId }
        val exec = verifyExecutiveOrRevoke(refreshed.accessToken, authUid, roleToCheck)
        val updated =
          saved.copy(
            accessToken = refreshed.accessToken,
            refreshToken = refreshed.refreshToken,
            executive = exec,
          )
        currentSession = updated
        sessionStore.saveSession(updated)
        refreshed.accessToken to exec
      } else {
        throw httpErr
      }
    }
  }

  private suspend fun verifyExecutiveOrRevoke(
    accessToken: String,
    authUserId: String,
    expectedRole: LoginRole,
  ): ExecutiveUser {
    val exec = api.fetchExecutiveProfile(accessToken = accessToken, authUserId = authUserId)
    val requiredDbRole = expectedDbRole(expectedRole)
    if (exec == null || !exec.isActive || exec.role != requiredDbRole) {
      api.signOut(accessToken)
      clearLocalSession()
      throw AccessRevokedException(ACCESS_REVOKED_MESSAGE_MR)
    }
    return exec
  }
}
