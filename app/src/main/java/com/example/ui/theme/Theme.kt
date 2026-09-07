package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = HdPrimaryDark,
    onPrimary = HdOnPrimaryDark,
    primaryContainer = HdPrimaryContainerDark,
    onPrimaryContainer = HdOnPrimaryContainerDark,
    secondary = HdSecondaryDark,
    onSecondary = HdOnSecondaryDark,
    secondaryContainer = HdSecondaryContainerDark,
    onSecondaryContainer = HdOnSecondaryContainerDark,
    tertiary = HdTertiaryDark,
    onTertiary = HdOnTertiaryDark,
    tertiaryContainer = HdTertiaryContainerDark,
    onTertiaryContainer = HdOnTertiaryContainerDark,
    background = HdBackgroundDark,
    surface = HdSurfaceDark,
    surfaceContainer = HdSurfaceContainerDark,
    surfaceContainerHigh = HdSurfaceContainerHighDark,
    surfaceContainerHighest = HdSurfaceContainerHighestDark,
    onBackground = HdOnSurfaceDark,
    onSurface = HdOnSurfaceDark,
    onSurfaceVariant = HdOnSurfaceVariantDark,
    outline = HdOutlineDark,
    outlineVariant = HdOutlineVariantDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = HdPrimaryLight,
    onPrimary = HdOnPrimaryLight,
    primaryContainer = HdPrimaryContainerLight,
    onPrimaryContainer = HdOnPrimaryContainerLight,
    secondary = HdSecondaryLight,
    onSecondary = HdOnSecondaryLight,
    secondaryContainer = HdSecondaryContainerLight,
    onSecondaryContainer = HdOnSecondaryContainerLight,
    tertiary = HdTertiaryLight,
    onTertiary = HdOnTertiaryLight,
    tertiaryContainer = HdTertiaryContainerLight,
    onTertiaryContainer = HdOnTertiaryContainerLight,
    background = HdBackgroundLight,
    surface = HdSurfaceLight,
    surfaceContainer = HdSurfaceContainerLight,
    surfaceContainerHigh = HdSurfaceContainerHighLight,
    surfaceContainerHighest = HdSurfaceContainerHighestLight,
    onBackground = HdOnSurfaceLight,
    onSurface = HdOnSurfaceLight,
    onSurfaceVariant = HdOnSurfaceVariantLight,
    outline = HdOutlineLight,
    outlineVariant = HdOutlineVariantLight,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
