package com.hayathatti.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Dark = darkColorScheme(
    primary = Color(0xFFFF6B5E),
    onPrimary = Color(0xFF2B0603),
    primaryContainer = Color(0xFF4A1612),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFF9AA8FF),
    onSecondary = Color(0xFF10163F),
    secondaryContainer = Color(0xFF2A3160),
    onSecondaryContainer = Color(0xFFE0E4FF),
    background = Color(0xFF0F1220),
    onBackground = Color(0xFFEDEFF7),
    surface = Color(0xFF0F1220),
    onSurface = Color(0xFFEDEFF7),
    surfaceVariant = Color(0xFF232840),
    onSurfaceVariant = Color(0xFFB6BCD3),
    surfaceContainerLowest = Color(0xFF0B0D18),
    surfaceContainerLow = Color(0xFF161A2B),
    surfaceContainer = Color(0xFF1A1E31),
    surfaceContainerHigh = Color(0xFF212640),
    surfaceContainerHighest = Color(0xFF2A3050),
    outline = Color(0xFF6A7191),
    outlineVariant = Color(0xFF343A57),
    error = Color(0xFFFFA69E),
    onError = Color(0xFF3B0A06),
)

private val Light = lightColorScheme(
    primary = Color(0xFFC8291C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF410603),
    secondary = Color(0xFF3F4BC0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E4FF),
    onSecondaryContainer = Color(0xFF10163F),
    background = Color(0xFFF5F6FB),
    onBackground = Color(0xFF151827),
    surface = Color(0xFFF5F6FB),
    onSurface = Color(0xFF151827),
    surfaceVariant = Color(0xFFE5E7F2),
    onSurfaceVariant = Color(0xFF4A5068),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEDEEF6),
    surfaceContainerHighest = Color(0xFFE5E7F2),
    outline = Color(0xFF747A94),
    outlineVariant = Color(0xFFD5D8E6),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val Type = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.25).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.5.sp, lineHeight = 16.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Başarı ve uyarı renkleri. Durum her zaman yazıyla da belirtilir, yalnız renge dayanmaz. */
object StatusColors {
    val ok: Color
        @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFF5EE0A8) else Color(0xFF0B7A4B)
    val warn: Color
        @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFFF7C65A) else Color(0xFF8A5A00)
}

/** SOS düğmesindeki kırmızı tonlar temadan bağımsızdır. */
val SosRedLight = Color(0xFFFF6B5E)
val SosRed = Color(0xFFE5261C)
val SosRedDeep = Color(0xFFA5120B)

@Composable
fun HayatHattiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = Type,
        shapes = AppShapes,
        content = content,
    )
}
