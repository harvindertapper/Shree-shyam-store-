package com.harrylabs.shreeshyamstore.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SaffronPrimary,
    secondary = BrownSecondary,
    tertiary = SaffronDark,
    background = WarmCreamBg, // Force light background for a clean, consistent daylight layout
    surface = SurfaceWhite,
    onPrimary = SurfaceWhite,
    onSecondary = SurfaceWhite,
    onBackground = TextNearBlack,
    onSurface = TextNearBlack
)

private val LightColorScheme = lightColorScheme(
    primary = SaffronPrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = SaffronLight,
    onPrimaryContainer = SaffronDark,
    secondary = BrownSecondary,
    onSecondary = SurfaceWhite,
    secondaryContainer = BrownContainer,
    onSecondaryContainer = TextNearBlack,
    background = WarmCreamBg,
    surface = SurfaceWhite,
    onBackground = TextNearBlack,
    onSurface = TextNearBlack,
    surfaceVariant = BrownContainer,
    onSurfaceVariant = TextNearBlack,
    outline = BorderStrong,
    error = ErrorRed,
    onError = SurfaceWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Force disabled to maintain high contrast and custom branding
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
