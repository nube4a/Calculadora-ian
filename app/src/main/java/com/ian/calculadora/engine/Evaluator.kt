package com.ian.calculadora.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atanh
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

enum class AngleMode { DEG, RAD }

class CalcException(message: String) : Exception(message)

/**
 * Evaluador de expresiones científicas.
 *
 * Soporta: + − × ÷ ^ mod, paréntesis (se cierran solos al evaluar), multiplicación implícita
 * (2π, 3(4), 2sin(30)), notación E (1.5E3), postfijos ! y %, constantes π, e, Ans, Rand,
 * funciones trigonométricas (directas, inversas e hiperbólicas), logaritmos, raíces, nCr, nPr...
 */
object Evaluator {

    fun evaluate(
        input: String,
        angleMode: AngleMode = AngleMode.DEG,
        ans: Double = 0.0,
        random: () -> Double = { Math.random() },
    ): Double {
        val tokens = Tokenizer(input).tokenize()
        if (tokens.isEmpty()) throw CalcException("Expresión vacía")
        val value = Parser(autoClose(tokens), angleMode, ans, random).parse()
        if (value.isNaN()) throw CalcException("Error matemático")
        if (value.isInfinite()) throw CalcException("Desbordamiento")
        return value
    }

    /** Cuántos paréntesis quedan abiertos en la expresión (para mostrarlos en gris). */
    fun openParens(input: String): Int {
        var depth = 0
        for (c in input) {
            if (c == '(') depth++ else if (c == ')' && depth > 0) depth--
        }
        return depth
    }

    private fun autoClose(tokens: List<Token>): List<Token> {
        var depth = 0
        for (t in tokens) {
            if (t is Token.LParen) depth++
            if (t is Token.RParen) {
                depth--
                if (depth < 0) throw CalcException("Paréntesis sin abrir")
            }
        }
        return tokens + List(depth) { Token.RParen }
    }
}

internal sealed class Token {
    data class Num(val value: Double) : Token()
    data class Op(val op: String) : Token()
    data class Func(val name: String) : Token()
    data class Const(val name: String) : Token()
    data class Postfix(val op: Char) : Token()
    data object LParen : Token()
    data object RParen : Token()
    data object Comma : Token()
}

/** Nombres que se muestran en pantalla tal cual los insertan los botones. */
object Names {
    const val ASIN = "sin⁻¹"
    const val ACOS = "cos⁻¹"
    const val ATAN = "tan⁻¹"
    const val ASINH = "sinh⁻¹"
    const val ACOSH = "cosh⁻¹"
    const val ATANH = "tanh⁻¹"
    const val LOG2 = "log₂"
    const val SQRT = "√"
    const val CBRT = "∛"
    const val ROOT = "root"
    const val ABS = "abs"
    const val NCR = "nCr"
    const val NPR = "nPr"
    const val MOD = "mod"
    const val ANS = "Ans"
    const val RAND = "Rand"

    val functions = listOf(
        "sin", "cos", "tan", ASIN, ACOS, ATAN,
        "sinh", "cosh", "tanh", ASINH, ACOSH, ATANH,
        "ln", "log", LOG2, SQRT, CBRT, ROOT, ABS, NCR, NPR,
    )
    val constants = listOf("π", "e", ANS, RAND)

    /** Todas las palabras reconocidas, de la más larga a la más corta (para backspace y tokenizado). */
    val words: List<String> = (functions + constants + MOD).sortedByDescending { it.length }
}

internal class Tokenizer(private val src: String) {
    private var i = 0

    fun tokenize(): List<Token> {
        val out = mutableListOf<Token>()
        while (i < src.length) {
            val c = src[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> out += Token.Num(readNumber())
                c == '+' -> { out += Token.Op("+"); i++ }
                c == '-' || c == '−' -> { out += Token.Op("-"); i++ }
                c == '*' || c == '×' -> { out += Token.Op("*"); i++ }
                c == '/' || c == '÷' -> { out += Token.Op("/"); i++ }
                c == '^' -> { out += Token.Op("^"); i++ }
                c == '!' || c == '%' -> { out += Token.Postfix(c); i++ }
                c == '(' -> { out += Token.LParen; i++ }
                c == ')' -> { out += Token.RParen; i++ }
                c == ',' || c == ';' -> { out += Token.Comma; i++ }
                else -> out += readWord()
            }
        }
        return out
    }

    private fun readNumber(): Double {
        val start = i
        var dots = 0
        while (i < src.length && (src[i].isDigit() || src[i] == '.')) {
            if (src[i] == '.') dots++
            i++
        }
        if (dots > 1) throw CalcException("Número inválido")
        val mantissa = src.substring(start, i)
        if (mantissa == ".") throw CalcException("Número inválido")
        var text = mantissa
        // Notación científica: 1.5E3, 2E−4
        if (i < src.length && src[i] == 'E') {
            var j = i + 1
            val sb = StringBuilder("E")
            if (j < src.length && (src[j] == '-' || src[j] == '−' || src[j] == '+')) {
                sb.append(if (src[j] == '+') '+' else '-')
                j++
            }
            val digitsStart = j
            while (j < src.length && src[j].isDigit()) j++
            if (j == digitsStart) throw CalcException("Exponente incompleto")
            sb.append(src, digitsStart, j)
            text += sb
            i = j
        }
        return text.toDoubleOrNull() ?: throw CalcException("Número inválido")
    }

    private fun readWord(): Token {
        for (w in Names.words) {
            if (src.startsWith(w, i)) {
                i += w.length
                return when (w) {
                    in Names.functions -> Token.Func(w)
                    Names.MOD -> Token.Op("mod")
                    else -> Token.Const(w)
                }
            }
        }
        throw CalcException("Símbolo desconocido '${src[i]}'")
    }
}

internal class Parser(
    private val tokens: List<Token>,
    private val angle: AngleMode,
    private val ans: Double,
    private val random: () -> Double,
) {
    private var pos = 0

    /** Valor con una marca de "es porcentaje" para que 200 + 10% = 220. */
    private data class V(val v: Double, val pct: Boolean = false)

    fun parse(): Double {
        val result = expression().v
        if (pos < tokens.size) throw CalcException("Error de sintaxis")
        return result
    }

    private fun peek(): Token? = tokens.getOrNull(pos)

    private fun matchOp(op: String): Boolean {
        val t = peek()
        if (t is Token.Op && t.op == op) { pos++; return true }
        return false
    }

    private fun expression(): V {
        var left = term()
        while (true) {
            val plus = matchOp("+")
            if (!plus && !matchOp("-")) return left
            val right = term()
            val r = if (right.pct) left.v * right.v else right.v
            left = V(if (plus) left.v + r else left.v - r)
        }
    }

    private fun term(): V {
        var left = unary()
        while (true) {
            left = when {
                matchOp("*") -> V(left.v * unary().v)
                matchOp("/") -> {
                    val r = unary().v
                    if (r == 0.0) throw CalcException("División por cero")
                    V(left.v / r)
                }
                matchOp("mod") -> {
                    val r = unary().v
                    if (r == 0.0) throw CalcException("División por cero")
                    V(left.v.mod(r))
                }
                startsOperand(peek()) -> V(left.v * unary().v) // multiplicación implícita
                else -> return left
            }
        }
    }

    private fun startsOperand(t: Token?) =
        t is Token.Num || t is Token.Const || t is Token.Func || t is Token.LParen

    private fun unary(): V = when {
        matchOp("-") -> unary().let { V(-it.v, it.pct) }
        matchOp("+") -> unary()
        else -> power()
    }

    private fun power(): V {
        val base = postfix()
        if (!matchOp("^")) return base
        return V(pow(base.v, unary().v))
    }

    private fun postfix(): V {
        var value = primary()
        while (true) {
            val t = peek()
            if (t !is Token.Postfix) return value
            pos++
            value = if (t.op == '!') V(factorial(value.v)) else V(value.v / 100.0, pct = true)
        }
    }

    private fun primary(): V {
        val t = peek() ?: throw CalcException("Expresión incompleta")
        pos++
        return when (t) {
            is Token.Num -> V(t.value)
            is Token.Const -> V(
                when (t.name) {
                    "π" -> PI
                    "e" -> Math.E
                    Names.ANS -> ans
                    else -> random()
                }
            )
            is Token.LParen -> {
                if (peek() is Token.RParen) throw CalcException("Paréntesis vacíos")
                val v = expression()
                expect(Token.RParen)
                V(v.v)
            }
            is Token.Func -> V(call(t.name, arguments()))
            else -> throw CalcException("Error de sintaxis")
        }
    }

    private fun expect(token: Token) {
        if (peek() != token) throw CalcException("Error de sintaxis")
        pos++
    }

    private fun arguments(): List<Double> {
        if (peek() != Token.LParen) return listOf(unary().v) // sin30, √9
        pos++
        val args = mutableListOf(expression().v)
        while (peek() == Token.Comma) {
            pos++
            args += expression().v
        }
        expect(Token.RParen)
        return args
    }

    private fun toRad(x: Double) = if (angle == AngleMode.DEG) Math.toRadians(x) else x
    private fun fromRad(x: Double) = if (angle == AngleMode.DEG) Math.toDegrees(x) else x

    /** Limpia el ruido de punto flotante en trigonometría: sin(180°) = 0, no 1.2E-16. */
    private fun clean(x: Double) = if (abs(x) < 1e-14) 0.0 else x

    private fun call(name: String, args: List<Double>): Double {
        val arity = if (name == Names.ROOT || name == Names.NCR || name == Names.NPR) 2 else 1
        if (args.size != arity) {
            throw CalcException("$name necesita $arity ${if (arity == 1) "argumento" else "argumentos (separa con ,)"}")
        }
        val x = args[0]
        return when (name) {
            "sin" -> clean(sin(toRad(x)))
            "cos" -> clean(cos(toRad(x)))
            "tan" -> {
                val r = toRad(x)
                if (abs(cos(r)) < 1e-14) throw CalcException("tan no definida")
                clean(tan(r))
            }
            Names.ASIN -> { domain(x in -1.0..1.0); fromRad(asin(x)) }
            Names.ACOS -> { domain(x in -1.0..1.0); fromRad(acos(x)) }
            Names.ATAN -> fromRad(atan(x))
            "sinh" -> sinh(x)
            "cosh" -> cosh(x)
            "tanh" -> tanh(x)
            Names.ASINH -> asinh(x)
            Names.ACOSH -> { domain(x >= 1.0); acosh(x) }
            Names.ATANH -> { domain(x > -1.0 && x < 1.0); atanh(x) }
            "ln" -> { domain(x > 0); ln(x) }
            "log" -> { domain(x > 0); log10(x) }
            Names.LOG2 -> { domain(x > 0); log2(x) }
            Names.SQRT -> { domain(x >= 0); sqrt(x) }
            Names.CBRT -> cbrt(x)
            Names.ABS -> abs(x)
            Names.ROOT -> nthRoot(x, args[1])
            Names.NCR -> combinations(x, args[1])
            Names.NPR -> permutations(x, args[1])
            else -> throw CalcException("Función desconocida")
        }
    }

    private fun domain(ok: Boolean) {
        if (!ok) throw CalcException("Fuera del dominio")
    }

    /** root(x, n) = raíz n-ésima de x. Admite bases negativas con índice impar. */
    private fun nthRoot(x: Double, n: Double): Double {
        if (n == 0.0) throw CalcException("Índice de raíz cero")
        if (x < 0) {
            if (isInt(n) && n.toLong() % 2L != 0L) return -abs(x).pow(1.0 / n)
            throw CalcException("Fuera del dominio")
        }
        return x.pow(1.0 / n)
    }

    private fun pow(base: Double, e: Double): Double {
        if (base == 0.0 && e < 0) throw CalcException("División por cero")
        if (base < 0 && !isInt(e)) {
            // (−8)^(1/3) = −2: si 1/e es un entero impar, es una raíz impar.
            val inv = 1.0 / e
            if (isInt(inv) && round(inv).toLong() % 2L != 0L) return -abs(base).pow(e)
            throw CalcException("Fuera del dominio")
        }
        return base.pow(e)
    }
}

private fun isInt(x: Double) = x.isFinite() && abs(x - round(x)) < 1e-9

private fun requireNonNegInt(x: Double, what: String): Long {
    if (!isInt(x) || x < 0) throw CalcException("$what debe ser entero ≥ 0")
    return round(x).toLong()
}

internal fun factorial(x: Double): Double {
    if (isInt(x)) {
        val n = round(x).toLong()
        if (n < 0) throw CalcException("Factorial de negativo")
        if (n > 170) throw CalcException("Desbordamiento")
        var r = 1.0
        for (k in 2..n) r *= k
        return r
    }
    if (x > 170) throw CalcException("Desbordamiento")
    return gamma(x + 1)
}

/** Función gamma (aproximación de Lanczos) para factoriales no enteros: 0.5! = √π/2. */
internal fun gamma(x: Double): Double {
    if (x < 0.5) {
        if (isInt(x)) throw CalcException("Fuera del dominio")
        return PI / (sin(PI * x) * gamma(1 - x))
    }
    val g = 7.0
    val c = doubleArrayOf(
        0.99999999999980993, 676.5203681218851, -1259.1392167224028,
        771.32342877765313, -176.61502916214059, 12.507343278686905,
        -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7,
    )
    val y = x - 1
    var a = c[0]
    val t = y + g + 0.5
    for (k in 1 until 9) a += c[k] / (y + k)
    return sqrt(2 * PI) * t.pow(y + 0.5) * exp(-t) * a
}

internal fun combinations(nD: Double, rD: Double): Double {
    val n = requireNonNegInt(nD, "n")
    val r = requireNonNegInt(rD, "r")
    if (r > n) throw CalcException("r no puede ser mayor que n")
    val k = minOf(r, n - r)
    var result = 1.0
    for (i in 1..k) result = result * (n - k + i) / i
    return floor(result + 0.5)
}

internal fun permutations(nD: Double, rD: Double): Double {
    val n = requireNonNegInt(nD, "n")
    val r = requireNonNegInt(rD, "r")
    if (r > n) throw CalcException("r no puede ser mayor que n")
    var result = 1.0
    for (i in (n - r + 1)..n) result *= i
    return result
}
