package com.aistudio.shreeshyamstore.pqwzkb.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SaffronLight,
    onPrimary = SaffronDark,
    primaryContainer = SaffronDark,
    onPrimaryContainer = SaffronLight,
    secondary = Color(0xFFCBD5E1),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFFF0ABFC),
    onTertiary = Color(0xFF4A044E),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
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
