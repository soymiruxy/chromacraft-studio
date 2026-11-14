package com.chromacraft.studio.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Paleta oscura insignia de estudio (Fallback Material 3)
private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioAccent,
    onPrimary = Color.White,
    primaryContainer = StudioAccentDark,
    onPrimaryContainer = StudioAccentLight,
    secondary = StudioSecondary,
    onSecondary = Color.White,
    tertiary = StudioTertiary,
    background = StudioBlack,
    onBackground = TextPrimary,
    surface = StudioDark,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurface,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder,
    outlineVariant = StudioBorderLight
)

// Paleta clara limpia y minimalista
private val StudioLightColorScheme = lightColorScheme(
    primary = StudioAccent,
    onPrimary = Color.White,
    primaryContainer = StudioAccentLight,
    onPrimaryContainer = StudioAccentDark,
    secondary = StudioSecondary,
    onSecondary = Color.White,
    tertiary = StudioTertiary,
    background = Color(0xFFF8F9FC),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFEFF1F8),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFFDDE1EB)
)

/**
 * Tema principal de ChromaCraft Studio basado en Material You (Material 3).
 * Soporta colores dinámicos nativos en Android 12+ (API 31+) adaptados al fondo de pantalla del usuario.
 */
@Composable
fun ChromaCraftStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> StudioDarkColorScheme
        else -> StudioLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = !darkTheme
            insets.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudioTypography,
        content = content
    )
}
