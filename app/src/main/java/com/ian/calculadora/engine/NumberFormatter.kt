package com.ian.calculadora.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs

object NumberFormatter {

    private const val SIGNIFICANT = 12

    /**
     * Texto "crudo" del número, que el evaluador puede volver a leer (sin separadores de miles).
     * Redondea a 12 cifras significativas, así 0.1 + 0.2 = 0.3.
     */
    fun format(value: Double): String {
        if (value == 0.0) return "0"
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return if (value > 0) "∞" else "−∞"
        val bd = BigDecimal(value).round(MathContext(SIGNIFICANT, RoundingMode.HALF_EVEN))
        val a = abs(value)
        val text = if (a >= 1e15 || a < 1e-9) {
            val exponent = bd.precision() - bd.scale() - 1
            val mantissa = bd.movePointLeft(exponent).stripTrailingZeros().toPlainString()
            "${mantissa}E$exponent"
        } else {
            bd.stripTrailingZeros().toPlainString()
        }
        return text.replace('-', '−')
    }

    /** Igual que [format] pero con separadores de miles para mostrar: 1 234 567.89 → "1,234,567.89". */
    fun formatGrouped(value: Double): String = group(format(value))

    fun group(raw: String): String {
        if (raw.contains('E')) return raw
        val negative = raw.startsWith('−')
        val body = raw.removePrefix("−")
        val intPart = body.substringBefore('.')
        val decPart = body.substringAfter('.', "")
        val grouped = intPart.reversed().chunked(3).joinToString(",").reversed()
        return (if (negative) "−" else "") + grouped + if (decPart.isNotEmpty()) ".$decPart" else ""
    }
}
