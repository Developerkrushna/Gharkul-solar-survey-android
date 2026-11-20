package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.BannerIceBlue
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.PendingRoseBg
import com.example.ui.theme.PendingRoseText
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

/**
 * Circular arrow sync/refresh button shown in both Executive and Installation top bars.
 * Rotates smoothly while syncing and displays a crimson count badge when offline items are queued.
 */
@Composable
fun CircularSyncTopBarButton(
  isSyncing: Boolean,
  pendingSyncCount: Int,
  onClick: () -> Unit,
  testTag: String,
  contentDescription: String = "डेटा सिंक व रिफ्रेश करा",
  modifier: Modifier = Modifier,
) {
  val rotation =
    if (isSyncing) {
      val transition = rememberInfiniteTransition(label = "sync_arrow_spin")
      val angle by
        transition.animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 900, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "sync_arrow_angle",
        )
      angle
    } else {
      0f
    }

  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    IconButton(
      onClick = onClick,
      enabled = !isSyncing,
      modifier = Modifier.minimumInteractiveComponentSize().testTag(testTag),
    ) {
      Surface(
        shape = CircleShape,
        color = if (pendingSyncCount > 0) PendingRoseBg else BannerIceBlue,
        border =
          BorderStroke(
            width = 1.dp,
            color = if (pendingSyncCount > 0) BrandCrimson.copy(alpha = 0.45f) else BorderSubtle,
          ),
        modifier = Modifier.size(36.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = contentDescription,
            tint = if (pendingSyncCount > 0) BrandCrimson else BrandNavy,
            modifier = Modifier.size(20.dp).rotate(rotation),
          )
        }
      }
    }

    if (pendingSyncCount > 0) {
      Surface(
        shape = CircleShape,
        color = BrandCrimson,
        modifier =
          Modifier.align(Alignment.TopEnd)
            .offset(x = (-2).dp, y = 2.dp)
            .size(18.dp)
            .testTag("${testTag}_pending_badge"),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = if (pendingSyncCount > 9) "9+" else pendingSyncCount.toString(),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 10.sp,
          )
        }
      }
    }
  }
}

/**
 * Displays a clear Marathi sync/offline error banner with a one-tap Retry action (min 48dp touch target).
 */
@Composable
fun SyncErrorRetryBanner(
  errorMessage: String?,
  isSyncing: Boolean,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (errorMessage.isNullOrBlank()) return
  Surface(
    modifier = modifier.fillMaxWidth().testTag("sync_error_banner"),
    shape = RoundedCornerShape(14.dp),
    color = PendingRoseBg,
    border = BorderStroke(1.dp, BrandCrimson.copy(alpha = 0.35f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = Icons.Outlined.CloudOff,
        contentDescription = "ऑफलाइन किंवा सिंक त्रुटी",
        tint = PendingRoseText,
        modifier = Modifier.size(22.dp),
      )
      Text(
        text = errorMessage,
        style = MaterialTheme.typography.bodySmall,
        color = PendingRoseText,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.weight(1f),
      )
      TextButton(
        onClick = onRetry,
        enabled = !isSyncing,
        modifier = Modifier.height(48.dp).testTag("sync_retry_button"),
      ) {
        Icon(
          imageVector = Icons.Outlined.Refresh,
          contentDescription = null,
          tint = BrandNavy,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "पुन्हा प्रयत्न",
          style = MaterialTheme.typography.labelLarge,
          color = BrandNavy,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

/**
 * Loads local cached images instantaneously and handles remote Cloudflare R2 (`*.r2.dev`) URLs
 * gracefully with an explicit retry button if an Indian mobile ISP blocks the direct preview.
 */
@Composable
fun RemoteAwarePhotoPreview(
  photoUri: String,
  contentDescription: String,
  modifier: Modifier = Modifier,
  compact: Boolean = false,
  isMarathi: Boolean = true,
) {
  val context = LocalContext.current
  var retryAttempt by remember(photoUri) { mutableIntStateOf(0) }

  val request =
    remember(photoUri, retryAttempt) {
      ImageRequest.Builder(context)
        .data(photoUri)
        .setParameter("retry_attempt", retryAttempt, memoryCacheKey = "$photoUri#$retryAttempt")
        .crossfade(true)
        .build()
    }

  SubcomposeAsyncImage(
    model = request,
    contentDescription = contentDescription,
    modifier = modifier,
    contentScale = ContentScale.Crop,
    loading = {
      Box(
        modifier = Modifier.fillMaxSize().background(BannerIceBlue),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          color = BrandNavy,
          strokeWidth = 2.dp,
          modifier = Modifier.size(if (compact) 16.dp else 26.dp),
        )
      }
    },
    error = {
      Box(
        modifier = Modifier.fillMaxSize().background(BannerIceBlue),
        contentAlignment = Alignment.Center,
      ) {
        if (compact) {
          TextButton(
            onClick = { retryAttempt++ },
            modifier = Modifier.fillMaxSize().testTag("r2_preview_retry_compact"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Refresh,
              contentDescription = if (isMarathi) "फोटो पुन्हा लोड करा" else "Retry photo preview",
              tint = BrandNavy,
              modifier = Modifier.size(18.dp),
            )
          }
        } else {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.BrokenImage,
              contentDescription = null,
              tint = TextMuted,
              modifier = Modifier.size(28.dp),
            )
            Text(
              text =
                if (isMarathi) "फोटो प्रिव्ह्यू लोड झाला नाही (सर्व्हरवर सुरक्षित आहे)"
                else "Preview unavailable on current network (photo saved on R2)",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
              textAlign = TextAlign.Center,
            )
            OutlinedButton(
              onClick = { retryAttempt++ },
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, BrandNavy),
              modifier = Modifier.height(48.dp).testTag("r2_preview_retry_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = BrandNavy,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isMarathi) "पुन्हा लोड करा" else "Retry Preview",
                color = BrandNavy,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    },
  )
}
