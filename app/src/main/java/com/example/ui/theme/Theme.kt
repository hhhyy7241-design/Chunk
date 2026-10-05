package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TechCyanPrimary,
    onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF004E5B),
    onPrimaryContainer = Color(0xFFA6EEFF),
    secondary = TechCyanGlow,
    onSecondary = Color(0xFF00344D),
    secondaryContainer = Color(0xFF004C6E),
    onSecondaryContainer = Color(0xFFC7E7FF),
    tertiary = TechBlueSecondary,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = TechBlueContainer,
    onTertiaryContainer = Color(0xFFD6E3FF),
    background = TechDarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = TechDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = TechDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = TechDarkBorder,
    outlineVariant = Color(0xFF1E293B),
    error = TechRoseError,
    onError = Color(0xFFFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = TechLightPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC3E8FF),
    onPrimaryContainer = Color(0xFF001E2E),
    secondary = TechLightSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE2FF),
    onSecondaryContainer = Color(0xFF00184A),
    tertiary = TechCyanPrimary,
    onTertiary = Color(0xFF00363F),
    tertiaryContainer = Color(0xFFA6EEFF),
    onTertiaryContainer = Color(0xFF004E5B),
    background = TechLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = TechLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = TechLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = TechLightBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = TechRoseError,
    onError = Color(0xFFFFFFFF)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
