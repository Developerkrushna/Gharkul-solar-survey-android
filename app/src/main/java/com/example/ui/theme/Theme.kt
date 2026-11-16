package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GharkulLightColorScheme =
  lightColorScheme(
    primary = BrandNavy,
    onPrimary = Color.White,
    primaryContainer = BannerIceBlue,
    onPrimaryContainer = BrandNavyDark,
    secondary = BrandCrimson,
    onSecondary = Color.White,
    secondaryContainer = PendingRoseBg,
    onSecondaryContainer = PendingRoseText,
    tertiary = BrandNavyDark,
    onTertiary = Color.White,
    background = SurfaceCanvas,
    onBackground = TextPrimary,
    surface = CardWhite,
    onSurface = TextPrimary,
    surfaceVariant = BannerIceBlue,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = DividerLight,
    error = BrandCrimson,
    onError = Color.White,
  )

@Composable
fun GharkulSurveyTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = GharkulLightColorScheme,
    typography = GharkulTypography,
    content = content,
  )
}
