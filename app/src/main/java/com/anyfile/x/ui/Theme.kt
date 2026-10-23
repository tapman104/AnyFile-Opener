package com.anyfile.x.ui

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

private val PrimaryPurple = Color(0xFFD0BCFF)
private val OnPrimaryPurple = Color(0xFF381E72)
private val PrimaryContainerPurple = Color(0xFF4F378B)
private val OnPrimaryContainerPurple = Color(0xFFEADDFF)

private val SecondaryPurple = Color(0xFFCCC2DC)
private val OnSecondaryPurple = Color(0xFF332D41)
private val SecondaryContainerPurple = Color(0xFF4A4458)
private val OnSecondaryContainerPurple = Color(0xFFE8DEF8)

private val TertiaryPink = Color(0xFFEFB8C8)
private val OnTertiaryPink = Color(0xFF492532)
private val TertiaryContainerPink = Color(0xFF633B48)
private val OnTertiaryContainerPink = Color(0xFFFFD8E4)

private val DarkBackground = Color(0xFF141218)
private val DarkOnBackground = Color(0xFFE6E0E9)
private val DarkSurface = Color(0xFF141218)
private val DarkOnSurface = Color(0xFFE6E0E9)
private val DarkSurfaceVariant = Color(0xFF49454F)
private val DarkOnSurfaceVariant = Color(0xFFCAC4D0)
private val DarkOutline = Color(0xFF938F99)
private val DarkOutlineVariant = Color(0xFF44424A)

private val DarkSurfaceContainer = Color(0xFF211F26)
private val DarkSurfaceContainerHigh = Color(0xFF2B2930)
private val DarkSurfaceContainerHighest = Color(0xFF36343B)
private val DarkSurfaceContainerLow = Color(0xFF1D1B20)
private val DarkSurfaceContainerLowest = Color(0xFF0F0D13)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = OnPrimaryPurple,
    primaryContainer = PrimaryContainerPurple,
    onPrimaryContainer = OnPrimaryContainerPurple,
    secondary = SecondaryPurple,
    onSecondary = OnSecondaryPurple,
    secondaryContainer = SecondaryContainerPurple,
    onSecondaryContainer = OnSecondaryContainerPurple,
    tertiary = TertiaryPink,
    onTertiary = OnTertiaryPink,
    tertiaryContainer = TertiaryContainerPink,
    onTertiaryContainer = OnTertiaryContainerPink,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

private val AmoledColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = OnPrimaryPurple,
    primaryContainer = PrimaryContainerPurple,
    onPrimaryContainer = OnPrimaryContainerPurple,
    secondary = SecondaryPurple,
    onSecondary = OnSecondaryPurple,
    secondaryContainer = SecondaryContainerPurple,
    onSecondaryContainer = OnSecondaryContainerPurple,
    tertiary = TertiaryPink,
    onTertiary = OnTertiaryPink,
    background = Color(0xFF000000),
    onBackground = Color.White,
    surface = Color(0xFF000000),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainer = Color(0xFF0D0D0D),
    surfaceContainerHigh = Color(0xFF161616),
    surfaceContainerHighest = Color(0xFF202020),
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainerLowest = Color(0xFF000000),
    outline = DarkOutline,
    outlineVariant = Color(0xFF282828)
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
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isActuallyDark
            controller.isAppearanceLightNavigationBars = !isActuallyDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
