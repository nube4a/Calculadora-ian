@file:OptIn(ExperimentalTextApi::class)

package com.ian.calculadora.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ian.calculadora.R

private fun manrope(weight: Int) = Font(
    R.font.manrope,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Manrope (variable): geométrica y moderna; es la voz de la marca. */
val Manrope = FontFamily(manrope(300), manrope(400), manrope(500), manrope(600), manrope(700), manrope(800))

/** Cifras de ancho fijo (tabulares) para que los números no "bailen" al cambiar. */
const val TABULAR = "tnum"

private fun style(size: Int, weight: Int, line: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = Manrope,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.em,
)

val AppTypography = Typography(
    displayLarge = style(64, 600, 72, -0.02),
    displayMedium = style(48, 600, 56, -0.02),
    displaySmall = style(36, 500, 44, -0.01),
    headlineLarge = style(30, 700, 38, -0.01),
    headlineMedium = style(26, 700, 34, -0.01),
    headlineSmall = style(22, 700, 30),
    titleLarge = style(20, 700, 28),
    titleMedium = style(17, 600, 24),
    titleSmall = style(14, 700, 20, 0.01),
    bodyLarge = style(16, 500, 24),
    bodyMedium = style(15, 500, 22),
    bodySmall = style(13, 500, 19),
    labelLarge = style(15, 700, 20),
    labelMedium = style(13, 600, 18, 0.02),
    labelSmall = style(11, 700, 16, 0.04),
)
