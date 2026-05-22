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
    primary = BentoPrimaryDark,
    secondary = BentoSecondaryContainerDark,
    tertiary = BentoTertiaryDark,
    background = BentoBackgroundDark,
    surface = BentoSurfaceDark,
    onPrimary = BentoOnPrimaryDark,
    onSecondary = BentoOnSecondaryContainerDark,
    onTertiary = BentoOnPrimaryDark,
    onBackground = BentoOnSurfaceDark,
    onSurface = BentoOnSurfaceDark,
    surfaceVariant = BentoSurfaceDark,
    onSurfaceVariant = BentoOnSurfaceVariantDark,
    outline = BentoOutlineDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BentoPrimaryLight,
    secondary = BentoSecondaryContainerLight,
    tertiary = BentoTertiaryLight,
    background = BentoBackgroundLight,
    surface = BentoSurfaceLight,
    onPrimary = BentoOnPrimaryLight,
    onSecondary = BentoOnSecondaryContainerLight,
    onTertiary = BentoOnPrimaryLight,
    onBackground = BentoOnSurfaceLight,
    onSurface = BentoOnSurfaceLight,
    surfaceVariant = BentoSurfaceLight,
    onSurfaceVariant = BentoOnSurfaceVariantLight,
    outline = BentoOutlineLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Set dynamicColor = false by default to ensure the beautiful Bento palette is seen
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
