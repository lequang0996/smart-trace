package com.fws.smarttrace.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Navy = Color(0xFF0B1F3A)
val NavyDeep = Color(0xFF07142A)
val Ivory = Color(0xFFF7F3EA)
val Gold = Color(0xFFE2B94B)
val GoldDeep = Color(0xFFB8901F)
val GoldLight = Color(0xFFF3DB94)

@Immutable
class AppColors(
    val gold: Color,
    val goldBrush: Brush,
    val hero: Brush,
    val heroText: Color,
    val success: Color,
    val warn: Color,
    val bad: Color,
    val info: Color,
    val outline: Color,
    val chipBg: Color,
)

private val LightExtras = AppColors(
    gold = GoldDeep,
    goldBrush = Brush.horizontalGradient(listOf(Color(0xFFE9C45A), Color(0xFFF6E3A1), Color(0xFFE2B94B))),
    hero = Brush.linearGradient(listOf(Navy, Color(0xFF1B3A6B))),
    heroText = Ivory,
    success = Color(0xFF1E7B4F),
    warn = Color(0xFFB7791F),
    bad = Color(0xFFB3261E),
    info = Color(0xFF4A5D80),
    outline = Color(0xFFDDD3BC),
    chipBg = Color(0xFFF1EAD8),
)

private val DarkExtras = AppColors(
    gold = Gold,
    goldBrush = Brush.horizontalGradient(listOf(Color(0xFFD9AE3F), Color(0xFFF3DB94), Color(0xFFE2B94B))),
    hero = Brush.linearGradient(listOf(Color(0xFF16305C), Color(0xFF0A1020))),
    heroText = Color(0xFFF3EEDF),
    success = Color(0xFF4CC38A),
    warn = Color(0xFFF0B84A),
    bad = Color(0xFFFF7A70),
    info = Color(0xFF9FB1D6),
    outline = Color(0xFF2B3A5E),
    chipBg = Color(0xFF1B2742),
)

private val LightScheme = lightColorScheme(
    primary = Navy, onPrimary = Ivory,
    primaryContainer = Color(0xFFE9DFC0), onPrimaryContainer = Navy,
    secondary = GoldDeep, onSecondary = Color.White,
    background = Ivory, onBackground = Color(0xFF14213D),
    surface = Color.White, onSurface = Color(0xFF14213D),
    surfaceVariant = Color(0xFFEFE8D8), onSurfaceVariant = Color(0xFF58637A),
    outline = Color(0xFFDDD3BC), error = Color(0xFFB3261E),
)

private val DarkScheme = darkColorScheme(
    primary = Gold, onPrimary = Color(0xFF0A1020),
    primaryContainer = Color(0xFF2A3B63), onPrimaryContainer = Color(0xFFF3EEDF),
    secondary = Gold, onSecondary = Color(0xFF0A1020),
    background = Color(0xFF0A1020), onBackground = Color(0xFFF3EEDF),
    surface = Color(0xFF121B30), onSurface = Color(0xFFF3EEDF),
    surfaceVariant = Color(0xFF1B2742), onSurfaceVariant = Color(0xFFA9B2C7),
    outline = Color(0xFF2B3A5E), error = Color(0xFFFF7A70),
)

private val Serif = FontFamily.Serif

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

private val LocalAppColors = staticCompositionLocalOf { LightExtras }

object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}

@Composable
fun SmartTraceTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalAppColors provides if (dark) DarkExtras else LightExtras) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
