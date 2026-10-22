package com.doubleangels.nextdnsmanagement.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.doubleangels.nextdnsmanagement.data.ThemeMode

private val BrandBlue = Color(0xFF007BFF)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF555F71),
    tertiary = Color(0xFF6E5676),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF003063),
    primaryContainer = Color(0xFF00468C),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFBDC7DC),
    tertiary = Color(0xFFDABDE2),
)

/** Colors with a fixed meaning (query status), tuned for light and dark backgrounds. */
@Immutable
data class StatusColors(val allowed: Color, val blocked: Color, val neutral: Color, val warning: Color)

private val LightStatus = StatusColors(
    allowed = Color(0xFF1E8E3E),
    blocked = Color(0xFFD93025),
    neutral = BrandBlue,
    warning = Color(0xFFE37400),
)

private val DarkStatus = StatusColors(
    allowed = Color(0xFF81C995),
    blocked = Color(0xFFF28B82),
    neutral = Color(0xFFA9C7FF),
    warning = Color(0xFFFDD663),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

/** Distinct colors for chart slices and bars. */
val ChartPalette = listOf(
    Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC04), Color(0xFF34A853),
    Color(0xFFA142F4), Color(0xFF24C1E0), Color(0xFFF538A0), Color(0xFFFA7B17),
    Color(0xFF9AA0A6), Color(0xFF5F6368),
)

@Composable
fun NextDnsTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    // minSdk is 32, so Material You dynamic color is always available.
    val colors = when {
        dynamicColor && dark -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalStatusColors provides if (dark) DarkStatus else LightStatus,
    ) {
        MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
    }
}
