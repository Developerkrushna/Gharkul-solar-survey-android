package com.example.data.remote

import android.content.Context
import android.net.Uri
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class R2UploadResult(
  val publicUrl: String,
  val objectKey: String,
  val sizeKb: Int? = null,
)

class CloudflareR2Uploader(
  private val context: Context,
  private val settings: SupabaseSettings,
  private val httpClient: OkHttpClient,
) {
  companion object {
    const val FOLDER_SURVEY_DOCS = "survey-docs"
    const val FOLDER_SURVEY_NOTECAM = "survey-notecam"
    const val FOLDER_INSTALLATION_PHOTOS = "installation-photos"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val SUPPORTED_IMAGE_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")
  }

  /**
   * Uploads a local content:// or file:// image to Cloudflare R2 via `r2-presign`.
   * Per ANDROID_SYNC_STRUCTURE.md Section 6 Step A: if replacing an existing uploaded photo
   * (`existingObjectKey` is non-blank), deletes the old R2 object key FIRST before presigning
   * and uploading the replacement bytes.
   */
  suspend fun uploadImageIfLocal(
    accessToken: String,
    folder: String,
    recordId: String,
    fileName: String,
    uriString: String?,
    existingObjectKey: String?,
  ): R2UploadResult? =
    withContext(Dispatchers.IO) {
      if (uriString.isNullOrBlank()) return@withContext null
      if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
        return@withContext R2UploadResult(
          publicUrl = uriString,
          objectKey = existingObjectKey.orEmpty(),
        )
      }
      if (uriString.startsWith("captured://")) {
        return@withContext R2UploadResult(
          publicUrl = uriString,
          objectKey = existingObjectKey.orEmpty(),
        )
      }

      val uri = Uri.parse(uriString)
      val imageBytes =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
          ?: throw IllegalStateException("Cannot read local photo file for upload: $fileName")

      val detectedMime =
        context.contentResolver.getType(uri)?.lowercase()?.takeIf { it in SUPPORTED_IMAGE_MIME_TYPES }
          ?: when {
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "image/jpeg"
          }
      val normalizedFileName =
        when (detectedMime) {
          "image/png" -> fileName.replace(Regex("\\.[^.]+$"), ".png")
          "image/webp" -> fileName.replace(Regex("\\.[^.]+$"), ".webp")
          else -> fileName
        }

      // Step A: Delete previous R2 object key before uploading replacement image
      if (!existingObjectKey.isNullOrBlank()) {
        deleteObjects(accessToken, listOf(existingObjectKey))
      }

      // Step B: Request presigned PUT URL from Supabase Edge Function `r2-presign`
      val presignPayload =
        JSONObject()
          .put("action", "presign")
          .put("folder", folder)
          .put("recordId", recordId)
          .put("fileName", normalizedFileName)
          .put("contentType", detectedMime)

      val presignReq =
        Request.Builder()
          .url("${settings.normalizedUrl}/functions/v1/r2-presign")
          .addHeader("Authorization", "Bearer $accessToken")
          .addHeader("apikey", settings.anonKey)
          .addHeader("Content-Type", "application/json")
          .post(presignPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
          .build()

      val presignBodyText: String
      httpClient.newCall(presignReq).execute().use { resp ->
        presignBodyText = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          throw IllegalStateException(
            "R2 presign failed (${resp.code}): ${extractErrorMessage(presignBodyText)}"
          )
        }
      }

      val presignJson = JSONObject(presignBodyText)
      val uploadUrl = presignJson.getString("upload_url")
      val publicUrl = presignJson.getString("public_url")
      val objectKey = presignJson.getString("object_key")

      // Step C: Upload raw image bytes directly to Cloudflare R2 with matching Content-Type
      val putReq =
        Request.Builder()
          .url(uploadUrl)
          .addHeader("Content-Type", detectedMime)
          .put(imageBytes.toRequestBody(detectedMime.toMediaType()))
          .build()

      httpClient.newCall(putReq).execute().use { putResp ->
        if (!putResp.isSuccessful) {
          val errText = putResp.body?.string().orEmpty()
          throw IllegalStateException("Cloudflare R2 upload failed (${putResp.code}): $errText")
        }
      }

      val computedSizeKb = max(1, (imageBytes.size / 1024.0).roundToInt())
      R2UploadResult(
        publicUrl = publicUrl,
        objectKey = objectKey,
        sizeKb = computedSizeKb,
      )
    }

  suspend fun deleteObjects(accessToken: String, objectKeys: List<String>) {
    val cleanKeys = objectKeys.filter { it.isNotBlank() }.distinct()
    if (cleanKeys.isEmpty()) return
    withContext(Dispatchers.IO) {
      val payload =
        JSONObject()
          .put("action", "delete")
          .put("object_keys", JSONArray(cleanKeys))

      val req =
        Request.Builder()
          .url("${settings.normalizedUrl}/functions/v1/r2-presign")
          .addHeader("Authorization", "Bearer $accessToken")
          .addHeader("apikey", settings.anonKey)
          .addHeader("Content-Type", "application/json")
          .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
          .build()

      runCatching {
        httpClient.newCall(req).execute().close()
      }
    }
  }

  private fun extractErrorMessage(raw: String): String =
    runCatching {
      val obj = JSONObject(raw)
      obj.optString("error").ifBlank { obj.optString("message").ifBlank { raw } }
    }.getOrDefault(raw)
}
