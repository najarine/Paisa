package com.paisa.najarine.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PaisaLightColorScheme = lightColorScheme(
    primary = PaisaTealPrimary,
    onPrimary = Color.White,
    primaryContainer = PaisaTealContainer,
    onPrimaryContainer = PaisaOnTealContainer,
    secondary = PaisaTealDark,
    onSecondary = Color.White,
    secondaryContainer = PaisaTealLight,
    onSecondaryContainer = Color.White,
    tertiary = PaisaGoldAmber,
    onTertiary = Color.White,
    tertiaryContainer = PaisaGoldLight,
    onTertiaryContainer = PaisaTextPrimary,
    background = PaisaBackground,
    onBackground = PaisaTextPrimary,
    surface = PaisaSurface,
    onSurface = PaisaTextPrimary,
    surfaceVariant = PaisaSurfaceVariant,
    onSurfaceVariant = PaisaTextSecondary,
    outline = PaisaBorder,
    outlineVariant = PaisaBorderSubtle,
    error = PaisaExpenseRed,
    onError = Color.White,
    errorContainer = PaisaExpenseLight,
    onErrorContainer = PaisaTextPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = PaisaLightColorScheme
    val typography = Typography

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
