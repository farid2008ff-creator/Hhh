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

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CinemaAmber,
    onPrimary = Color.Black,
    secondary = CinemaAmberLight,
    onSecondary = Color.Black,
    tertiary = Pink80,
    background = CinemaObsidian,
    onBackground = CinemaTextPrimary,
    surface = CinemaSurface,
    onSurface = CinemaTextPrimary,
    surfaceVariant = CinemaSurfaceVariant,
    onSurfaceVariant = CinemaTextSecondary,
    outline = CinemaBorder
)

private val LightColorScheme = darkColorScheme( // Enforce eye-safe movie dark theme even as light fallback
    primary = CinemaAmber,
    onPrimary = Color.Black,
    secondary = CinemaAmberLight,
    onSecondary = Color.Black,
    tertiary = Pink40,
    background = CinemaObsidian,
    onBackground = CinemaTextPrimary,
    surface = CinemaSurface,
    onSurface = CinemaTextPrimary,
    surfaceVariant = CinemaSurfaceVariant,
    onSurfaceVariant = CinemaTextSecondary,
    outline = CinemaBorder
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Enforce consistent Immersive Theme colors across devices
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
