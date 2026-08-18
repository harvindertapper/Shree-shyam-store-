package com.aistudio.shreeshyamstore.pqwzkb.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SaffronPrimary,
    secondary = SlateSecondary,
    tertiary = SaffronDark,
    background = WarmCreamBg,
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
    secondary = SlateSecondary,
    onSecondary = SurfaceWhite,
    secondaryContainer = SlateContainer,
    onSecondaryContainer = TextNearBlack,
    background = WarmCreamBg,
    surface = SurfaceWhite,
    onBackground = TextNearBlack,
    onSurface = TextNearBlack,
    surfaceVariant = SlateContainer,
    onSurfaceVariant = TextMediumGray,
    outline = BorderStrong,
    outlineVariant = SurfaceCardBorder,
    error = ErrorRed,
    errorContainer = ErrorRedLight,
    onError = SurfaceWhite,
    onErrorContainer = ErrorRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

