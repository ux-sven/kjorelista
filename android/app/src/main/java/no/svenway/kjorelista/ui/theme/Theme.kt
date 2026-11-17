package no.svenway.kjorelista.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Fargeprofil fra Kjørelista-logoen: lilla gradient (#63517B → #7E6F91) og gull (#DBC388).
// Lilla er hovedfarge i lys modus, gull i mørk modus. Holdes lik ios/Kjorelista/Core/Theme.swift.
@Immutable
data class AppColors(
    val background: Color,
    val foreground: Color,
    val card: Color,
    val primary: Color,
    val primaryForeground: Color,
    val muted: Color,
    val mutedForeground: Color,
    val destructive: Color,
    val destructiveForeground: Color,
    val success: Color,
    val warning: Color,
    val border: Color,
    val inputBackground: Color,
)

val LightAppColors = AppColors(
    background = Color(0xFFF5F3F8),
    foreground = Color(0xFF2E2540),
    card = Color(0xFFFFFFFF),
    primary = Color(0xFF5A4873),
    primaryForeground = Color(0xFFFFFFFF),
    muted = Color(0xFFEEEAF3),
    mutedForeground = Color(0xFF6B6280),
    destructive = Color(0xFFC0392B),
    destructiveForeground = Color(0xFFFFFFFF),
    success = Color(0xFF27AE60),
    warning = Color(0xFFD68910),
    border = Color(0xFFE3DEEA),
    inputBackground = Color(0xFFF8F6FA),
)

val DarkAppColors = AppColors(
    background = Color(0xFF1C1726),
    foreground = Color(0xFFF5F2F9),
    card = Color(0xFF2A2337),
    primary = Color(0xFFDBC388),
    primaryForeground = Color(0xFF2E2540),
    muted = Color(0xFF3A3148),
    mutedForeground = Color(0xFFC3BBD0),
    destructive = Color(0xFFEF4444),
    destructiveForeground = Color(0xFFF8FAFC),
    success = Color(0xFF10B981),
    warning = Color(0xFFF59E0B),
    border = Color(0xFF4A4058),
    inputBackground = Color(0xFF1C1726),
)

/** Merkevarefarger (like i lys og mørk modus) */
object Brand {
    val gold = Color(0xFFDBC388)
    val onGold = Color(0xFF3D3152)
    val purpleLight = Color(0xFF7E6F91)
    val purple = Color(0xFF63517B)
    val purpleDark = Color(0xFF4E3F66)
}

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

@Composable
fun KjorelistaTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) DarkAppColors else LightAppColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.primary,
            onPrimary = c.primaryForeground,
            primaryContainer = Color(0xFF4A3D2A),
            onPrimaryContainer = Color(0xFFF3E6C4),
            secondaryContainer = Color(0xFF3A3148),
            onSecondaryContainer = Color(0xFFE8E1F2),
            background = c.background,
            onBackground = c.foreground,
            surface = c.background,
            onSurface = c.foreground,
            surfaceVariant = c.muted,
            onSurfaceVariant = c.mutedForeground,
            surfaceContainerLowest = c.background,
            surfaceContainerLow = c.card,
            surfaceContainer = c.card,
            surfaceContainerHigh = Color(0xFF332A42),
            surfaceContainerHighest = c.muted,
            outline = c.border,
            outlineVariant = c.border,
            error = c.destructive,
            onError = c.destructiveForeground,
        )
    } else {
        lightColorScheme(
            primary = c.primary,
            onPrimary = c.primaryForeground,
            primaryContainer = Color(0xFFE6DFF0),
            onPrimaryContainer = Color(0xFF2E2540),
            secondaryContainer = Color(0xFFE6DFF0),
            onSecondaryContainer = Color(0xFF2E2540),
            background = c.background,
            onBackground = c.foreground,
            surface = c.background,
            onSurface = c.foreground,
            surfaceVariant = c.muted,
            onSurfaceVariant = c.mutedForeground,
            surfaceContainerLowest = c.card,
            surfaceContainerLow = c.card,
            surfaceContainer = c.card,
            surfaceContainerHigh = c.card,
            surfaceContainerHighest = c.muted,
            outline = Color(0xFFC9C0D6),
            outlineVariant = c.border,
            error = c.destructive,
            onError = c.destructiveForeground,
        )
    }
    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
