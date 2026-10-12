package com.ian.calculadora.engine

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

/**
 * Álgebra simbólica: árbol en forma canónica (sumas y productos n-arios) con simplificación,
 * derivación con reglas y salida legible ("2x·sin(x) + x²·cos(x)") que el evaluador puede volver a leer.
 */
sealed class Sym {
    data class N(val v: Double) : Sym() { override fun toString() = Symbolic.print(this) }
    data object X : Sym() { override fun toString() = "x" }
    data class C(val name: String, val v: Double) : Sym() { override fun toString() = name }
    data class Add(val terms: List<Sym>) : Sym() { override fun toString() = Symbolic.print(this) }
    data class Mul(val factors: List<Sym>) : Sym() { override fun toString() = Symbolic.print(this) }
    data class Pow(val b: Sym, val e: Sym) : Sym() { override fun toString() = Symbolic.print(this) }
    data class Fn(val name: String, val a: Sym) : Sym() { override fun toString() = Symbolic.print(this) }
}

data class DerivativeResult(val derivative: Sym, val rules: List<String>)

object Symbolic {

    private val ZERO = Sym.N(0.0)
    private val ONE = Sym.N(1.0)
    private val MINUS_ONE = Sym.N(-1.0)
    private val EC = Sym.C("e", E)

    // ---------- Construcción ----------

    fun parse(text: String): Sym = simplify(fromNode(Evaluator.compile(text).root))

    private fun add(vararg t: Sym): Sym = Sym.Add(t.toList())
    private fun mul(vararg f: Sym): Sym = Sym.Mul(f.toList())
    private fun pow(b: Sym, e: Sym): Sym = Sym.Pow(b, e)
    private fun neg(a: Sym): Sym = mul(MINUS_ONE, a)
    private fun div(a: Sym, b: Sym): Sym = mul(a, pow(b, MINUS_ONE))
    private fun n(v: Double): Sym = Sym.N(v)

    private fun fromNode(node: Node): Sym = when (node) {
        is NumNode -> n(node.v)
        is ConstNode -> when (node.name) {
            "x" -> Sym.X
            "π" -> Sym.C("π", PI)
            "e" -> EC
            else -> throw CalcException("${node.name} no se puede usar en álgebra simbólica")
        }
        is NegNode -> neg(fromNode(node.n))
        is PctNode -> div(fromNode(node.n), n(100.0))
        is FactNode -> throw CalcException("El factorial no se puede derivar simbólicamente")
        is BinNode -> {
            val a = fromNode(node.l)
            val b = fromNode(node.r)
            when (node.op) {
                '+' -> add(a, b)
                '-' -> add(a, neg(b))
                '*' -> mul(a, b)
                '/' -> div(a, b)
                '^' -> pow(a, b)
                else -> throw CalcException("mod no se puede usar en álgebra simbólica")
            }
        }
        is CallNode -> {
            val a = fromNode(node.args[0])
            when (node.name) {
                Names.SQRT -> pow(a, n(0.5))
                Names.CBRT -> pow(a, n(1.0 / 3))
                Names.ROOT -> pow(a, div(ONE, fromNode(node.args[1])))
                "exp" -> pow(EC, a)
                "sin", "cos", "tan", Names.ASIN, Names.ACOS, Names.ATAN,
                "sinh", "cosh", "tanh", Names.ASINH, Names.ACOSH, Names.ATANH,
                "ln", "log", Names.LOG2, Names.ABS -> Sym.Fn(node.name, a)
                else -> throw CalcException("${node.name} no se puede derivar simbólicamente")
            }
        }
    }

    fun hasX(s: Sym): Boolean = when (s) {
        is Sym.X -> true
        is Sym.N, is Sym.C -> false
        is Sym.Add -> s.terms.any { hasX(it) }
        is Sym.Mul -> s.factors.any { hasX(it) }
        is Sym.Pow -> hasX(s.b) || hasX(s.e)
        is Sym.Fn -> hasX(s.a)
    }

    // ---------- Simplificación ----------

    fun simplify(s: Sym): Sym = when (s) {
        is Sym.N, is Sym.X, is Sym.C -> s
        is Sym.Fn -> simplifyFn(s.name, simplify(s.a))
        is Sym.Pow -> simplifyPow(simplify(s.b), simplify(s.e))
        is Sym.Add -> simplifyAdd(s.terms.map { simplify(it) })
        is Sym.Mul -> simplifyMul(s.factors.map { simplify(it) })
    }

    private fun nice(v: Double): Boolean = v.isFinite() && (isInt(v) || StepSolver.toFraction(v) != null)

    private fun snapNum(v: Double): Double {
        val r = round(v)
        return if (abs(v - r) < 1e-12 * maxOf(1.0, abs(v))) r else v
    }

    private fun simplifyFn(name: String, a: Sym): Sym {
        if (a is Sym.N) {
            val v = runCatching { callFunction(name, listOf(a.v), AngleMode.RAD) }.getOrNull()
            if (v != null && isInt(v)) return n(snapNum(v))
        }
        if (name == "ln") {
            if (a == EC) return ONE
            if (a is Sym.Pow && a.b == EC) return a.e
        }
        if (name == Names.ABS && a is Sym.N) return n(abs(a.v))
        return Sym.Fn(name, a)
    }

    private fun simplifyPow(b: Sym, e: Sym): Sym {
        if (e is Sym.N) {
            if (e.v == 0.0) return ONE
            if (e.v == 1.0) return b
        }
        if (b is Sym.N) {
            if (b.v == 1.0) return ONE
            if (b.v == 0.0 && e is Sym.N && e.v > 0) return ZERO
            if (e is Sym.N) {
                val v = b.v.pow(e.v)
                if (nice(v) && (isInt(e.v) || isInt(v))) return n(snapNum(v))
            }
        }
        if (b == EC && e is Sym.Fn && e.name == "ln") return e.a
        // (a^p)^q = a^(p·q) con exponentes numéricos
        if (b is Sym.Pow && b.e is Sym.N && e is Sym.N) return simplifyPow(b.b, n(snapNum(b.e.v * e.v)))
        // (a·b)^n = aⁿ·bⁿ con n entero
        if (b is Sym.Mul && e is Sym.N && isInt(e.v)) return simplifyMul(b.factors.map { simplifyPow(it, e) })
        return Sym.Pow(b, e)
    }

    /** Separa el coeficiente numérico de un término: 3x² → (3, x²). */
    private fun splitCoef(t: Sym): Pair<Double, Sym?> = when {
        t is Sym.N -> t.v to null
        t is Sym.Mul && t.factors.first() is Sym.N -> {
            val rest = t.factors.drop(1)
            (t.factors.first() as Sym.N).v to (if (rest.size == 1) rest[0] else Sym.Mul(rest))
        }
        else -> 1.0 to t
    }

    private fun simplifyAdd(terms0: List<Sym>): Sym {
        val flat = terms0.flatMap { if (it is Sym.Add) it.terms else listOf(it) }
        var constant = 0.0
        val coefs = LinkedHashMap<String, Pair<Double, Sym>>()
        for (t in flat) {
            val (c, rest) = splitCoef(t)
            if (rest == null) { constant += c; continue }
            val key = rest.toString()
            val prev = coefs[key]
            coefs[key] = ((prev?.first ?: 0.0) + c) to rest
        }
        val out = coefs.values.mapNotNull { (c, rest) ->
            val cc = snapNum(c)
            if (cc == 0.0) null else simplifyMul(listOf(n(cc), rest), distribute = false)
        }.sortedWith(compareByDescending<Sym> { degree(it) }.thenBy { it.toString() }).toMutableList()
        constant = snapNum(constant)
        if (constant != 0.0 || out.isEmpty()) out += n(constant)
        return if (out.size == 1) out[0] else Sym.Add(out)
    }

    private fun degree(t: Sym): Double = when (t) {
        is Sym.X -> 1.0
        is Sym.Pow -> if (t.b == Sym.X && t.e is Sym.N) t.e.v else 0.0
        is Sym.Mul -> t.factors.sumOf { degree(it) }
        else -> 0.0
    }

    private fun simplifyMul(factors0: List<Sym>, distribute: Boolean = true): Sym {
        val flat = factors0.flatMap { if (it is Sym.Mul) it.factors else listOf(it) }
        var coef = 1.0
        val bases = LinkedHashMap<String, Pair<Sym, MutableList<Sym>>>()
        for (f in flat) {
            when {
                f is Sym.N -> coef *= f.v
                // Fracciones numéricas sencillas como 3⁻¹ se integran al coeficiente.
                f is Sym.Pow && f.b is Sym.N && f.e is Sym.N && nice(f.b.v.pow(f.e.v)) -> coef *= f.b.v.pow(f.e.v)
                else -> {
                    val (b, e) = if (f is Sym.Pow) f.b to f.e else f to ONE
                    bases.getOrPut(b.toString()) { b to mutableListOf() }.second += e
                }
            }
        }
        coef = snapNum(coef)
        if (coef == 0.0) return ZERO
        val rest = mutableListOf<Sym>()
        for ((b, es) in bases.values) {
            val e = if (es.size == 1) es[0] else simplifyAdd(es)
            when (val p = simplifyPow(b, e)) {
                is Sym.N -> coef *= p.v
                is Sym.Mul -> p.factors.forEach { if (it is Sym.N) coef *= it.v else rest += it }
                else -> rest += p
            }
        }
        coef = snapNum(coef)
        // Distribuye un coeficiente sobre una suma: 2(x + 1) → 2x + 2.
        if (distribute && rest.size == 1 && rest[0] is Sym.Add && coef != 1.0) {
            return simplifyAdd((rest[0] as Sym.Add).terms.map { simplifyMul(listOf(n(coef), it), distribute = false) })
        }
        rest.sortWith(compareBy<Sym> { kindOrder(it) }.thenBy { it.toString() })
        if (rest.isEmpty()) return n(coef)
        if (coef == 1.0 && rest.size == 1) return rest[0]
        return Sym.Mul((if (coef == 1.0) emptyList() else listOf(n(coef))) + rest)
    }

    private fun kindOrder(s: Sym): Int = when {
        s is Sym.C -> 0
        s == Sym.X || (s is Sym.Pow && s.b == Sym.X) -> 1
        s is Sym.Pow && s.b is Sym.C -> 3
        s is Sym.Fn -> 2
        else -> 4
    }

    // ---------- Derivación ----------

    fun derivative(s: Sym): DerivativeResult {
        val rules = LinkedHashSet<String>()
        val d = simplify(diff(s, rules))
        return DerivativeResult(d, rules.toList())
    }

    private fun diff(s: Sym, rules: MutableSet<String>): Sym {
        if (!hasX(s)) {
            if (s !is Sym.N) rules += "constante"
            return ZERO
        }
        return when (s) {
            is Sym.X -> ONE
            is Sym.N, is Sym.C -> ZERO
            is Sym.Add -> { rules += "suma"; Sym.Add(s.terms.map { diff(it, rules) }) }
            is Sym.Mul -> {
                val withX = s.factors.filter { hasX(it) }
                if (withX.size > 1) {
                    val isQuotient = s.factors.any { it is Sym.Pow && it.e is Sym.N && (it.e as Sym.N).v < 0 && hasX(it.b) }
                    rules += if (isQuotient) "cociente" else "producto"
                } else if (s.factors.size > 1) {
                    rules += "constante por función"
                }
                Sym.Add(s.factors.indices.map { i ->
                    Sym.Mul(s.factors.mapIndexed { j, f -> if (i == j) diff(f, rules) else f })
                })
            }
            is Sym.Pow -> {
                val bx = hasX(s.b)
                val ex = hasX(s.e)
                when {
                    bx && !ex -> {
                        rules += "potencia"
                        if (s.b != Sym.X) rules += "cadena"
                        mul(s.e, pow(s.b, add(s.e, MINUS_ONE)), diff(s.b, rules))
                    }
                    !bx && ex -> {
                        rules += "exponencial"
                        if (s.e != Sym.X) rules += "cadena"
                        val lnB = if (s.b == EC) ONE else Sym.Fn("ln", s.b)
                        mul(s, lnB, diff(s.e, rules))
                    }
                    else -> {
                        rules += "derivación logarítmica"
                        mul(s, add(mul(diff(s.e, rules), Sym.Fn("ln", s.b)), mul(s.e, diff(s.b, rules), pow(s.b, MINUS_ONE))))
                    }
                }
            }
            is Sym.Fn -> {
                val u = s.a
                if (u != Sym.X) rules += "cadena"
                rules += "derivada de ${s.name}"
                val du = diff(u, rules)
                val outer: Sym = when (s.name) {
                    "sin" -> Sym.Fn("cos", u)
                    "cos" -> neg(Sym.Fn("sin", u))
                    "tan" -> pow(Sym.Fn("cos", u), n(-2.0))
                    Names.ASIN -> pow(add(ONE, neg(pow(u, n(2.0)))), n(-0.5))
                    Names.ACOS -> neg(pow(add(ONE, neg(pow(u, n(2.0)))), n(-0.5)))
                    Names.ATAN -> pow(add(ONE, pow(u, n(2.0))), MINUS_ONE)
                    "sinh" -> Sym.Fn("cosh", u)
                    "cosh" -> Sym.Fn("sinh", u)
                    "tanh" -> pow(Sym.Fn("cosh", u), n(-2.0))
                    Names.ASINH -> pow(add(pow(u, n(2.0)), ONE), n(-0.5))
                    Names.ACOSH -> pow(add(pow(u, n(2.0)), MINUS_ONE), n(-0.5))
                    Names.ATANH -> pow(add(ONE, neg(pow(u, n(2.0)))), MINUS_ONE)
                    "ln" -> pow(u, MINUS_ONE)
                    "log" -> pow(mul(u, Sym.Fn("ln", n(10.0))), MINUS_ONE)
                    Names.LOG2 -> pow(mul(u, Sym.Fn("ln", n(2.0))), MINUS_ONE)
                    Names.ABS -> mul(u, pow(Sym.Fn(Names.ABS, u), MINUS_ONE))
                    else -> throw CalcException("No sé derivar ${s.name}")
                }
                mul(outer, du)
            }
        }
    }

    /** Recta tangente y = f(a) + f′(a)(x − a), simplificada. */
    fun tangent(f: Sym, a: Double): Sym {
        val fa = evaluate(f, a)
        val dfa = evaluate(derivative(f).derivative, a)
        if (!fa.isFinite() || !dfa.isFinite()) throw CalcException("La función no es derivable en x = ${NumberFormatter.format(a)}")
        return simplify(add(n(fa), mul(n(dfa), add(Sym.X, n(-a)))))
    }

    fun evaluate(s: Sym, x: Double): Double =
        Evaluator.compile(print(s)).at(x, EvalContext(AngleMode.RAD))

    // ---------- Polinomios ----------

    /** Coeficientes [a₀, a₁, …] si la expresión es un polinomio en x de grado ≤ maxDeg. */
    fun polynomialCoefficients(text: String, maxDeg: Int = 8): List<Double>? {
        val f = runCatching { Evaluator.compile(text) }.getOrNull() ?: return null
        val ctx = EvalContext(AngleMode.RAD)
        val xs = (0..maxDeg).map { it - maxDeg / 2.0 }
        val ys = xs.map { f.at(it, ctx) }
        if (ys.any { !it.isFinite() }) return null
        val c = runCatching { solveLinear(xs.map { x -> (0..maxDeg).map { x.pow(it) } }, ys) }.getOrNull() ?: return null
        for (x in listOf(0.37, -3.1, 5.6, -7.9)) {
            val p = c.indices.sumOf { c[it] * x.pow(it) }
            val y = f.at(x, ctx)
            if (!y.isFinite() || abs(p - y) > 1e-7 * maxOf(1.0, abs(y))) return null
        }
        val scale = c.maxOf { abs(it) }.coerceAtLeast(1e-300)
        val cleaned = c.map { v ->
            if (abs(v) < 1e-9 * scale) 0.0 else {
                val r = round(v)
                if (abs(v - r) < 1e-8 * maxOf(1.0, abs(v))) r else roundSig(v, 10)
            }
        }
        val deg = cleaned.indexOfLast { it != 0.0 }
        return if (deg < 0) listOf(0.0) else cleaned.take(deg + 1)
    }

    fun polynomialSym(ascending: List<Double>): Sym =
        simplify(Sym.Add(ascending.mapIndexed { k, a -> mul(n(a), pow(Sym.X, n(k.toDouble()))) }))

    /** Factorización real: a·(x − r₁)(x − r₂)…(x² + px + q) para raíces complejas conjugadas. */
    fun factorText(ascending: List<Double>): String? {
        if (ascending.size < 3) return null
        var desc = ascending.reversed()
        val lead = desc[0]
        val parts = mutableListOf<String>()
        fun linear(r: Double) { parts += factorPart(simplify(add(Sym.X, n(-r)))) }
        fun quadratic(p: Double, q: Double) {
            parts += factorPart(simplify(add(pow(Sym.X, n(2.0)), mul(n(roundSig(p, 10)), Sym.X), n(roundSig(q, 10)))))
        }
        // 1) Raíces racionales exactas (teorema de la raíz racional), incluidas las repetidas.
        while (desc.size > 1) {
            val r = rationalRoot(desc) ?: break
            linear(r)
            desc = syntheticDivision(desc, r)
        }
        // 2) Lo que queda: cuadrática por fórmula o Durand–Kerner.
        when {
            desc.size == 3 -> {
                val (a, b, c) = desc
                val disc = b * b - 4 * a * c
                val center = -b / (2 * a)
                val rad = disc / (4 * a * a)
                val sq = kotlin.math.sqrt(rad)
                when {
                    disc < 0 -> quadratic(b / a, c / a)
                    // Raíces irracionales exactas: centro ± √rad (por ejemplo 1 ± √3).
                    !nice(sq) && nice(rad) && (isInt(center) || StepSolver.toFraction(center) != null) -> {
                        val root = "√" + (StepSolver.toFraction(rad)?.let { "($it)" } ?: numText(rad))
                        val base = if (center == 0.0) "x" else "x " + (if (center > 0) "− " + numText(center) else "+ " + numText(-center))
                        parts += "($base − $root)"
                        parts += "($base + $root)"
                    }
                    else -> { linear(center - sq); linear(center + sq) }
                }
            }
            desc.size > 3 -> {
                val roots = polynomialRoots(desc).toMutableList()
                while (roots.isNotEmpty()) {
                    val r = roots.removeAt(0)
                    if (abs(r.im) < 1e-6 * maxOf(1.0, abs(r.re))) { linear(r.re); continue }
                    val j = roots.indexOfFirst { abs(it.re - r.re) < 1e-6 && abs(it.im + r.im) < 1e-6 }
                    if (j < 0) return null
                    roots.removeAt(j)
                    quadratic(-2 * r.re, r.re * r.re + r.im * r.im)
                }
            }
            desc.size == 2 -> linear(-desc[1] / desc[0])
        }
        // Agrupa factores repetidos: (x − 1)(x − 1) → (x − 1)²
        val grouped = parts.groupingBy { it }.eachCount().entries.joinToString("") { (f, k) ->
            if (k == 1) f else f + k.toString().map { "⁰¹²³⁴⁵⁶⁷⁸⁹"[it - '0'] }.joinToString("")
        }
        val leadText = when (lead) { 1.0 -> ""; -1.0 -> "−"; else -> numText(lead) }
        return leadText + grouped
    }

    private fun syntheticDivision(desc: List<Double>, r: Double): List<Double> {
        val out = mutableListOf(desc[0])
        for (k in 1 until desc.size - 1) out += desc[k] + out.last() * r
        return out
    }

    /** Busca una raíz racional p/q (p divide a₀, q divide aₙ) si los coeficientes son enteros. */
    private fun rationalRoot(desc: List<Double>): Double? {
        if (desc.last() == 0.0) return 0.0
        if (desc.any { !isInt(it) || abs(it) > 1e7 }) return null
        fun divisors(v: Double): List<Long> {
            val a = abs(round(v).toLong())
            return (1..minOf(a, 10000L)).filter { a % it == 0L }
        }
        val scale = desc.maxOf { abs(it) }
        for (p in divisors(desc.last())) for (q in divisors(desc[0])) for (sign in listOf(1, -1)) {
            val r = sign * p.toDouble() / q
            val value = desc.fold(0.0) { acc, c -> acc * r + c }
            if (abs(value) < 1e-9 * scale * maxOf(1.0, abs(r).pow(desc.size - 1))) return r
        }
        return null
    }

    private fun factorPart(s: Sym) = if (s == Sym.X) "x" else "(${print(s)})"

    // ---------- Impresión ----------

    private val superDigits = "⁰¹²³⁴⁵⁶⁷⁸⁹"

    fun numText(v: Double): String = StepSolver.toFraction(v) ?: NumberFormatter.format(v)

    fun print(s: Sym): String = p(s, 0)

    private fun prec(s: Sym): Int = when (s) {
        is Sym.Add -> 1
        is Sym.Mul -> 2
        is Sym.N -> if (s.v < 0 || StepSolver.toFraction(s.v) != null) 2 else 6
        is Sym.Pow -> if (s.e is Sym.N && (s.e as Sym.N).v < 0) 2 else 4
        else -> 6
    }

    private fun wrap(s: Sym, min: Int) = if (prec(s) < min) "(${p(s, 0)})" else p(s, 0)

    private fun isNegativeTerm(t: Sym) = (t is Sym.N && t.v < 0) || (t is Sym.Mul && t.factors.first().let { it is Sym.N && it.v < 0 })

    private fun negate(t: Sym): Sym = when {
        t is Sym.N -> n(-t.v)
        t is Sym.Mul -> {
            val c = -(t.factors.first() as Sym.N).v
            val rest = t.factors.drop(1)
            if (c == 1.0) (if (rest.size == 1) rest[0] else Sym.Mul(rest)) else Sym.Mul(listOf(n(c)) + rest)
        }
        else -> t
    }

    private fun p(s: Sym, ctx: Int): String = when (s) {
        is Sym.N -> numText(s.v)
        is Sym.X -> "x"
        is Sym.C -> s.name
        is Sym.Fn -> s.name + "(" + p(s.a, 0) + ")"
        is Sym.Add -> buildString {
            s.terms.forEachIndexed { i, t ->
                if (i == 0) append(p(t, 1))
                else if (isNegativeTerm(t)) append(" − ").append(wrap(negate(t), 2))
                else append(" + ").append(wrap(t, 2))
            }
        }
        is Sym.Pow -> powText(s)
        is Sym.Mul -> mulText(s)
    }

    private fun powText(s: Sym.Pow): String {
        val e = s.e
        if (e is Sym.N) {
            if (e.v < 0) return "1/" + wrap(Sym.Pow(s.b, n(-e.v)).let { if (e.v == -1.0) s.b else it }, 3)
            if (e.v == 0.5) return "√" + rootArg(s.b)
            if (abs(e.v - 1.0 / 3) < 1e-12) return "∛" + rootArg(s.b)
            if (isInt(e.v) && e.v in 2.0..99.0 && (s.b is Sym.X || s.b is Sym.C)) {
                return wrap(s.b, 6) + e.v.toInt().toString().map { superDigits[it - '0'] }.joinToString("")
            }
        }
        return wrap(s.b, 6) + "^" + wrap(e, 6)
    }

    private fun rootArg(b: Sym) = if (b is Sym.X || b is Sym.C || (b is Sym.N && b.v >= 0)) p(b, 0) else "(${p(b, 0)})"

    private fun mulText(s: Sym.Mul): String {
        var coef = 1.0
        val num = mutableListOf<Sym>()
        val den = mutableListOf<Sym>()
        for (f in s.factors) {
            when {
                f is Sym.N -> coef *= f.v
                f is Sym.Pow && f.e is Sym.N && (f.e as Sym.N).v < 0 -> {
                    val pe = -(f.e as Sym.N).v
                    den += if (pe == 1.0) f.b else Sym.Pow(f.b, n(pe))
                }
                else -> num += f
            }
        }
        val sign = if (coef < 0) "−" else ""
        val frac = StepSolver.toFraction(abs(coef))
        val (cNum, cDen) = if (frac != null) frac.substringBefore('/') to frac.substringAfter('/') else NumberFormatter.format(abs(coef)) to null
        val numParts = num.map { wrap(it, 3) }
        val numText = buildString {
            if (cNum != "1" || numParts.isEmpty()) append(cNum)
            numParts.forEachIndexed { i, part ->
                val implicit = (i == 0 && isNotEmpty() && (part[0].isLetter() || part[0] == '(' || part[0] == '√' || part[0] == 'π'))
                if (isNotEmpty() && !implicit) append("·")
                append(part)
            }
        }
        if (den.isEmpty() && cDen == null) return sign + numText
        val denParts = (listOfNotNull(cDen) + den.map { wrap(it, 3) })
        val joined = buildString {
            denParts.forEachIndexed { i, part ->
                val implicit = i == 1 && cDen != null && (part[0].isLetter() || part[0] == '(' || part[0] == '√' || part[0] == 'π')
                if (i > 0 && !implicit) append("·")
                append(part)
            }
        }
        val denText = if (denParts.size == 1) joined else "($joined)"
        return sign + numText + "/" + denText
    }
}
