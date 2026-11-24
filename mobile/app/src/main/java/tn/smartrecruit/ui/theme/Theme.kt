package tn.smartrecruit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palette taken from the Figma screens
val GreenDark = Color(0xFF1B4D2E)
val GreenMid = Color(0xFF1F7A3E)
val GreenLight = Color(0xFFE6F1E7)
val Orange = Color(0xFFB5651D)
val Cream = Color(0xFFFDF8F0)
val TextDark = Color(0xFF1C2B20)
private val LightTextMuted = Color(0xFF6B7A70)

internal val LocalDarkTheme = compositionLocalOf { false }

/** Text colors used by existing screens; they turn white in dark mode. */
val TextMuted: Color
    @Composable get() = if (LocalDarkTheme.current) Color.White else LightTextMuted

val TextPrimary: Color
    @Composable get() = if (LocalDarkTheme.current) Color.White else MaterialTheme.colorScheme.primary

val TextSecondary: Color
    @Composable get() = if (LocalDarkTheme.current) Color.White else MaterialTheme.colorScheme.secondary

/** The CV card keeps its light-green look in light mode only. */
val CvCardBackground: Color
    @Composable get() = if (LocalDarkTheme.current) MaterialTheme.colorScheme.surface else GreenLight

private val LightColors = lightColorScheme(
    primary = GreenDark,
    onPrimary = Color.White,
    secondary = Orange,
    background = Cream,
    surface = Color.White,
    onBackground = TextDark,
    onSurface = TextDark,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark
)

/** Same brand palette, inverted for comfortable low-light reading. */
private val DarkColors = darkColorScheme(
    primary = GreenMid,
    onPrimary = Color.White,
    secondary = Orange,
    onSecondary = Color.White,
    background = TextDark,
    onBackground = Color.White,
    surface = GreenDark,
    onSurface = Color.White,
    primaryContainer = GreenDark,
    onPrimaryContainer = Color.White,
    secondaryContainer = GreenDark,
    onSecondaryContainer = Color.White,
    surfaceVariant = GreenDark,
    onSurfaceVariant = Color.White,
    outline = LightTextMuted
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp)
)

@Composable
fun SmartRecruitTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content
        )
    }
}
