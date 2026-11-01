package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.AppThemeMode

private val PremiumLight = lightColorScheme(
    primary = Color(0xFF0877EA), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEEFF), onPrimaryContainer = Color(0xFF102C59),
    secondary = Color(0xFF13B6C7), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7F9FC), onSecondaryContainer = Color(0xFF0B3A57),
    background = Color(0xFFF4F9FF), onBackground = Color(0xFF10213D),
    surface = Color.White, onSurface = Color(0xFF10213D),
    surfaceVariant = Color(0xFFECF5FF), onSurfaceVariant = Color(0xFF5B728E),
    outline = Color(0xFFCDE3FC), outlineVariant = Color(0xFFE2EFFD),
    error = Color(0xFFCC3148), onError = Color.White
)
private val PremiumDark = darkColorScheme(
    primary = Color(0xFF6AB9FF), onPrimary = Color(0xFF071A35),
    primaryContainer = Color(0xFF213E6B), onPrimaryContainer = Color(0xFFE0F3FF),
    secondary = Color(0xFF64DADB), onSecondary = Color(0xFF062F3C),
    secondaryContainer = Color(0xFF19475A), onSecondaryContainer = Color(0xFFE0FFFF),
    background = Color(0xFF0A1528), onBackground = Color(0xFFF3F8FF),
    surface = Color(0xFF172A49), onSurface = Color(0xFFF3F8FF),
    surfaceVariant = Color(0xFF203A60), onSurfaceVariant = Color(0xFFBED1EB),
    outline = Color(0xFF365A86), outlineVariant = Color(0xFF294467),
    error = Color(0xFFFF7590), onError = Color(0xFF360616)
)
@Composable
fun SmartDrivoTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) PremiumDark else PremiumLight,
        typography = Typography,
        content = content
    )
}
