package com.ian.calculadora.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs

/** Formato del resultado: normal, científico (1.23E4) o ingeniería (12.3E3, exponente múltiplo de 3). */
enum class DisplayMode(val label: String) { NORMAL("NORM"), SCI("SCI"), ENG("ENG") }

object NumberFormatter {

    private const val SIGNIFICANT = 12

    /**
     * Texto "crudo" del número, que el evaluador puede volver a leer (sin separadores de miles).
     * Redondea a 12 cifras significativas, así 0.1 + 0.2 = 0.3.
     */
    fun format(value: Double, mode: DisplayMode = DisplayMode.NORMAL): String {
        if (value == 0.0) return "0"
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return if (value > 0) "∞" else "−∞"
        val bd = BigDecimal(value).round(MathContext(SIGNIFICANT, RoundingMode.HALF_EVEN))
        val a = abs(value)
        val exponent = bd.precision() - bd.scale() - 1
        val text = when {
            mode == DisplayMode.SCI || (mode == DisplayMode.NORMAL && (a >= 1e15 || a < 1e-9)) -> {
                val mantissa = bd.movePointLeft(exponent).stripTrailingZeros().toPlainString()
                if (exponent == 0) mantissa else "${mantissa}E$exponent"
            }
            mode == DisplayMode.ENG -> {
                val engExp = Math.floorDiv(exponent, 3) * 3
                val mantissa = bd.movePointLeft(engExp).stripTrailingZeros().toPlainString()
                if (engExp == 0) mantissa else "${mantissa}E$engExp"
            }
            else -> bd.stripTrailingZeros().toPlainString()
        }
        return text.replace('-', '−')
    }

    /** Igual que [format] pero con separadores de miles para mostrar: 1234567.89 → "1,234,567.89". */
    fun formatGrouped(value: Double, mode: DisplayMode = DisplayMode.NORMAL): String = group(format(value, mode))

    fun group(raw: String): String {
        if (raw.contains('E')) return raw
        val negative = raw.startsWith('−')
        val body = raw.removePrefix("−")
        val intPart = body.substringBefore('.')
        val decPart = body.substringAfter('.', "")
        val grouped = intPart.reversed().chunked(3).joinToString(",").reversed()
        return (if (negative) "−" else "") + grouped + if (decPart.isNotEmpty()) ".$decPart" else ""
    }

    private val prefixes = mapOf(
        -24 to "y", -21 to "z", -18 to "a", -15 to "f", -12 to "p", -9 to "n", -6 to "µ", -3 to "m",
        3 to "k", 6 to "M", 9 to "G", 12 to "T", 15 to "P", 18 to "E", 21 to "Z", 24 to "Y",
    )

    /** Con prefijo del SI: 0.0047 → "4.7 m", 2200000 → "2.2 M". Null si no aplica. */
    fun siPrefix(value: Double): String? {
        if (value == 0.0 || !value.isFinite()) return null
        val bd = BigDecimal(value).round(MathContext(SIGNIFICANT, RoundingMode.HALF_EVEN))
        val exponent = bd.precision() - bd.scale() - 1
        val engExp = Math.floorDiv(exponent, 3) * 3
        val prefix = prefixes[engExp] ?: return null
        val mantissa = bd.movePointLeft(engExp).stripTrailingZeros().toPlainString().replace('-', '−')
        return "$mantissa $prefix"
    }
}
