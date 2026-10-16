package com.ian.calculadora.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Sistema de diseño de Calculadora IAN.
 *
 * Forma: "squircle" (cuadrado muy redondeado) en lugar de círculos o píldoras, para una identidad propia.
 * Espaciado: escala de 4 dp.
 */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object Radius {
    val sm = 12.dp
    val md = 18.dp
    val lg = 24.dp
    val xl = 32.dp
}

object Shapes {
    /** Teclas: 30 % de redondeo, cuadradas pero suaves. */
    val key = RoundedCornerShape(percent = 30)
    val card = RoundedCornerShape(Radius.lg)
    val field = RoundedCornerShape(Radius.md)
    val chip = RoundedCornerShape(Radius.sm)
    val pill = RoundedCornerShape(percent = 50)
}
