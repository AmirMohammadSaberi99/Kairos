package com.kairos.daily.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val KairosBackground = Color(0xFF090B10)
val KairosSurface = Color(0xFF12151D)
val KairosSurfaceHigh = Color(0xFF1A1E29)
val KairosAccent = Color(0xFF8B7CFF)
val KairosMint = Color(0xFF70E1B0)
val KairosText = Color(0xFFF2F1F6)
val KairosMuted = Color(0xFF989BA8)

private val colors = darkColorScheme(
    primary = KairosAccent,
    onPrimary = Color.White,
    secondary = KairosMint,
    background = KairosBackground,
    onBackground = KairosText,
    surface = KairosSurface,
    onSurface = KairosText,
    surfaceVariant = KairosSurfaceHigh,
    onSurfaceVariant = KairosMuted,
    outline = Color(0xFF2A2E3A),
    error = Color(0xFFFF7D8B)
)

@Composable
fun KairosTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = KairosBackground.toArgb()
            window.navigationBarColor = KairosBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(colorScheme = colors, typography = KairosTypography, content = content)
}
