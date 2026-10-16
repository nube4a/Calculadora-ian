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

/** Colores propios del teclado: azul para operaciones y =, rojo suave para AC. */
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
    number = Color(0xFF1C2333), onNumber = Color(0xFFEEF2F8),
    function = Color(0xFF151C2A), onFunction = Color(0xFFA9C2EE),
    operator = Color(0xFF1F3563), onOperator = Color(0xFF9DBEFF),
    action = Color(0xFFFF8A8A), onAction = Color(0xFF3D0A0A),
    equals = Color(0xFF7FAAFF), onEquals = Color(0xFF071A3D),
    display = Color(0xFF151C2A),
)

private val LightCalcColors = CalcColors(
    number = Color(0xFFEDF1F8), onNumber = Color(0xFF1B2330),
    function = Color(0xFFE6EDF9), onFunction = Color(0xFF2E4F86),
    operator = Color(0xFFD9E6FF), onOperator = Color(0xFF1F5BD8),
    action = Color(0xFFFFDADA), onAction = Color(0xFFB71C1C),
    equals = Color(0xFF2F6BFF), onEquals = Color(0xFFFFFFFF),
    display = Color(0xFFFFFFFF),
)

val LocalCalcColors = staticCompositionLocalOf { DarkCalcColors }

/** Azul principal (y para lo seleccionado); el rojo se reserva para borrar (AC) y errores. */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FAAFF), onPrimary = Color(0xFF071A3D),
    primaryContainer = Color(0xFF1F3563), onPrimaryContainer = Color(0xFFD9E6FF),
    secondary = Color(0xFFAFC4EC), onSecondary = Color(0xFF102A55),
    secondaryContainer = Color(0xFF24324D), onSecondaryContainer = Color(0xFFD9E6FF),
    tertiary = Color(0xFF8FD3C0), onTertiary = Color(0xFF00382D),
    tertiaryContainer = Color(0xFF1D3A33), onTertiaryContainer = Color(0xFFC4F0E3),
    background = Color(0xFF0E1320), onBackground = Color(0xFFEEF2F8),
    surface = Color(0xFF0E1320), onSurface = Color(0xFFEEF2F8),
    surfaceVariant = Color(0xFF1A2130), onSurfaceVariant = Color(0xFFB3BDCE),
    surfaceTint = Color(0xFF7FAAFF),
    surfaceContainerLowest = Color(0xFF0A0E18), surfaceContainerLow = Color(0xFF121827),
    surfaceContainer = Color(0xFF161D2C), surfaceContainerHigh = Color(0xFF1B2333),
    surfaceContainerHighest = Color(0xFF212A3C),
    outline = Color(0xFF6B7690), outlineVariant = Color(0xFF283147),
    error = Color(0xFFFF8A8A), onError = Color(0xFF3D0A0A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6BFF), onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E6FF), onPrimaryContainer = Color(0xFF0A2A6B),
    secondary = Color(0xFF4A5F86), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE6F8), onSecondaryContainer = Color(0xFF102A55),
    tertiary = Color(0xFF1F7A66), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC4F0E3), onTertiaryContainer = Color(0xFF00382D),
    background = Color(0xFFF7F9FC), onBackground = Color(0xFF1B2330),
    surface = Color(0xFFF7F9FC), onSurface = Color(0xFF1B2330),
    surfaceVariant = Color(0xFFEAEFF7), onSurfaceVariant = Color(0xFF4A5468),
    surfaceTint = Color(0xFF2F6BFF),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF1F4FA),
    surfaceContainer = Color(0xFFEBF0F8), surfaceContainerHigh = Color(0xFFE5EBF5),
    surfaceContainerHighest = Color(0xFFDFE6F2),
    outline = Color(0xFF7A8499), outlineVariant = Color(0xFFD3DAE6),
    error = Color(0xFFD93636), onError = Color.White,
)

@Composable
fun CalculadoraTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalCalcColors provides if (dark) DarkCalcColors else LightCalcColors) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
    }
}
