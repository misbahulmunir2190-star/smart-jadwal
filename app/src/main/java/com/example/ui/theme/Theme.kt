package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = GoldDark,
    background = DarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = SurfaceLight,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenPrimary,
    secondary = GreenSecondary,
    tertiary = GoldAccent,
    tertiaryContainer = GoldContainer,
    background = LightBackground,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight
)

@Composable
fun SmartSchedulerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
