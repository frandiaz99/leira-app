package com.fdm.plancito.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokens de color. El acento es el rosa de la PWA; "visited" (verde) marca los
 * lugares a los que ya fueron. Los *Text pasan contraste como texto, los rellenos no siempre.
 */
@Immutable
data class PlancitoColors(
    val bg: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val border: Color,
    val text: Color,
    val textMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryTint: Color,
    val primaryText: Color,
    val visited: Color,
    val visitedTint: Color,
    val visitedText: Color,
    val star: Color,
    val danger: Color,
    val isDark: Boolean,
)

val LightTokens = PlancitoColors(
    bg = Color(0xFFFFF7F5),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF5EEEC),
    border = Color(0xFFE9DFDC),
    text = Color(0xFF1C1917),
    textMuted = Color(0xFF6B625E),
    primary = Color(0xFFE11D48),
    onPrimary = Color(0xFFFFFFFF),
    primaryTint = Color(0xFFFFE4E8),
    primaryText = Color(0xFFBE123C),
    visited = Color(0xFF059669),
    visitedTint = Color(0xFFD1FAE5),
    visitedText = Color(0xFF065F46),
    star = Color(0xFFF59E0B),
    danger = Color(0xFFDC2626),
    isDark = false,
)

val DarkTokens = PlancitoColors(
    bg = Color(0xFF16110F),
    surface = Color(0xFF211A18),
    surfaceMuted = Color(0xFF2E2523),
    border = Color(0xFF3A302D),
    text = Color(0xFFF5EFED),
    textMuted = Color(0xFFA89F9B),
    primary = Color(0xFFFB7185),
    onPrimary = Color(0xFF2A0A12),
    primaryTint = Color(0xFF3B1820),
    primaryText = Color(0xFFFDA4AF),
    visited = Color(0xFF34D399),
    visitedTint = Color(0xFF0F2E24),
    visitedText = Color(0xFF6EE7B7),
    star = Color(0xFFFBBF24),
    danger = Color(0xFFF87171),
    isDark = true,
)

val LocalPlancitoColors = staticCompositionLocalOf { LightTokens }

/** Acceso a los tokens: `Plancito.colors.primary`. */
object Plancito {
    val colors: PlancitoColors
        @Composable @ReadOnlyComposable get() = LocalPlancitoColors.current
}
