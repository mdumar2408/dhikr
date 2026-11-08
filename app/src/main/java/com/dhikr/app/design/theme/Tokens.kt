package com.dhikr.app.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colour tokens — docs/DESIGN_SPEC.md §2. `ember` is reserved for completion. */
@Immutable
data class DhikrColors(
    val canvas: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val hairline: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val dotOff: Color,
    val ember: Color,
    val isDark: Boolean,
)

val DarkColors = DhikrColors(
    canvas = Color(0xFF000000),
    surface = Color(0xFF0E0E0E),
    surfaceRaised = Color(0xFF181818),
    hairline = Color(0xFF262626),
    ink = Color(0xFFF2F2F2),
    inkMuted = Color(0xFF8A8A8A),
    inkFaint = Color(0xFF4A4A4A),
    dotOff = Color(0xFF1C1C1C),
    ember = Color(0xFFE8452C),
    isDark = true,
)

val LightColors = DhikrColors(
    canvas = Color(0xFFF3F1EC),
    surface = Color(0xFFE9E6DF),
    surfaceRaised = Color(0xFFDEDAD1),
    hairline = Color(0xFFCFCAC0),
    ink = Color(0xFF111111),
    inkMuted = Color(0xFF5E5B55),
    inkFaint = Color(0xFF9C978D),
    dotOff = Color(0xFFDCD7CD),
    ember = Color(0xFFC9341D),
    isDark = false,
)

/** Spacing tokens on a 4 dp grid — §4. */
@Immutable
data class DhikrSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val s: Dp = 12.dp,
    val m: Dp = 16.dp,
    val l: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
    val xxxl: Dp = 64.dp,
    val gutter: Dp = 24.dp,
    val touchTarget: Dp = 48.dp,
    val padRadius: Dp = 28.dp,
)

/** Typography tokens — §3. Dot-matrix text is drawn, not set in a font; see design/dotmatrix. */
@Immutable
data class DhikrType(
    val arabic: TextStyle = TextStyle(fontSize = 32.sp, lineHeight = 52.sp),
    val arabicSmall: TextStyle = TextStyle(fontSize = 20.sp, lineHeight = 32.sp),
    val title: TextStyle = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    val body: TextStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    val bodySmall: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    val label: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.96.sp, // +8 %
    ),
)

/** Motion tokens — §8. */
object DhikrMotion {
    const val FAST_MS = 90
    const val BASE_MS = 180
    const val SLOW_MS = 320
    const val DOT_ON_MS = 80
    const val DOT_OFF_MS = 120
}
