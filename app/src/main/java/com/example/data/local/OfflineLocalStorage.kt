package com.example.data.local

import android.content.Context
import android.net.Uri
import com.example.data.remote.CustomVillageRecord
import com.example.data.remote.SupabaseMappers
import com.example.model.CustomerSurvey
import com.example.model.InstallationReport
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class OfflineCachedSnapshot(
  val surveys: List<CustomerSurvey>,
  val installationReports: List<InstallationReport>,
  val customVillages: List<CustomVillageRecord>,
)

data class OfflinePendingQueue(
  val pendingSurveys: List<CustomerSurvey> = emptyList(),
  val pendingReports: List<InstallationReport> = emptyList(),
  val pendingCustomVillages: List<CustomVillageRecord> = emptyList(),
) {
  val totalCount: Int
    get() = pendingSurveys.size + pendingReports.size + pendingCustomVillages.size
}

/**
 * Durable device-local storage in `context.filesDir` for offline form data, cached lists,
 * and captured document/site images. Survives app restarts and OS cache eviction until synced
 * to Supabase + Cloudflare R2 or cleared on explicit logout/revocation.
 */
class OfflineLocalStorage(private val context: Context) {

  private val ioLock = Any()

  private val storeDir: File
    get() = File(context.filesDir, "offline_store").apply { mkdirs() }

  private val photosDir: File
    get() = File(context.filesDir, "offline_photos").apply { mkdirs() }

  private val cacheFile: File
    get() = File(storeDir, "cached_snapshot.json")

  private val queueFile: File
    get() = File(storeDir, "pending_sync_queue.json")

  suspend fun persistSurveyPhotosLocally(survey: CustomerSurvey): CustomerSurvey =
    withContext(Dispatchers.IO) {
      val durableDocs =
        survey.documents.mapValues { (docType, entry) ->
          val durableUri = copyPhotoToOfflineDir(entry.photoUri, "${survey.id}_${docType.name.lowercase()}")
          entry.copy(photoUri = durableUri)
        }
      val site = survey.siteSurveyDetails
      val durableRoof =
        copyPhotoToOfflineDir(site.noteCamRoofBeforePhotoUri, "${survey.id}_notecam_roof")
      val durableEarthing =
        copyPhotoToOfflineDir(site.noteCamEarthingWithOwnerPhotoUri, "${survey.id}_notecam_earthing")

      survey.copy(
        documents = durableDocs,
        siteSurveyDetails =
          site.copy(
            noteCamRoofBeforePhotoUri = durableRoof,
            noteCamEarthingWithOwnerPhotoUri = durableEarthing,
          ),
      )
    }

  suspend fun persistInstallationPhotosLocally(report: InstallationReport): InstallationReport =
    withContext(Dispatchers.IO) {
      val durablePhotos =
        report.afterInstallationPhotos.mapIndexed { idx, photo ->
          val durableUri = copyPhotoToOfflineDir(photo.photoUri, "${report.id}_photo_${idx}")
          photo.copy(photoUri = durableUri)
        }
      report.copy(afterInstallationPhotos = durablePhotos)
    }

  private fun copyPhotoToOfflineDir(uriString: String?, prefix: String): String? {
    if (uriString.isNullOrBlank()) return uriString
    if (
      uriString.startsWith("http://") ||
        uriString.startsWith("https://") ||
        uriString.startsWith("captured://")
    ) {
      return uriString
    }
    val parsed = Uri.parse(uriString)
    // Already stored in durable offline_photos directory
    if (parsed.scheme == "file" && parsed.path?.startsWith(photosDir.absolutePath) == true) {
      val existing = File(parsed.path!!)
      if (existing.exists() && existing.length() > 0L) return uriString
    }

    return runCatching {
      val bytes =
        context.contentResolver.openInputStream(parsed)?.use { it.readBytes() }
          ?: return uriString
      val safePrefix = prefix.replace(Regex("[^a-zA-Z0-9_-]"), "_")
      val destFile = File(photosDir, "${safePrefix}_${System.currentTimeMillis()}.jpg")
      FileOutputStream(destFile).use { it.write(bytes) }
      Uri.fromFile(destFile).toString()
    }.getOrDefault(uriString)
  }

  suspend fun cleanupOfflinePhotosForSurvey(survey: CustomerSurvey) =
    withContext(Dispatchers.IO) {
      val uris =
        buildList {
          survey.documents.values.forEach { add(it.photoUri) }
          add(survey.siteSurveyDetails.noteCamRoofBeforePhotoUri)
          add(survey.siteSurveyDetails.noteCamEarthingWithOwnerPhotoUri)
        }
      deleteOfflinePhotoFiles(uris)
    }

  suspend fun cleanupOfflinePhotosForReport(report: InstallationReport) =
    withContext(Dispatchers.IO) {
      deleteOfflinePhotoFiles(report.afterInstallationPhotos.map { it.photoUri })
    }

  private fun deleteOfflinePhotoFiles(uris: List<String?>) {
    uris.forEach { uriStr ->
      if (uriStr.isNullOrBlank()) return@forEach
      runCatching {
        val parsed = Uri.parse(uriStr)
        if (parsed.scheme == "file" && parsed.path?.startsWith(photosDir.absolutePath) == true) {
          File(parsed.path!!).delete()
        }
      }
    }
  }

  suspend fun saveCache(
    surveys: List<CustomerSurvey>,
    installationReports: List<InstallationReport>,
    customVillages: List<CustomVillageRecord>,
  ) =
    withContext(Dispatchers.IO) {
      synchronized(ioLock) {
        val root = JSONObject()
        val surveysArr = JSONArray()
        surveys.forEach { survey ->
          val obj = SupabaseMappers.customerSurveyToJson(survey)
          obj.put(
            "survey_documents",
            SupabaseMappers.surveyDocumentsToJsonArray(survey.id, survey.documents),
          )
          surveysArr.put(obj)
        }
        val reportsArr = JSONArray()
        installationReports.forEach { report ->
          val obj = SupabaseMappers.installationReportToJson(report)
          obj.put(
            "installation_photos",
            SupabaseMappers.installationPhotosToJsonArray(report.id, report.afterInstallationPhotos),
          )
          reportsArr.put(obj)
        }
        val villagesArr = JSONArray()
        customVillages.forEach { rec ->
          villagesArr.put(SupabaseMappers.customVillageToJson(rec, null))
        }
        root.put("surveys", surveysArr)
        root.put("installation_reports", reportsArr)
        root.put("custom_villages", villagesArr)
        writeAtomically(cacheFile, root.toString())
      }
    }

  suspend fun loadCache(): OfflineCachedSnapshot =
    withContext(Dispatchers.IO) {
      synchronized(ioLock) {
        if (!cacheFile.exists()) {
          return@withContext OfflineCachedSnapshot(emptyList(), emptyList(), emptyList())
        }
        runCatching {
          val root = JSONObject(cacheFile.readText(Charsets.UTF_8))
          val surveys = decodeSurveyArray(root.optJSONArray("surveys"))
          val reports = decodeReportArray(root.optJSONArray("installation_reports"))
          val villages = decodeVillageArray(root.optJSONArray("custom_villages"))
          OfflineCachedSnapshot(surveys, reports, villages)
        }.getOrElse {
          OfflineCachedSnapshot(emptyList(), emptyList(), emptyList())
        }
      }
    }

  suspend fun loadPendingQueue(): OfflinePendingQueue =
    withContext(Dispatchers.IO) {
      synchronized(ioLock) {
        readQueueLocked()
      }
    }

  suspend fun enqueuePendingSurvey(survey: CustomerSurvey): OfflinePendingQueue =
    withContext(Dispatchers.IO) {
      val durableSurvey = persistSurveyPhotosLocally(survey)
      synchronized(ioLock) {
        val cur = readQueueLocked()
        val updated =
          cur.copy(
            pendingSurveys =
              cur.pendingSurveys.filterNot { it.id == durableSurvey.id } + durableSurvey,
          )
        writeQueueLocked(updated)
        updated
      }
    }

  suspend fun removePendingSurvey(surveyId: String): OfflinePendingQueue =
    withContext(Dispatchers.IO) {
      synchronized(ioLock) {
        val cur = readQueueLocked()
        val updated = cur.copy(pendingSurveys = cur.pendingSurveys.filterNot { it.id == surveyId })
        writeQueueLocked(updated)
        updated
      }
    }

  suspend fun enqueuePendingReport(report: InstallationReport): OfflinePendingQueue =
    withContext(Dispatchers.IO) {
      val durableReport = persistInstallationPhotosLocally(report)
      synchronized(ioLock) {
        val cur = readQueueLocked()
        val updated =
          cur.copy(
            pendingReports =
              cur.pendingReports.filterNot { it.id == durableReport.id } + durableReport,
          )
        writeQueueLocked(updated)
        updated
      }
    }

  suspend fun removePendingReport(reportId: String): OfflinePendingQueue =
    withContext(Dispatchers.IO) {
      synchronized(ioLock) {
        val cur = readQueueLocked()
        val updated = cur.copy(pendingReports = cur.pendingReports.filterNot { it.id == reportId })
        writeQueueLocked(updated)
        updated
      }
    }

  suspend fun enqueuePendingCustomVillage(record: CustomVillageRecord): OfflinePendingQueue =
    withContext(Dispatchers.IO) {
      synchronized(ioLock) {
        val cur = readQueueLocked()
        val updated =
          cur.copy(
            pendingCustomVillages =
              cur.pendingCustomVillages.filterNot { it.id == record.id } + record,
          )
        writeQueueLocked(updated)
        updated
      }
    }

  suspend fun removePendingCustomVillage(villageId: String): OfflinePendingQueue =
    withContext(Dispatchers.IO) {
      synchronized(ioLock) {
        val cur = readQueueLocked()
        val updated =
          cur.copy(
            pendingCustomVillages = cur.pendingCustomVillages.filterNot { it.id == villageId },
          )
        writeQueueLocked(updated)
        updated
      }
    }

  suspend fun clearAll() =
    withContext(Dispatchers.IO) {
      synchronized(ioLock) {
        runCatching { cacheFile.delete() }
        runCatching { queueFile.delete() }
        runCatching { photosDir.listFiles()?.forEach { it.delete() } }
      }
    }

  private fun readQueueLocked(): OfflinePendingQueue {
    if (!queueFile.exists()) return OfflinePendingQueue()
    return runCatching {
      val root = JSONObject(queueFile.readText(Charsets.UTF_8))
      OfflinePendingQueue(
        pendingSurveys = decodeSurveyArray(root.optJSONArray("pending_surveys")),
        pendingReports = decodeReportArray(root.optJSONArray("pending_reports")),
        pendingCustomVillages = decodeVillageArray(root.optJSONArray("pending_custom_villages")),
      )
    }.getOrElse { OfflinePendingQueue() }
  }

  private fun writeQueueLocked(queue: OfflinePendingQueue) {
    val root = JSONObject()
    val surveysArr = JSONArray()
    queue.pendingSurveys.forEach { survey ->
      val obj = SupabaseMappers.customerSurveyToJson(survey)
      obj.put(
        "survey_documents",
        SupabaseMappers.surveyDocumentsToJsonArray(survey.id, survey.documents),
      )
      surveysArr.put(obj)
    }
    val reportsArr = JSONArray()
    queue.pendingReports.forEach { report ->
      val obj = SupabaseMappers.installationReportToJson(report)
      obj.put(
        "installation_photos",
        SupabaseMappers.installationPhotosToJsonArray(report.id, report.afterInstallationPhotos),
      )
      reportsArr.put(obj)
    }
    val villagesArr = JSONArray()
    queue.pendingCustomVillages.forEach { rec ->
      villagesArr.put(SupabaseMappers.customVillageToJson(rec, null))
    }
    root.put("pending_surveys", surveysArr)
    root.put("pending_reports", reportsArr)
    root.put("pending_custom_villages", villagesArr)
    writeAtomically(queueFile, root.toString())
  }

  private fun decodeSurveyArray(arr: JSONArray?): List<CustomerSurvey> {
    if (arr == null) return emptyList()
    return buildList {
      for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        runCatching { SupabaseMappers.customerSurveyFromJson(obj) }.getOrNull()?.let { add(it) }
      }
    }
  }

  private fun decodeReportArray(arr: JSONArray?): List<InstallationReport> {
    if (arr == null) return emptyList()
    return buildList {
      for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        runCatching { SupabaseMappers.installationReportFromJson(obj) }.getOrNull()?.let { add(it) }
      }
    }
  }

  private fun decodeVillageArray(arr: JSONArray?): List<CustomVillageRecord> {
    if (arr == null) return emptyList()
    return buildList {
      for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        runCatching { SupabaseMappers.customVillageFromJson(obj) }.getOrNull()?.let { add(it) }
      }
    }
  }

  private fun writeAtomically(target: File, content: String) {
    val tmp = File(target.parentFile, "${target.name}.tmp")
    tmp.writeText(content, Charsets.UTF_8)
    if (!tmp.renameTo(target)) {
      target.writeText(content, Charsets.UTF_8)
      tmp.delete()
    }
  }
}
