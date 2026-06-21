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
    primary = ChopGreen,
    secondary = ChopDarkGreen,
    tertiary = ChopOrange,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = ChopWhite,
    onSecondary = ChopWhite,
    onBackground = TextDark,
    onSurface = TextDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ChopGreen,
    secondary = ChopDarkGreen,
    tertiary = ChopOrange,
    background = BackgroundLight,
    surface = ChopWhite,
    onPrimary = ChopWhite,
    onSecondary = ChopWhite,
    onBackground = ChopBlack,
    onSurface = ChopBlack
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // For e-commerce, disable dynamic coloring to fully enforce CHOP.KZ brand colors
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
