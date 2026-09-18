package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.AppThemeMode

// Bright, clean Material Design 3 Color Scheme (Blue Primary & Light Gray Background)
private val SmartDrivoColorScheme = lightColorScheme(
    primary = BluePrimary,                       // Clean vibrant Blue (#1E88E5)
    onPrimary = Color.White,
    primaryContainer = BlueContainer,            // Light Blue pill/capsule indicator (#E3F2FD)
    onPrimaryContainer = BlueDark,
    secondary = BlueLight,
    onSecondary = Color.White,
    secondaryContainer = BlueContainer,          // Light Blue pill
    onSecondaryContainer = BlueDark,
    background = LightBackground,                // Light Gray screen background (#F5F5F5)
    onBackground = TextDarkPrimary,              // Slate dark text (#0F172A)
    surface = CardBackground,                    // Pure White card surface (#FFFFFF)
    onSurface = TextDarkPrimary,                 // Slate dark text
    surfaceVariant = CardBackground,             // Pure White card surface (#FFFFFF)
    onSurfaceVariant = TextDarkSecondary,        // Muted text
    outline = CardBorderDefault,                 // Flat subtle border (#E2E8F0)
    outlineVariant = Color(0xFFF1F5F9),
    error = StatusInactiveRed,
    onError = Color.White
)

@Composable
fun SmartDrivoTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    // Bright, colorful theme is applied as default everywhere
    MaterialTheme(
        colorScheme = SmartDrivoColorScheme,
        typography = Typography,
        content = content
    )
}

