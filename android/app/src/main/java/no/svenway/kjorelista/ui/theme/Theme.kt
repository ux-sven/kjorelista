package no.svenway.kjorelista.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Fargene er hentet fra kjoreliste-app/src/lib/theme.tsx (som igjen følger webløsningen)
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
    background = Color(0xFFF6F7FA),
    foreground = Color(0xFF2C3E50),
    card = Color(0xFFFFFFFF),
    primary = Color(0xFF3498DB),
    primaryForeground = Color(0xFFFFFFFF),
    muted = Color(0xFFF1F2F6),
    mutedForeground = Color(0xFF5A6C7D),
    destructive = Color(0xFFC0392B),
    destructiveForeground = Color(0xFFFFFFFF),
    success = Color(0xFF27AE60),
    warning = Color(0xFFD68910),
    border = Color(0xFFE5E7EB),
    inputBackground = Color(0xFFF8F9FB),
)

val DarkAppColors = AppColors(
    background = Color(0xFF0F172A),
    foreground = Color(0xFFF8FAFC),
    card = Color(0xFF1E293B),
    primary = Color(0xFF60A5FA),
    primaryForeground = Color(0xFF0F172A),
    muted = Color(0xFF334155),
    mutedForeground = Color(0xFFCBD5E1),
    destructive = Color(0xFFEF4444),
    destructiveForeground = Color(0xFFF8FAFC),
    success = Color(0xFF10B981),
    warning = Color(0xFFF59E0B),
    border = Color(0xFF475569),
    inputBackground = Color(0xFF0F172A),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

@Composable
fun KjorelistaTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) DarkAppColors else LightAppColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.primary,
            onPrimary = c.primaryForeground,
            primaryContainer = Color(0xFF1E3A5F),
            onPrimaryContainer = Color(0xFFDBEAFE),
            secondaryContainer = Color(0xFF1E3A5F),
            onSecondaryContainer = Color(0xFFDBEAFE),
            background = c.background,
            onBackground = c.foreground,
            surface = c.background,
            onSurface = c.foreground,
            surfaceVariant = c.muted,
            onSurfaceVariant = c.mutedForeground,
            surfaceContainerLowest = c.background,
            surfaceContainerLow = c.card,
            surfaceContainer = c.card,
            surfaceContainerHigh = Color(0xFF243247),
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
            primaryContainer = Color(0xFFD6EAF8),
            onPrimaryContainer = Color(0xFF1B4F72),
            secondaryContainer = Color(0xFFD6EAF8),
            onSecondaryContainer = Color(0xFF1B4F72),
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
            outline = Color(0xFFCBD5E1),
            outlineVariant = c.border,
            error = c.destructive,
            onError = c.destructiveForeground,
        )
    }
    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
