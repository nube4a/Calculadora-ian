package com.ian.calculadora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Colores del teclado. Marca: azul eléctrico (acción principal) y coral (borrar), como el logo √x.
 * - number: teclas de cifras (en claro, blancas y "elevadas" con sombra suave).
 * - operator: + − × ÷ ( ) %.
 * - function: funciones científicas.
 * - action: AC (coral).
 * - equals: degradado azul (la tecla más importante).
 */
@Immutable
class CalcColors(
    val number: Color, val onNumber: Color,
    val function: Color, val onFunction: Color,
    val operator: Color, val onOperator: Color,
    val action: Color, val onAction: Color,
    val equals: Color, val equalsEnd: Color, val onEquals: Color,
    val display: Color,
    val raisedKeys: Boolean,
) {
    val equalsBrush: Brush get() = Brush.linearGradient(listOf(equals, equalsEnd))
}

private val DarkCalcColors = CalcColors(
    number = Color(0xFF1A2236), onNumber = Color(0xFFF1F4FA),
    function = Color(0xFF141B2C), onFunction = Color(0xFFA9B8D6),
    operator = Color(0xFF1C2C52), onOperator = Color(0xFF94B4FF),
    action = Color(0xFF3A1D24), onAction = Color(0xFFFF8F87),
    equals = Color(0xFF5B8CFF), equalsEnd = Color(0xFF3A66F0), onEquals = Color.White,
    display = Color(0xFF131A2B),
    raisedKeys = false,
)

private val LightCalcColors = CalcColors(
    number = Color(0xFFFFFFFF), onNumber = Color(0xFF151B2B),
    function = Color(0xFFEDF1F8), onFunction = Color(0xFF3B4A66),
    operator = Color(0xFFE6EEFF), onOperator = Color(0xFF2559E6),
    action = Color(0xFFFFE8E6), onAction = Color(0xFFC62828),
    equals = Color(0xFF3D7BFF), equalsEnd = Color(0xFF2456E8), onEquals = Color.White,
    display = Color(0xFFFFFFFF),
    raisedKeys = true,
)

val LocalCalcColors = staticCompositionLocalOf { DarkCalcColors }

/** Azul principal (y para lo seleccionado); el coral se reserva para borrar (AC) y errores. */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FA6FF), onPrimary = Color(0xFF061A44),
    primaryContainer = Color(0xFF1C2C52), onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFAFC0E4), onSecondary = Color(0xFF102A55),
    secondaryContainer = Color(0xFF22304C), onSecondaryContainer = Color(0xFFDCE6FF),
    tertiary = Color(0xFF8FD3C0), onTertiary = Color(0xFF00382D),
    tertiaryContainer = Color(0xFF1D3A33), onTertiaryContainer = Color(0xFFC4F0E3),
    background = Color(0xFF0B1020), onBackground = Color(0xFFF1F4FA),
    surface = Color(0xFF0B1020), onSurface = Color(0xFFF1F4FA),
    surfaceVariant = Color(0xFF161E30), onSurfaceVariant = Color(0xFF9AA6BE),
    surfaceTint = Color(0xFF7FA6FF),
    surfaceContainerLowest = Color(0xFF080C18), surfaceContainerLow = Color(0xFF0F1526),
    surfaceContainer = Color(0xFF131A2B), surfaceContainerHigh = Color(0xFF182033),
    surfaceContainerHighest = Color(0xFF1E273C),
    outline = Color(0xFF5E6982), outlineVariant = Color(0xFF232C42),
    error = Color(0xFFFF8F87), onError = Color(0xFF3D0A0A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6BFF), onPrimary = Color.White,
    primaryContainer = Color(0xFFE6EEFF), onPrimaryContainer = Color(0xFF0A2A6B),
    secondary = Color(0xFF4A5F86), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6EEFF), onSecondaryContainer = Color(0xFF102A55),
    tertiary = Color(0xFF1F7A66), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC4F0E3), onTertiaryContainer = Color(0xFF00382D),
    background = Color(0xFFF4F6FB), onBackground = Color(0xFF151B2B),
    surface = Color(0xFFF4F6FB), onSurface = Color(0xFF151B2B),
    surfaceVariant = Color(0xFFEDF1F8), onSurfaceVariant = Color(0xFF5A657C),
    surfaceTint = Color(0xFF2F6BFF),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF9FAFD),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFF1F4FA),
    surfaceContainerHighest = Color(0xFFE9EDF5),
    outline = Color(0xFF8A93A8), outlineVariant = Color(0xFFE1E6EF),
    error = Color(0xFFC62828), onError = Color.White,
)

@Composable
fun CalculadoraTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalCalcColors provides if (dark) DarkCalcColors else LightCalcColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
