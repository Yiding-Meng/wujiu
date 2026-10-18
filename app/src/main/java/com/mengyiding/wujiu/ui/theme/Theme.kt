package com.mengyiding.wujiu.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = ForestContainerLight,
    onPrimaryContainer = InkLight,
    secondary = Color(0xFF6B7B3C),
    onSecondary = Color.White,
    secondaryContainer = Sand,
    onSecondaryContainer = InkLight,
    background = CanvasLight,
    onBackground = InkLight,
    surface = CardLight,
    onSurface = InkLight,
    surfaceVariant = ForestContainerLight,
    onSurfaceVariant = MutedLight,
    outline = DividerLight,
    error = DangerLight,
)

private val DarkColors = darkColorScheme(
    primary = ForestLight,
    onPrimary = Color(0xFF0A2B1E),
    primaryContainer = ForestContainerDark,
    onPrimaryContainer = InkDark,
    secondary = Color(0xFFD4C48E),
    onSecondary = Color(0xFF302D16),
    secondaryContainer = SandDark,
    onSecondaryContainer = InkDark,
    background = CanvasDark,
    onBackground = InkDark,
    surface = CardDark,
    onSurface = InkDark,
    surfaceVariant = ForestContainerDark,
    onSurfaceVariant = MutedDark,
    outline = DividerDark,
    error = DangerDark,
)

@Composable
fun WujiuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = WujiuTypography,
        content = content,
    )
}
