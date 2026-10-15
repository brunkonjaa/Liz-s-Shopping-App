package ie.suric.lizshopping

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WarmCream = Color(0xFFF7F0E5)
private val WarmIvory = Color(0xFFFFFBF5)
private val Oat = Color(0xFFEFE2D1)
private val Olive = Color(0xFF5A6440)
private val Sage = Color(0xFFDCE2CD)
private val Caramel = Color(0xFFB8782D)
private val Terracotta = Color(0xFFB9532D)
private val WarmInk = Color(0xFF2B2A23)
private val WarmMuted = Color(0xFF716B60)

private val LizSans = FontFamily(Font(R.font.roboto_regular))
private val LizSerif = FontFamily(Font(R.font.noto_serif_regular, FontWeight.Normal), Font(R.font.noto_serif_bold, FontWeight.Bold))

private val LightLizColors = lightColorScheme(
    primary = Olive, onPrimary = Color.White, primaryContainer = Sage, onPrimaryContainer = WarmInk,
    secondary = Caramel, onSecondary = Color.White, secondaryContainer = Oat, onSecondaryContainer = WarmInk,
    tertiary = Terracotta, onTertiary = Color.White, background = WarmCream, onBackground = WarmInk,
    surface = WarmIvory, onSurface = WarmInk, surfaceVariant = Oat, onSurfaceVariant = WarmMuted,
    outline = Color(0xFFD7C9B8)
)

private val DarkLizColors = darkColorScheme(
    primary = Color(0xFFB9C68F), onPrimary = Color(0xFF283019), primaryContainer = Color(0xFF46502F), onPrimaryContainer = Color(0xFFE2EAC4),
    secondary = Color(0xFFE1A85C), onSecondary = Color(0xFF3D2608), secondaryContainer = Color(0xFF5A3D18), onSecondaryContainer = Color(0xFFFFDDA8),
    tertiary = Color(0xFFF19A70), onTertiary = Color(0xFF4A1808), background = Color(0xFF211F1A), onBackground = Color(0xFFF4EDE1),
    surface = Color(0xFF2C2922), onSurface = Color(0xFFF4EDE1), surfaceVariant = Color(0xFF464036), onSurfaceVariant = Color(0xFFD5C9B8),
    outline = Color(0xFF756B5C)
)

private val LizTypography = Typography(
    displayLarge = TextStyle(fontFamily = LizSerif, fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 52.sp),
    headlineLarge = TextStyle(fontFamily = LizSerif, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 42.sp),
    headlineMedium = TextStyle(fontFamily = LizSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = LizSans, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = LizSans, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = LizSans, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontFamily = LizSans, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = LizSans, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = LizSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = LizSans, fontSize = 12.sp)
)

private val LizShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
)

@Composable
fun LizTheme(mode: String, content: @Composable () -> Unit) {
    val dark = when (mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    MaterialTheme(colorScheme = if (dark) DarkLizColors else LightLizColors, typography = LizTypography, shapes = LizShapes, content = content)
}
