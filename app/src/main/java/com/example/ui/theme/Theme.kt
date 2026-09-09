package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BotanicalForest,
    onPrimary = BotanicalSurface,
    primaryContainer = BotanicalSage,
    onPrimaryContainer = BotanicalSurface,
    secondary = BotanicalMint,
    onSecondary = BotanicalTextPrimary,
    background = BotanicalCream,
    onBackground = BotanicalTextPrimary,
    surface = BotanicalSurface,
    onSurface = BotanicalTextPrimary,
    surfaceVariant = BotanicalCardBg,
    onSurfaceVariant = BotanicalTextSecondary,
    outline = BotanicalCardBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = BotanicalMint,
    onPrimary = DarkBackground,
    primaryContainer = BotanicalSage,
    onPrimaryContainer = DarkTextPrimary,
    secondary = BotanicalSage,
    onSecondary = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardBg,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkCardBorder
)

@Composable
fun RefluxTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
