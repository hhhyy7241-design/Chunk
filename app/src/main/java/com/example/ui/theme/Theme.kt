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
    primary = ProBlueBright,
    onPrimary = Color.White,
    primaryContainer = ProBlueContainer,
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = ProCyanAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF034A6E),
    onSecondaryContainer = Color(0xFFC7E7FF),
    tertiary = ProEmeraldSuccess,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = ProDarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = ProDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = ProDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = ProDarkBorder,
    outlineVariant = ProDarkBorderSubtle,
    error = ProRoseError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ProBluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = ProCyanAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = ProEmeraldSuccess,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF065F46),
    background = ProLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = ProLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = ProLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = ProLightBorder,
    outlineVariant = ProLightBorderSubtle,
    error = ProRoseError,
    onError = Color.White
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
