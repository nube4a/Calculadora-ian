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
import kotlin.math.ceil
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

/** Estado con el que se evalúa una expresión. `vars` contiene variables como x. */
class EvalContext(
    val angle: AngleMode = AngleMode.DEG,
    val ans: Double = 0.0,
    val random: () -> Double = { Math.random() },
    val vars: MutableMap<String, Double> = HashMap(),
) {
    internal var budget = 0
}

/**
 * Evaluador de expresiones científicas.
 *
 * Soporta: + − × ÷ ^ mod, paréntesis (se cierran solos al evaluar), multiplicación implícita
 * (2π, 3(4), 2sin(30)), notación E (1.5E3), postfijos ! y %, constantes π, e, Ans, Rand, la variable x,
 * funciones trigonométricas (directas, inversas e hiperbólicas), logaritmos, raíces, nCr, nPr,
 * y cálculo: ∫(f,a,b), d/dx(f,a), Σ(f,a,b), Π(f,a,b).
 */
object Evaluator {

    fun evaluate(
        input: String,
        angleMode: AngleMode = AngleMode.DEG,
        ans: Double = 0.0,
        random: () -> Double = { Math.random() },
    ): Double = compile(input).evaluate(EvalContext(angleMode, ans, random))

    /** Analiza la expresión una sola vez; útil para graficar o resolver (se evalúa muchas veces). */
    fun compile(input: String): Expression {
        val tokens = Tokenizer(input).tokenize()
        if (tokens.isEmpty()) throw CalcException("Expresión vacía")
        return Expression(Parser(autoClose(tokens)).parse())
    }

    /** Compila una ecuación "izq = der" como la función izq − der (o la expresión tal cual si no hay "="). */
    fun compileEquation(input: String): Expression {
        val parts = input.split('=')
        if (parts.size > 2) throw CalcException("Solo puede haber un signo =")
        if (parts.size == 1) return compile(input)
        if (parts[0].isBlank() || parts[1].isBlank()) throw CalcException("Falta un lado de la ecuación")
        return Expression(BinNode('-', compile(parts[0]).root, compile(parts[1]).root))
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

class Expression internal constructor(internal val root: Node) {

    fun evaluate(ctx: EvalContext): Double {
        ctx.budget = MAX_EVALS
        val v = root.eval(ctx)
        if (v.isNaN()) throw CalcException("Error matemático")
        if (v.isInfinite()) throw CalcException("Desbordamiento")
        return v
    }

    /** f(x) para gráficas y métodos numéricos: devuelve NaN en vez de lanzar errores. */
    fun at(x: Double, ctx: EvalContext): Double {
        ctx.vars["x"] = x
        ctx.budget = MAX_EVALS
        return try {
            root.eval(ctx)
        } catch (e: CalcException) {
            Double.NaN
        }
    }

    companion object {
        internal const val MAX_EVALS = 2_000_000
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
    const val INTEGRAL = "∫"
    const val DERIV = "d/dx"
    const val SUM = "Σ"
    const val PROD = "Π"

    /** Funciones y cuántos argumentos aceptan. */
    internal val arity: Map<String, IntRange> = buildMap {
        listOf(
            "sin", "cos", "tan", ASIN, ACOS, ATAN, "sinh", "cosh", "tanh", ASINH, ACOSH, ATANH,
            "ln", "log", LOG2, SQRT, CBRT, ABS, "exp", "floor", "ceil", "round",
        ).forEach { put(it, 1..1) }
        listOf(ROOT, NCR, NPR, "gcd", "lcm", DERIV).forEach { put(it, 2..2) }
        listOf(INTEGRAL, SUM, PROD).forEach { put(it, 3..3) }
        put("max", 1..64)
        put("min", 1..64)
    }

    /** Funciones cuyo primer argumento es una expresión en x (no se evalúa directamente). */
    internal val lazy = setOf(INTEGRAL, DERIV, SUM, PROD)

    val constants = listOf("π", "e", ANS, RAND, "x")

    /** Escritura alternativa con el teclado normal: pi, sqrt, asin... */
    internal val aliases = mapOf(
        "pi" to "π", "sqrt" to SQRT, "cbrt" to CBRT, "asin" to ASIN, "acos" to ACOS, "atan" to ATAN,
        "asinh" to ASINH, "acosh" to ACOSH, "atanh" to ATANH, "log2" to LOG2, "ans" to ANS,
        "integral" to INTEGRAL, "deriv" to DERIV, "sum" to SUM, "prod" to PROD,
    )

    /** Todas las palabras reconocidas, de la más larga a la más corta (para backspace y tokenizado). */
    val words: List<String> =
        (arity.keys + constants + MOD + aliases.keys).sortedByDescending { it.length }
}

internal class Tokenizer(private val src: String) {
    private var i = 0

    private companion object {
        const val SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹"
    }

    fun tokenize(): List<Token> {
        val out = mutableListOf<Token>()
        while (i < src.length) {
            val c = src[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> out += Token.Num(readNumber())
                c == '+' -> { out += Token.Op("+"); i++ }
                c == '-' || c == '−' -> { out += Token.Op("-"); i++ }
                c == '*' || c == '×' || c == '·' -> { out += Token.Op("*"); i++ }
                c == '/' || c == '÷' -> { out += Token.Op("/"); i++ }
                c == '^' -> { out += Token.Op("^"); i++ }
                c in SUPERSCRIPTS -> {
                    // x², x¹⁰ → x^2, x^10
                    var v = 0.0
                    while (i < src.length && src[i] in SUPERSCRIPTS) {
                        v = v * 10 + SUPERSCRIPTS.indexOf(src[i])
                        i++
                    }
                    out += Token.Op("^")
                    out += Token.Num(v)
                }
                c == '!' || c == '%' -> { out += Token.Postfix(c); i++ }
                c == '(' || c == '[' -> { out += Token.LParen; i++ }
                c == ')' || c == ']' -> { out += Token.RParen; i++ }
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
                val name = Names.aliases[w] ?: w
                return when (name) {
                    in Names.arity -> Token.Func(name)
                    Names.MOD -> Token.Op("mod")
                    else -> Token.Const(name)
                }
            }
        }
        throw CalcException("Símbolo desconocido '${src[i]}'")
    }
}

// ---------- Árbol de la expresión ----------

internal sealed interface Node {
    fun eval(c: EvalContext): Double
}

internal class NumNode(val v: Double) : Node {
    override fun eval(c: EvalContext) = v
}

internal class ConstNode(val name: String) : Node {
    override fun eval(c: EvalContext): Double = when (name) {
        "π" -> PI
        "e" -> Math.E
        Names.ANS -> c.ans
        Names.RAND -> c.random()
        else -> c.vars[name] ?: throw CalcException("$name no tiene valor (úsala en ∫, d/dx, Σ o gráficas)")
    }
}

internal class NegNode(val n: Node) : Node {
    override fun eval(c: EvalContext) = -n.eval(c)
}

/** x% — el valor ÷ 100, y además permite que 200 + 10% = 220. */
internal class PctNode(val n: Node) : Node {
    override fun eval(c: EvalContext) = n.eval(c) / 100.0
}

internal class FactNode(val n: Node) : Node {
    override fun eval(c: EvalContext) = factorial(n.eval(c))
}

private fun isPct(n: Node): Boolean = n is PctNode || (n is NegNode && isPct(n.n))

internal class BinNode(val op: Char, val l: Node, val r: Node) : Node {
    override fun eval(c: EvalContext): Double {
        val a = l.eval(c)
        val b = r.eval(c)
        return when (op) {
            '+' -> if (isPct(r)) a + a * b else a + b
            '-' -> if (isPct(r)) a - a * b else a - b
            '*' -> a * b
            '/' -> {
                if (b == 0.0) throw CalcException("División por cero")
                a / b
            }
            'm' -> {
                if (b == 0.0) throw CalcException("División por cero")
                a.mod(b)
            }
            '^' -> pow(a, b)
            else -> throw CalcException("Operador desconocido")
        }
    }
}

internal class CallNode(val name: String, val args: List<Node>) : Node {
    override fun eval(c: EvalContext): Double {
        if (name in Names.lazy) return evalCalculus(c)
        val v = args.map { it.eval(c) }
        return callFunction(name, v, c.angle)
    }

    private fun evalCalculus(c: EvalContext): Double {
        val f = args[0]
        val saved = c.vars["x"]
        val fx = { x: Double ->
            if (--c.budget < 0) throw CalcException("Cálculo demasiado largo")
            c.vars["x"] = x
            f.eval(c)
        }
        try {
            // Los límites se evalúan con la x exterior (si existe), no con la del integrando.
            fun bound(i: Int): Double {
                if (saved != null) c.vars["x"] = saved else c.vars.remove("x")
                return args[i].eval(c)
            }
            return when (name) {
                Names.INTEGRAL -> integrate(fx, bound(1), bound(2))
                Names.DERIV -> derivative(fx, bound(1))
                else -> {
                    val a = bound(1)
                    val b = bound(2)
                    if (!isInt(a) || !isInt(b)) throw CalcException("Los límites de $name deben ser enteros")
                    val from = round(a).toLong()
                    val to = round(b).toLong()
                    if (to - from > 1_000_000) throw CalcException("Demasiados términos")
                    var acc = if (name == Names.SUM) 0.0 else 1.0
                    var k = from
                    while (k <= to) {
                        val term = fx(k.toDouble())
                        acc = if (name == Names.SUM) acc + term else acc * term
                        k++
                    }
                    acc
                }
            }
        } finally {
            if (saved != null) c.vars["x"] = saved else c.vars.remove("x")
        }
    }
}

// ---------- Analizador sintáctico ----------

internal class Parser(private val tokens: List<Token>) {
    private var pos = 0

    fun parse(): Node {
        val result = expression()
        if (pos < tokens.size) throw CalcException("Error de sintaxis")
        return result
    }

    private fun peek(): Token? = tokens.getOrNull(pos)

    private fun matchOp(op: String): Boolean {
        val t = peek()
        if (t is Token.Op && t.op == op) { pos++; return true }
        return false
    }

    private fun expression(): Node {
        var left = term()
        while (true) {
            left = when {
                matchOp("+") -> BinNode('+', left, term())
                matchOp("-") -> BinNode('-', left, term())
                else -> return left
            }
        }
    }

    private fun term(): Node {
        var left = unary()
        while (true) {
            left = when {
                matchOp("*") -> BinNode('*', left, unary())
                matchOp("/") -> BinNode('/', left, unary())
                matchOp("mod") -> BinNode('m', left, unary())
                startsOperand(peek()) -> BinNode('*', left, unary()) // multiplicación implícita
                else -> return left
            }
        }
    }

    private fun startsOperand(t: Token?) =
        t is Token.Num || t is Token.Const || t is Token.Func || t is Token.LParen

    private fun unary(): Node = when {
        matchOp("-") -> NegNode(unary())
        matchOp("+") -> unary()
        else -> power()
    }

    private fun power(): Node {
        val base = postfix()
        if (!matchOp("^")) return base
        return BinNode('^', base, unary())
    }

    private fun postfix(): Node {
        var value = primary()
        while (true) {
            val t = peek()
            if (t !is Token.Postfix) return value
            pos++
            value = if (t.op == '!') FactNode(value) else PctNode(value)
        }
    }

    private fun primary(): Node {
        val t = peek() ?: throw CalcException("Expresión incompleta")
        pos++
        return when (t) {
            is Token.Num -> NumNode(t.value)
            is Token.Const -> ConstNode(t.name)
            is Token.LParen -> {
                if (peek() is Token.RParen) throw CalcException("Paréntesis vacíos")
                val v = expression()
                expect(Token.RParen)
                // Un porcentaje entre paréntesis ya no es "porcentaje de" en sumas.
                if (isPct(v)) BinNode('*', v, NumNode(1.0)) else v
            }
            is Token.Func -> {
                val args = arguments()
                val range = Names.arity.getValue(t.name)
                if (args.size !in range) {
                    val n = range.first
                    throw CalcException(
                        "${t.name} necesita $n ${if (n == 1) "argumento" else "argumentos separados por coma"}"
                    )
                }
                CallNode(t.name, args)
            }
            else -> throw CalcException("Error de sintaxis")
        }
    }

    private fun expect(token: Token) {
        if (peek() != token) throw CalcException("Error de sintaxis")
        pos++
    }

    private fun arguments(): List<Node> {
        if (peek() != Token.LParen) return listOf(unary()) // sin30, √9
        pos++
        val args = mutableListOf(expression())
        while (peek() == Token.Comma) {
            pos++
            args += expression()
        }
        expect(Token.RParen)
        return args
    }
}

// ---------- Funciones ----------

/** Limpia el ruido de punto flotante en trigonometría: sin(180°) = 0, no 1.2E-16. */
private fun clean(x: Double) = if (abs(x) < 1e-14) 0.0 else x

private fun domain(ok: Boolean) {
    if (!ok) throw CalcException("Fuera del dominio")
}

internal fun callFunction(name: String, args: List<Double>, angle: AngleMode): Double {
    fun toRad(x: Double) = if (angle == AngleMode.DEG) Math.toRadians(x) else x
    fun fromRad(x: Double) = if (angle == AngleMode.DEG) Math.toDegrees(x) else x
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
        "exp" -> exp(x)
        "floor" -> floor(x)
        "ceil" -> ceil(x)
        "round" -> round(x)
        "max" -> args.max()
        "min" -> args.min()
        "gcd" -> gcd(requireInt(x, "gcd"), requireInt(args[1], "gcd")).toDouble()
        "lcm" -> {
            val a = requireInt(x, "lcm")
            val b = requireInt(args[1], "lcm")
            if (a == 0L || b == 0L) 0.0 else abs(a / gcd(a, b) * b).toDouble()
        }
        Names.ROOT -> nthRoot(x, args[1])
        Names.NCR -> combinations(x, args[1])
        Names.NPR -> permutations(x, args[1])
        else -> throw CalcException("Función desconocida")
    }
}

private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) abs(a) else gcd(b, a % b)

private fun requireInt(x: Double, what: String): Long {
    if (!isInt(x) || abs(x) > 9e15) throw CalcException("$what necesita enteros")
    return round(x).toLong()
}

/** root(x, n) = raíz n-ésima de x. Admite bases negativas con índice impar. */
private fun nthRoot(x: Double, n: Double): Double {
    if (n == 0.0) throw CalcException("Índice de raíz cero")
    if (x < 0) {
        if (isInt(n) && round(n).toLong() % 2L != 0L) return -abs(x).pow(1.0 / n)
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

internal fun isInt(x: Double) = x.isFinite() && abs(x - round(x)) < 1e-9

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
