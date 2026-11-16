package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class CompressedImageResult(
  val uriString: String,
  val sizeKb: Int,
  val isWithinTargetRange: Boolean,
)

object ImageCompressionUtil {

  const val MIN_TARGET_KB = 200
  const val MAX_TARGET_KB = 500
  private const val MIN_BYTES = MIN_TARGET_KB * 1024
  private const val MAX_BYTES = MAX_TARGET_KB * 1024

  /**
   * Compresses or scales an image URI into a JPEG cached inside survey_photos/ aiming strictly
   * for 200 KB to 500 KB.
   */
  fun compressToTargetRange(context: Context, sourceUri: Uri, prefix: String): CompressedImageResult? {
    return runCatching {
      val originalBytes =
        context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() } ?: return null

      val outDir = File(context.cacheDir, "survey_photos").apply { mkdirs() }
      val outFile = File(outDir, "${prefix.lowercase()}_${System.currentTimeMillis()}.jpg")

      if (originalBytes.size in MIN_BYTES..MAX_BYTES) {
        FileOutputStream(outFile).use { it.write(originalBytes) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        return CompressedImageResult(
          uriString = uri.toString(),
          sizeKb = (originalBytes.size / 1024).coerceAtLeast(1),
          isWithinTargetRange = true,
        )
      }

      val bitmap = decodeScaledBitmap(originalBytes, maxDimension = 1800) ?: return null
      var workingBitmap = bitmap
      var finalBytes = encodeJpeg(workingBitmap, 92)

      // If too large (> 500 KB), reduce JPEG quality then dimensions iteratively
      if (finalBytes.size > MAX_BYTES) {
        val qualities = listOf(85, 75, 65, 55, 45, 35, 25)
        for (q in qualities) {
          finalBytes = encodeJpeg(workingBitmap, q)
          if (finalBytes.size <= MAX_BYTES) break
        }
        var scaleAttempts = 0
        while (finalBytes.size > MAX_BYTES && scaleAttempts < 3) {
          val nextW = (workingBitmap.width * 0.78f).toInt().coerceAtLeast(640)
          val nextH = (workingBitmap.height * 0.78f).toInt().coerceAtLeast(480)
          val scaled = Bitmap.createScaledBitmap(workingBitmap, nextW, nextH, true)
          if (workingBitmap != bitmap) workingBitmap.recycle()
          workingBitmap = scaled
          finalBytes = encodeJpeg(workingBitmap, 75)
          if (finalBytes.size > MAX_BYTES) {
            finalBytes = encodeJpeg(workingBitmap, 55)
          }
          scaleAttempts++
        }
      } else if (finalBytes.size < MIN_BYTES) {
        // If under 200 KB, encode at max quality and upscale moderately if tiny to keep clear legibility
        finalBytes = encodeJpeg(workingBitmap, 100)
        var upscaleAttempts = 0
        while (finalBytes.size < MIN_BYTES && upscaleAttempts < 2 && workingBitmap.width < 2200) {
          val nextW = (workingBitmap.width * 1.35f).toInt().coerceAtMost(2400)
          val nextH = (workingBitmap.height * 1.35f).toInt().coerceAtMost(2400)
          val scaled = Bitmap.createScaledBitmap(workingBitmap, nextW, nextH, true)
          if (workingBitmap != bitmap) workingBitmap.recycle()
          workingBitmap = scaled
          finalBytes = encodeJpeg(workingBitmap, 100)
          upscaleAttempts++
        }
      }

      if (workingBitmap != bitmap) workingBitmap.recycle()
      bitmap.recycle()

      FileOutputStream(outFile).use { it.write(finalBytes) }
      val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
      val kb = (finalBytes.size / 1024).coerceAtLeast(1)
      CompressedImageResult(
        uriString = uri.toString(),
        sizeKb = kb,
        isWithinTargetRange = kb in MIN_TARGET_KB..MAX_TARGET_KB,
      )
    }.getOrNull()
  }

  private fun decodeScaledBitmap(bytes: ByteArray, maxDimension: Int): Bitmap? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    val maxSide = maxOf(opts.outWidth, opts.outHeight)
    var sampleSize = 1
    while (maxSide / sampleSize > maxDimension) {
      sampleSize *= 2
    }
    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
  }

  private fun encodeJpeg(bitmap: Bitmap, quality: Int): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    return stream.toByteArray()
  }

  /**
   * Deletes all app-owned cached photo files referenced by a customer's documents or site photos.
   * Ignores synthetic preview URIs (captured://) and null/blank strings safely.
   */
  fun deleteCachedPhotos(context: Context, uriStrings: Iterable<String?>) {
    val photosDir = File(context.cacheDir, "survey_photos")
    val canonicalDir = runCatching { photosDir.canonicalPath }.getOrNull() ?: return
    val authority = "${context.packageName}.fileprovider"

    uriStrings.forEach { raw ->
      if (raw.isNullOrBlank() || raw.startsWith("captured://")) return@forEach
      runCatching {
        val uri = Uri.parse(raw)
        val candidateFile: File? =
          when {
            uri.scheme == "file" && uri.path != null -> File(uri.path!!)
            uri.scheme == "content" && uri.authority == authority -> {
              val fileName = uri.lastPathSegment ?: return@runCatching
              File(photosDir, fileName)
            }
            else -> null
          }
        if (candidateFile != null && candidateFile.exists()) {
          val canonicalFile = candidateFile.canonicalPath
          if (canonicalFile.startsWith(canonicalDir + File.separator)) {
            candidateFile.delete()
          }
        }
      }
    }
  }
}
