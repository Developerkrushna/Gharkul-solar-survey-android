package com.example.data.remote

import com.example.model.CustomerSurvey
import com.example.model.ExecutiveUser
import com.example.model.InstallationReport
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class AuthTokenResponse(
  val accessToken: String,
  val refreshToken: String,
  val authUserId: String,
  val email: String,
)

class SupabaseHttpException(
  val statusCode: Int,
  message: String,
) : IllegalStateException(message)

class SupabaseApiClient(
  private val settings: SupabaseSettings,
  private val httpClient: OkHttpClient,
) {
  companion object {
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
  }

  suspend fun signInWithPassword(email: String, password: String): AuthTokenResponse =
    withContext(Dispatchers.IO) {
      val payload = JSONObject().put("email", email.trim()).put("password", password)
      val req =
        Request.Builder()
          .url("${settings.normalizedUrl}/auth/v1/token?grant_type=password")
          .addHeader("apikey", settings.anonKey)
          .addHeader("Content-Type", "application/json")
          .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
          .build()

      val body = executeForBody(req, "लॉगिन अयशस्वी")
      val json = JSONObject(body)
      val userObj = json.optJSONObject("user")
      AuthTokenResponse(
        accessToken = json.getString("access_token"),
        refreshToken = json.optString("refresh_token"),
        authUserId = userObj?.optString("id").orEmpty(),
        email = userObj?.optString("email").orEmpty().ifBlank { email.trim() },
      )
    }

  suspend fun refreshSession(refreshToken: String): AuthTokenResponse =
    withContext(Dispatchers.IO) {
      val payload = JSONObject().put("refresh_token", refreshToken)
      val req =
        Request.Builder()
          .url("${settings.normalizedUrl}/auth/v1/token?grant_type=refresh_token")
          .addHeader("apikey", settings.anonKey)
          .addHeader("Content-Type", "application/json")
          .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
          .build()

      val body = executeForBody(req, "Session refresh failed")
      val json = JSONObject(body)
      val userObj = json.optJSONObject("user")
      AuthTokenResponse(
        accessToken = json.getString("access_token"),
        refreshToken = json.optString("refresh_token").ifBlank { refreshToken },
        authUserId = userObj?.optString("id").orEmpty(),
        email = userObj?.optString("email").orEmpty(),
      )
    }

  suspend fun signOut(accessToken: String) {
    if (accessToken.isBlank()) return
    withContext(Dispatchers.IO) {
      val req =
        Request.Builder()
          .url("${settings.normalizedUrl}/auth/v1/logout")
          .addHeader("apikey", settings.anonKey)
          .addHeader("Authorization", "Bearer $accessToken")
          .post("".toRequestBody(null))
          .build()
      runCatching { httpClient.newCall(req).execute().close() }
    }
  }

  /**
   * Looks up `public.executives` strictly by `auth_user_id = auth.uid()` per Section 1 of
   * ANDROID_SYNC_STRUCTURE.md.
   */
  suspend fun fetchExecutiveProfile(
    accessToken: String,
    authUserId: String,
  ): ExecutiveUser? =
    withContext(Dispatchers.IO) {
      if (authUserId.isBlank()) return@withContext null
      val req =
        Request.Builder()
          .url("${settings.normalizedUrl}/rest/v1/executives?auth_user_id=eq.${encode(authUserId)}&select=*")
          .addHeader("apikey", settings.anonKey)
          .addHeader("Authorization", "Bearer $accessToken")
          .get()
          .build()

      val body = executeForBody(req, "Failed to load executive profile")
      val arr = JSONArray(body)
      if (arr.length() == 0) return@withContext null
      SupabaseMappers.executiveFromJson(arr.getJSONObject(0))
    }

  suspend fun touchExecutiveLogin(accessToken: String, executiveId: String) {
    if (executiveId.isBlank()) return
    withContext(Dispatchers.IO) {
      val isoFmt =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
          timeZone = TimeZone.getTimeZone("UTC")
        }
      val payload = JSONObject().put("last_login_at", isoFmt.format(Date()))
      val req =
        Request.Builder()
          .url("${settings.normalizedUrl}/rest/v1/executives?id=eq.${encode(executiveId)}")
          .addHeader("apikey", settings.anonKey)
          .addHeader("Authorization", "Bearer $accessToken")
          .addHeader("Content-Type", "application/json")
          .patch(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
          .build()
      runCatching { httpClient.newCall(req).execute().close() }
    }
  }

  suspend fun fetchCustomVillages(accessToken: String): List<CustomVillageRecord> =
    withContext(Dispatchers.IO) {
      val req =
        Request.Builder()
          .url("${settings.normalizedUrl}/rest/v1/custom_villages?select=*&order=created_at.asc")
          .addHeader("apikey", settings.anonKey)
          .addHeader("Authorization", "Bearer $accessToken")
          .get()
          .build()

      val body = executeForBody(req, "Failed to load custom villages")
      val arr = JSONArray(body)
      buildList {
        for (i in 0 until arr.length()) {
          add(SupabaseMappers.customVillageFromJson(arr.getJSONObject(i)))
        }
      }
    }

  suspend fun upsertCustomVillage(
    accessToken: String,
    record: CustomVillageRecord,
    executiveId: String?,
  ) =
    withContext(Dispatchers.IO) {
      val payload = SupabaseMappers.customVillageToJson(record, executiveId)
      val req =
        Request.Builder()
          .url("${settings.normalizedUrl}/rest/v1/custom_villages")
          .addHeader("apikey", settings.anonKey)
          .addHeader("Authorization", "Bearer $accessToken")
          .addHeader("Content-Type", "application/json")
          .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
          .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
          .build()
      executeForBody(req, "Failed to save village")
    }

  /**
   * SURVEY_EXECUTIVE scoped query: fetches ONLY surveys created by `executiveId`
   * along with their `survey_documents`.
   */
  suspend fun fetchSurveysForExecutive(
    accessToken: String,
    executiveId: String,
  ): List<CustomerSurvey> =
    withContext(Dispatchers.IO) {
      val req =
        Request.Builder()
          .url(
            "${settings.normalizedUrl}/rest/v1/customer_surveys" +
              "?executive_id=eq.${encode(executiveId)}&select=*,survey_documents(*)&order=created_at.desc"
          )
          .addHeader("apikey", settings.anonKey)
          .addHeader("Authorization", "Bearer $accessToken")
          .get()
          .build()

      val body = executeForBody(req, "Failed to load customer surveys")
      val arr = JSONArray(body)
      buildList {
        for (i in 0 until arr.length()) {
          add(SupabaseMappers.customerSurveyFromJson(arr.getJSONObject(i)))
        }
      }
    }

  /**
   * SOLAR_INSTALLATION scoped query: fetches ONLY Category 1 surveys where Office Admin
   * marked `gov_form_completed = true`. RLS additionally excludes surveys that already have an
   * `installation_reports` row.
   */
  suspend fun fetchPendingInstallationSurveys(accessToken: String): List<CustomerSurvey> =
    withContext(Dispatchers.IO) {
      val req =
        Request.Builder()
          .url(
            "${settings.normalizedUrl}/rest/v1/customer_surveys" +
              "?final_category=eq.CATEGORY_1&gov_form_completed=eq.true&select=*&order=created_at.desc"
          )
          .addHeader("apikey", settings.anonKey)
          .addHeader("Authorization", "Bearer $accessToken")
          .get()
          .build()

      val body = executeForBody(req, "Failed to load pending installation customers")
      val arr = JSONArray(body)
      buildList {
        for (i in 0 until arr.length()) {
          add(SupabaseMappers.customerSurveyFromJson(arr.getJSONObject(i)))
        }
      }
    }

  suspend fun upsertSurvey(accessToken: String, survey: CustomerSurvey) =
    withContext(Dispatchers.IO) {
      val surveyJson = SupabaseMappers.customerSurveyToJson(survey)
      val surveyReq =
        Request.Builder()
          .url("${settings.normalizedUrl}/rest/v1/customer_surveys")
          .addHeader("apikey", settings.anonKey)
          .addHeader("Authorization", "Bearer $accessToken")
          .addHeader("Content-Type", "application/json")
          .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
          .post(surveyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
          .build()
      executeForBody(surveyReq, "Failed to save customer survey")

      val docsArr = SupabaseMappers.surveyDocumentsToJsonArray(survey.id, survey.documents)
      if (docsArr.length() > 0) {
        val docsReq =
          Request.Builder()
            .url("${settings.normalizedUrl}/rest/v1/survey_documents?on_conflict=survey_id,doc_type")
            .addHeader("apikey", settings.anonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(docsArr.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        executeForBody(docsReq, "Failed to save survey documents")
      }
    }

  suspend fun deleteSurvey(accessToken: String, surveyId: String) =
    withContext(Dispatchers.IO) {
      val req =
        Request.Builder()
          .url("${settings.normalizedUrl}/rest/v1/customer_surveys?id=eq.${encode(surveyId)}")
          .addHeader("apikey", settings.anonKey)
          .addHeader("Authorization", "Bearer $accessToken")
          .delete()
          .build()
      executeForBody(req, "Failed to delete customer survey")
    }

  /**
   * SOLAR_INSTALLATION scoped query: fetches ONLY installation reports submitted by
   * `installerExecutiveId` along with their `installation_photos`.
   */
  suspend fun fetchInstallationReportsForExecutive(
    accessToken: String,
    installerExecutiveId: String,
  ): List<InstallationReport> =
    withContext(Dispatchers.IO) {
      val req =
        Request.Builder()
          .url(
            "${settings.normalizedUrl}/rest/v1/installation_reports" +
              "?installer_executive_id=eq.${encode(installerExecutiveId)}&select=*,installation_photos(*)&order=updated_at_millis.desc"
          )
          .addHeader("apikey", settings.anonKey)
          .addHeader("Authorization", "Bearer $accessToken")
          .get()
          .build()

      val body = executeForBody(req, "Failed to load installation reports")
      val arr = JSONArray(body)
      buildList {
        for (i in 0 until arr.length()) {
          add(SupabaseMappers.installationReportFromJson(arr.getJSONObject(i)))
        }
      }
    }

  suspend fun upsertInstallationReport(accessToken: String, report: InstallationReport) =
    withContext(Dispatchers.IO) {
      val reportJson = SupabaseMappers.installationReportToJson(report)
      val req =
        Request.Builder()
          .url("${settings.normalizedUrl}/rest/v1/installation_reports")
          .addHeader("apikey", settings.anonKey)
          .addHeader("Authorization", "Bearer $accessToken")
          .addHeader("Content-Type", "application/json")
          .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
          .post(reportJson.toString().toRequestBody(JSON_MEDIA_TYPE))
          .build()
      executeForBody(req, "Failed to save installation report")

      val deleteOldReq =
        Request.Builder()
          .url("${settings.normalizedUrl}/rest/v1/installation_photos?report_id=eq.${encode(report.id)}")
          .addHeader("apikey", settings.anonKey)
          .addHeader("Authorization", "Bearer $accessToken")
          .delete()
          .build()
      executeForBody(deleteOldReq, "Failed to sync installation photos")

      val photosArr =
        SupabaseMappers.installationPhotosToJsonArray(report.id, report.afterInstallationPhotos)
      if (photosArr.length() > 0) {
        val photosReq =
          Request.Builder()
            .url("${settings.normalizedUrl}/rest/v1/installation_photos")
            .addHeader("apikey", settings.anonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(photosArr.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        executeForBody(photosReq, "Failed to save installation photos")
      }
    }

  private fun executeForBody(request: Request, fallbackPrefix: String): String {
    httpClient.newCall(request).execute().use { resp ->
      val body = resp.body?.string().orEmpty()
      if (!resp.isSuccessful) {
        val detail = parseErrorMessage(body)
        throw SupabaseHttpException(resp.code, "$fallbackPrefix (${resp.code}): $detail")
      }
      return body
    }
  }

  private fun parseErrorMessage(raw: String): String =
    runCatching {
      val obj = JSONObject(raw)
      obj.optString("error_description")
        .ifBlank { obj.optString("msg") }
        .ifBlank { obj.optString("message") }
        .ifBlank { obj.optString("error") }
        .ifBlank { raw }
    }.getOrDefault(raw)

  private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
