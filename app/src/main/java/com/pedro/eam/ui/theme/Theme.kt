package com.pedro.eam.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = TextBlack,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = TextBlack,
    secondary = BlueSecondary,
    onSecondary = TextBlack,
    secondaryContainer = Color(0xFFE1F5FE),
    onSecondaryContainer = TextBlack,
    tertiary = BlueDark,
    onTertiary = TextBlack,
    background = WhiteBackground,
    onBackground = TextBlack,
    surface = WhiteBackground,
    onSurface = TextBlack,
    surfaceVariant = SurfaceVariantColor,
    onSurfaceVariant = BluePrimary
)

@Composable
fun EAMTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = true
            windowInsetsController.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
