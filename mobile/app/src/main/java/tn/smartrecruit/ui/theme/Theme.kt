package tn.smartrecruit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
val TextMuted = Color(0xFF6B7A70)

private val Colors = lightColorScheme(
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

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp)
)

@Composable
fun SmartRecruitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography, content = content)
}
