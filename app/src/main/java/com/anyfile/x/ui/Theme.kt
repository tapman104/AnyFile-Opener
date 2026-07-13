package com.anyfile.x

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PurpleAccent = Color(0xFF7B61FF)

private val DarkColorScheme = darkColorScheme(
    primary = PurpleAccent,
    secondary = PurpleAccent,
    tertiary = PurpleAccent
)

private val LightColorScheme = lightColorScheme(
    primary = PurpleAccent,
    secondary = PurpleAccent,
    tertiary = PurpleAccent
)

private val AmoledColorScheme = darkColorScheme(
    primary = PurpleAccent,
    secondary = PurpleAccent,
    tertiary = PurpleAccent,
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF0D0D0D),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun AnyFileOpenerTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM_DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themePreference) {
        ThemePreference.SYSTEM_DEFAULT -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        ThemePreference.AMOLED -> AmoledColorScheme
        ThemePreference.MATERIAL_YOU -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
    }

    val isActuallyDark = when (themePreference) {
        ThemePreference.SYSTEM_DEFAULT -> darkTheme
        ThemePreference.AMOLED -> true
        ThemePreference.MATERIAL_YOU -> darkTheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isActuallyDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
