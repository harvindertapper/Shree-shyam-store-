package com.aistudio.shreeshyamstore.pqwzkb.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE18B68),
    onPrimary = Color(0xFF3E160B),
    primaryContainer = Color(0xFF6E2B18),
    onPrimaryContainer = Color(0xFFFFDBCA),
    secondary = Color(0xFFD8C4B6),
    onSecondary = Color(0xFF392A22),
    background = Color(0xFF171412),
    surface = Color(0xFF211D1A),
    surfaceVariant = Color(0xFF4B4039),
    onBackground = Color(0xFFF1E8E1),
    onSurface = Color(0xFFF1E8E1),
    onSurfaceVariant = Color(0xFFD5C5BA),
    outline = Color(0xFF9A897D),
    outlineVariant = Color(0xFF594B42),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
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

