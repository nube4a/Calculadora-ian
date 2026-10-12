package com.ian.calculadora.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB74D),
    onPrimary = Color(0xFF3E2500),
    primaryContainer = Color(0xFF5A3A00),
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondaryContainer = Color(0xFF3A3F4B),
    onSecondaryContainer = Color(0xFFDDE2F0),
    tertiaryContainer = Color(0xFF26323A),
    onTertiaryContainer = Color(0xFFBFE9FF),
    background = Color(0xFF121316),
    surface = Color(0xFF121316),
    surfaceVariant = Color(0xFF24262B),
    onSurfaceVariant = Color(0xFFC6C8CF),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFE67E00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB3),
    onPrimaryContainer = Color(0xFF2A1700),
    secondaryContainer = Color(0xFFE1E4EC),
    onSecondaryContainer = Color(0xFF1B1F27),
    tertiaryContainer = Color(0xFFD4ECF8),
    onTertiaryContainer = Color(0xFF0B2633),
    background = Color(0xFFF8F9FB),
    surface = Color(0xFFF8F9FB),
    surfaceVariant = Color(0xFFEEF0F4),
    onSurfaceVariant = Color(0xFF44474E),
)

@Composable
fun CalculadoraTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
