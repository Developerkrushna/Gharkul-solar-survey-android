package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class CompressedImageResult(
  val uriString: String,
  val sizeKb: Int,
  val isWithinTargetRange: Boolean = true,
)

data class NoteCamPhotoResult(
  val uriString: String,
  val sizeKb: Int,
  val geoSummary: String,
)

sealed class LiveLocationOutcome {
  data class Success(
    val coordsText: String,
    val addressText: String,
    val timestampText: String,
  ) : LiveLocationOutcome()

  data object LocationDisabled : LiveLocationOutcome()

  data object Unavailable : LiveLocationOutcome()
}

object ImageCompressionUtil {

  /**
   * Saves any selected or captured image as-is without altering pixels or adding overlays.
   */
  fun saveDocumentPhoto(context: Context, sourceUri: Uri, prefix: String): CompressedImageResult? {
    return runCatching {
      val originalBytes =
        context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() } ?: return null
      val outDir = File(context.cacheDir, "survey_photos").apply { mkdirs() }
      val outFile = File(outDir, "${prefix.lowercase()}_${System.currentTimeMillis()}.jpg")

      FileOutputStream(outFile).use { it.write(originalBytes) }
      val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
      val kb = (originalBytes.size / 1024).coerceAtLeast(1)
      CompressedImageResult(
        uriString = uri.toString(),
        sizeKb = kb,
        isWithinTargetRange = true,
      )
    }.getOrNull()
  }

  /**
   * Stamps Note Cam live geo-location details (GPS coords, live address/location, Date & Time)
   * onto the bottom-right corner of a camera-captured image with low translucent background opacity.
   */
  fun stampNoteCamGeoLocation(
    context: Context,
    sourceUri: Uri,
    prefix: String,
    gpsCoords: String,
    locationBreadcrumb: String,
    timestampText: String = currentTimestampString(),
  ): NoteCamPhotoResult? {
    return runCatching {
      val originalBytes =
        context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() } ?: return null
      val decoded = decodeScaledBitmap(originalBytes, maxDimension = 1920) ?: return null
      val mutableBitmap =
        if (decoded.isMutable && decoded.config == Bitmap.Config.ARGB_8888) {
          decoded
        } else {
          decoded.copy(Bitmap.Config.ARGB_8888, true).also { decoded.recycle() }
        }

      val canvas = Canvas(mutableBitmap)
      val w = mutableBitmap.width.toFloat()
      val h = mutableBitmap.height.toFloat()

      val baseTextSize = (w * 0.024f).coerceIn(18f, 38f)
      val padding = baseTextSize * 0.75f
      val lineGap = baseTextSize * 1.32f

      val bgPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          // Low opacity (~50% translucent) so underlying site photo stays clearly visible
          color = Color.argb(128, 15, 23, 42)
        }
      val accentPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.argb(200, 229, 25, 75)
        }
      val titlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.argb(242, 255, 255, 255)
          textSize = baseTextSize
          isFakeBoldText = true
          setShadowLayer(3f, 1f, 1f, Color.argb(180, 0, 0, 0))
        }
      val bodyPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.argb(235, 241, 245, 249)
          textSize = baseTextSize * 0.9f
          setShadowLayer(3f, 1f, 1f, Color.argb(180, 0, 0, 0))
        }
      val timePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.argb(225, 226, 232, 240)
          textSize = baseTextSize * 0.85f
          setShadowLayer(3f, 1f, 1f, Color.argb(180, 0, 0, 0))
        }

      val gpsLine = "GPS: $gpsCoords"
      val locLine = locationBreadcrumb.ifBlank { "स्थान उपलब्ध" }
      val timeLine = timestampText

      val maxTextWidth =
        maxOf(
          titlePaint.measureText(gpsLine),
          bodyPaint.measureText(locLine),
          timePaint.measureText(timeLine),
        )
      val margin = (w * 0.025f).coerceAtLeast(14f)
      val accentW = baseTextSize * 0.28f
      val boxWidth = (maxTextWidth + (padding * 2f) + accentW + 8f).coerceAtMost(w - (margin * 2f))
      val bannerHeight = (padding * 2f) + (lineGap * 2.85f)

      val right = w - margin
      val bottom = h - margin
      val left = (right - boxWidth).coerceAtLeast(margin)
      val top = (bottom - bannerHeight).coerceAtLeast(margin)

      canvas.drawRoundRect(RectF(left, top, right, bottom), 14f, 14f, bgPaint)
      canvas.drawRoundRect(RectF(left, top, left + accentW, bottom), 6f, 6f, accentPaint)

      val textX = left + accentW + padding
      val line1Y = top + padding + (baseTextSize * 0.9f)
      val line2Y = line1Y + lineGap
      val line3Y = line2Y + lineGap

      canvas.drawText(gpsLine, textX, line1Y, titlePaint)
      canvas.drawText(locLine, textX, line2Y, bodyPaint)
      canvas.drawText(timeLine, textX, line3Y, timePaint)

      val jpegBytes = encodeJpeg(mutableBitmap, 90)
      mutableBitmap.recycle()

      val outDir = File(context.cacheDir, "survey_photos").apply { mkdirs() }
      val outFile = File(outDir, "${prefix.lowercase()}_${System.currentTimeMillis()}.jpg")
      FileOutputStream(outFile).use { it.write(jpegBytes) }

      val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
      val kb = (jpegBytes.size / 1024).coerceAtLeast(1)
      val summary = "$gpsLine\n$locLine\n$timeLine"

      NoteCamPhotoResult(
        uriString = uri.toString(),
        sizeKb = kb,
        geoSummary = summary,
      )
    }.getOrNull()
  }

  fun isDeviceLocationEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return runCatching { LocationManagerCompat.isLocationEnabled(manager) }.getOrDefault(false)
  }

  /**
   * Acquires live GPS location from the device and reverse-geocodes the place name.
   * Never fabricates demo GPS coordinates; reports LocationDisabled or Unavailable if no real fix exists.
   */
  suspend fun fetchLiveLocationOutcome(
    context: Context,
    fallbackLocationName: String,
  ): LiveLocationOutcome {
    val hasFine =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val hasCoarse =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) {
      return LiveLocationOutcome.Unavailable
    }

    val manager =
      context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return LiveLocationOutcome.Unavailable

    if (!LocationManagerCompat.isLocationEnabled(manager)) {
      return LiveLocationOutcome.LocationDisabled
    }

    val liveLocation = resolveLiveDeviceLocation(context, manager, hasFine)
      ?: return LiveLocationOutcome.Unavailable

    val latDir = if (liveLocation.latitude >= 0) "N" else "S"
    val lonDir = if (liveLocation.longitude >= 0) "E" else "W"
    val coordsText =
      String.format(
        Locale.US,
        "%.5f° %s, %.5f° %s",
        abs(liveLocation.latitude),
        latDir,
        abs(liveLocation.longitude),
        lonDir,
      )

    val liveAddress =
      reverseGeocodePlace(context, liveLocation.latitude, liveLocation.longitude)
        ?.takeIf { it.isNotBlank() }
        ?: fallbackLocationName

    return LiveLocationOutcome.Success(
      coordsText = coordsText,
      addressText = liveAddress,
      timestampText = currentTimestampString(),
    )
  }

  private suspend fun resolveLiveDeviceLocation(
    context: Context,
    manager: LocationManager,
    hasFine: Boolean,
  ): Location? {
    val providers = buildList {
      if (hasFine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        add(LocationManager.GPS_PROVIDER)
      }
      if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        add(LocationManager.NETWORK_PROVIDER)
      }
    }

    for (provider in providers) {
      val current = requestSingleCurrentFix(context, manager, provider)
      if (current != null) return current
    }

    // Fallback to a real device last-known fix only if available from OS providers (no static demo coordinates)
    return runCatching {
      val candidates = mutableListOf<Location>()
      if (hasFine) {
        manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { candidates.add(it) }
      }
      manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let { candidates.add(it) }
      manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)?.let { candidates.add(it) }
      candidates.maxByOrNull { it.time }
    }.getOrNull()
  }

  private suspend fun requestSingleCurrentFix(
    context: Context,
    manager: LocationManager,
    provider: String,
  ): Location? {
    return withTimeoutOrNull(4500L) {
      suspendCancellableCoroutine { cont ->
        val cancelSignal = CancellationSignal()
        cont.invokeOnCancellation { cancelSignal.cancel() }
        runCatching {
          LocationManagerCompat.getCurrentLocation(
            manager,
            provider,
            cancelSignal,
            ContextCompat.getMainExecutor(context),
          ) { location ->
            if (cont.isActive) {
              cont.resume(location)
            }
          }
        }.onFailure {
          if (cont.isActive) cont.resume(null)
        }
      }
    }
  }

  private suspend fun reverseGeocodePlace(
    context: Context,
    latitude: Double,
    longitude: Double,
  ): String? {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context, Locale("mr", "IN"))
    val addresses: List<Address>? =
      withTimeoutOrNull(2500L) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          suspendCancellableCoroutine { cont ->
            runCatching {
              geocoder.getFromLocation(latitude, longitude, 1) { list ->
                if (cont.isActive) cont.resume(list)
              }
            }.onFailure {
              if (cont.isActive) cont.resume(null)
            }
          }
        } else {
          withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            runCatching { geocoder.getFromLocation(latitude, longitude, 1) }.getOrNull()
          }
        }
      }

    val addr = addresses?.firstOrNull() ?: return null
    val parts =
      listOfNotNull(
        addr.subLocality,
        addr.locality,
        addr.subAdminArea,
        addr.adminArea,
      ).map { it.trim() }.filter { it.isNotBlank() }.distinct()
    return parts.take(3).joinToString(" · ").takeIf { it.isNotBlank() }
  }

  fun currentTimestampString(): String {
    return SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
  }

  private fun decodeScaledBitmap(bytes: ByteArray, maxDimension: Int): Bitmap? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
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
