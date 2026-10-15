package com.ian.calculadora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colores propios del teclado de la calculadora (los mismos de la marca: naranja sobre grafito). */
@Immutable
class CalcColors(
    val number: Color, val onNumber: Color,
    val function: Color, val onFunction: Color,
    val operator: Color, val onOperator: Color,
    val action: Color, val onAction: Color,
    val equals: Color, val onEquals: Color,
    val display: Color,
)

private val DarkCalcColors = CalcColors(
    number = Color(0xFF23272F), onNumber = Color(0xFFF2F4F8),
    function = Color(0xFF181C23), onFunction = Color(0xFFA8C7FA),
    operator = Color(0xFF3A2B12), onOperator = Color(0xFFFFC66D),
    action = Color(0xFFFFDDB3), onAction = Color(0xFF2A1700),
    equals = Color(0xFFFFB74D), onEquals = Color(0xFF2A1700),
    display = Color(0xFF181B21),
)

private val LightCalcColors = CalcColors(
    number = Color(0xFFE9ECF2), onNumber = Color(0xFF1A1D23),
    function = Color(0xFFE3EAF6), onFunction = Color(0xFF2B4F86),
    operator = Color(0xFFFFE2BC), onOperator = Color(0xFF8A4700),
    action = Color(0xFFFFD08A), onAction = Color(0xFF2A1700),
    equals = Color(0xFFE67E00), onEquals = Color(0xFFFFFFFF),
    display = Color(0xFFFFFFFF),
)

val LocalCalcColors = staticCompositionLocalOf { DarkCalcColors }

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB74D), onPrimary = Color(0xFF2A1700),
    primaryContainer = Color(0xFF4A3410), onPrimaryContainer = Color(0xFFFFDDB3),
    secondary = Color(0xFFB8C7E0), onSecondary = Color(0xFF1E2A3A),
    secondaryContainer = Color(0xFF2B3647), onSecondaryContainer = Color(0xFFDCE6F5),
    tertiary = Color(0xFF8FD3C0), onTertiary = Color(0xFF00382D),
    tertiaryContainer = Color(0xFF1D3A33), onTertiaryContainer = Color(0xFFC4F0E3),
    background = Color(0xFF101216), onBackground = Color(0xFFE9EBF0),
    surface = Color(0xFF101216), onSurface = Color(0xFFE9EBF0),
    surfaceVariant = Color(0xFF1B1F26), onSurfaceVariant = Color(0xFFB4BAC6),
    surfaceTint = Color(0xFFFFB74D),
    surfaceContainerLowest = Color(0xFF0C0E11), surfaceContainerLow = Color(0xFF14171C),
    surfaceContainer = Color(0xFF181B21), surfaceContainerHigh = Color(0xFF1D2128),
    surfaceContainerHighest = Color(0xFF232831),
    outline = Color(0xFF6B7280), outlineVariant = Color(0xFF2C313B),
    error = Color(0xFFFF8A80), onError = Color(0xFF3B0A06),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFE67E00), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE2BC), onPrimaryContainer = Color(0xFF2A1700),
    secondary = Color(0xFF4B5D78), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE6F7), onSecondaryContainer = Color(0xFF14233A),
    tertiary = Color(0xFF1F7A66), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC4F0E3), onTertiaryContainer = Color(0xFF00382D),
    background = Color(0xFFF5F6F9), onBackground = Color(0xFF1A1D23),
    surface = Color(0xFFF5F6F9), onSurface = Color(0xFF1A1D23),
    surfaceVariant = Color(0xFFE9ECF2), onSurfaceVariant = Color(0xFF4A505C),
    surfaceTint = Color(0xFFE67E00),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF0F2F6),
    surfaceContainer = Color(0xFFEBEEF3), surfaceContainerHigh = Color(0xFFE5E8EE),
    surfaceContainerHighest = Color(0xFFDFE3EA),
    outline = Color(0xFF7A8190), outlineVariant = Color(0xFFD3D8E1),
    error = Color(0xFFC0392B), onError = Color.White,
)

@Composable
fun CalculadoraTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalCalcColors provides if (dark) DarkCalcColors else LightCalcColors) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
    }
}
