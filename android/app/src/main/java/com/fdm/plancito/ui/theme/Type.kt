package com.fdm.plancito.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fdm.plancito.R

/** Manrope variable (res/font/manrope.ttf, licencia OFL), pesos 400 a 800. */
@OptIn(ExperimentalTextApi::class)
val Manrope = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(R.font.manrope, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

private fun style(size: Int, weight: Int, lineHeight: Int, letterSpacing: TextUnit = 0.sp) = TextStyle(
    fontFamily = Manrope,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing,
)

// Mismos roles que gastos:
//   título de pantalla 22–28/800 → headline · título de sección 15–17/800 → title
//   texto 14–15 → body · etiqueta 13/600 → labelLarge · detalle y chips 11–12 → labelMedium/labelSmall
val PlancitoTypography = Typography(
    displayMedium = style(36, 800, 42, (-0.02).em),
    displaySmall = style(28, 800, 34, (-0.02).em),
    headlineLarge = style(28, 800, 34, (-0.02).em),
    headlineMedium = style(24, 800, 30, (-0.02).em),
    headlineSmall = style(22, 800, 28, (-0.01).em),
    titleLarge = style(17, 800, 22),
    titleMedium = style(15, 800, 20),
    titleSmall = style(14, 700, 20),
    bodyLarge = style(15, 500, 22),
    bodyMedium = style(14, 500, 20),
    bodySmall = style(12, 500, 16),
    labelLarge = style(13, 600, 18),
    labelMedium = style(12, 600, 16),
    labelSmall = style(11, 700, 14),
)
