package com.ian.calculadora.engine

enum class Base(val radix: Int, val label: String, val digits: String) {
    HEX(16, "HEX", "0123456789ABCDEF"),
    DEC(10, "DEC", "0123456789"),
    OCT(8, "OCT", "01234567"),
    BIN(2, "BIN", "01"),
}

enum class BitOp(val label: String, val unary: Boolean = false) {
    ADD("+"), SUB("−"), MUL("×"), DIV("÷"), MOD("mod"),
    AND("AND"), OR("OR"), XOR("XOR"), NAND("NAND"), NOR("NOR"),
    SHL("<<"), SHR(">>"), USHR(">>>"), ROL("ROL"), ROR("ROR"),
    NOT("NOT", unary = true), NEG("NEG", unary = true),
}

/** Aritmética entera de 64 bits en complemento a dos (modo programador). */
object Programmer {

    fun parse(text: String, base: Base): Long {
        val t = text.trim().replace(" ", "").replace("_", "").uppercase()
        if (t.isEmpty()) throw CalcException("Ingresa un número")
        val negative = t.startsWith("-") || t.startsWith("−")
        val body = if (negative) t.substring(1) else t
        if (body.isEmpty() || body.any { it !in base.digits }) {
            throw CalcException("Dígito no válido en ${base.label}")
        }
        val value = if (base == Base.DEC) {
            body.toBigInteger()
        } else {
            body.toBigInteger(base.radix)
        }
        // DEC acepta con signo; HEX/OCT/BIN se interpretan como patrón de 64 bits.
        val max = if (base == Base.DEC) java.math.BigInteger.valueOf(Long.MAX_VALUE) else java.math.BigInteger.ONE.shiftLeft(64).subtract(java.math.BigInteger.ONE)
        if (value > max) throw CalcException("No cabe en 64 bits")
        val l = value.toLong()
        return if (negative) -l else l
    }

    fun format(value: Long, base: Base): String = when (base) {
        Base.DEC -> value.toString().replace('-', '−')
        Base.HEX -> group(java.lang.Long.toHexString(value).uppercase(), 4)
        Base.OCT -> group(java.lang.Long.toOctalString(value), 3)
        Base.BIN -> group(java.lang.Long.toBinaryString(value), 4)
    }

    private fun group(s: String, size: Int) = s.reversed().chunked(size).joinToString(" ").reversed()

    fun apply(op: BitOp, a: Long, b: Long): Long = when (op) {
        BitOp.ADD -> a + b
        BitOp.SUB -> a - b
        BitOp.MUL -> a * b
        BitOp.DIV -> { if (b == 0L) throw CalcException("División por cero"); a / b }
        BitOp.MOD -> { if (b == 0L) throw CalcException("División por cero"); a % b }
        BitOp.AND -> a and b
        BitOp.OR -> a or b
        BitOp.XOR -> a xor b
        BitOp.NAND -> (a and b).inv()
        BitOp.NOR -> (a or b).inv()
        BitOp.SHL -> a shl shift(b)
        BitOp.SHR -> a shr shift(b)
        BitOp.USHR -> a ushr shift(b)
        BitOp.ROL -> java.lang.Long.rotateLeft(a, shift(b))
        BitOp.ROR -> java.lang.Long.rotateRight(a, shift(b))
        BitOp.NOT -> a.inv()
        BitOp.NEG -> -a
    }

    private fun shift(b: Long): Int {
        if (b < 0 || b > 63) throw CalcException("El desplazamiento debe estar entre 0 y 63")
        return b.toInt()
    }

    /** Cantidad de bits en 1 (útil en electrónica/redes). */
    fun popCount(v: Long) = java.lang.Long.bitCount(v)
}
