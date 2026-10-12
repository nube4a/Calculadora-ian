package com.ian.calculadora.engine

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// ---------- Cálculo ----------

/** Integral definida por Simpson adaptativo. */
fun integrate(f: (Double) -> Double, a: Double, b: Double): Double {
    if (a == b) return 0.0
    if (!a.isFinite() || !b.isFinite()) throw CalcException("Los límites deben ser finitos")
    val fa = f(a)
    val fb = f(b)
    val m = (a + b) / 2
    val fm = f(m)
    val whole = (b - a) / 6 * (fa + 4 * fm + fb)
    val r = simpson(f, a, b, fa, fm, fb, whole, 1e-11 * max(1.0, abs(whole)), 40)
    if (!r.isFinite()) throw CalcException("La integral no converge")
    return r
}

private fun simpson(
    f: (Double) -> Double, a: Double, b: Double,
    fa: Double, fm: Double, fb: Double, whole: Double, eps: Double, depth: Int,
): Double {
    val m = (a + b) / 2
    val lm = (a + m) / 2
    val rm = (m + b) / 2
    val flm = f(lm)
    val frm = f(rm)
    val left = (m - a) / 6 * (fa + 4 * flm + fm)
    val right = (b - m) / 6 * (fm + 4 * frm + fb)
    val delta = left + right - whole
    if (depth <= 0 || abs(delta) <= 15 * eps) return left + right + delta / 15
    return simpson(f, a, m, fa, flm, fm, left, eps / 2, depth - 1) +
        simpson(f, m, b, fm, frm, fb, right, eps / 2, depth - 1)
}

/** Derivada numérica en un punto (diferencias centradas de 5 puntos + extrapolación de Richardson). */
fun derivative(f: (Double) -> Double, x: Double): Double {
    fun d(h: Double) = (-f(x + 2 * h) + 8 * f(x + h) - 8 * f(x - h) + f(x - 2 * h)) / (12 * h)
    val h = 1e-2 * max(1.0, abs(x))
    val d1 = d(h)
    val d2 = d(h / 2)
    val r = d2 + (d2 - d1) / 15
    if (!r.isFinite()) throw CalcException("La derivada no existe en ese punto")
    return roundSig(r, 10)
}

internal fun roundSig(v: Double, digits: Int): Double {
    if (v == 0.0 || !v.isFinite()) return v
    return java.math.BigDecimal(v).round(java.math.MathContext(digits)).toDouble()
}

// ---------- Raíces de f(x) = 0 ----------

/** Busca todas las raíces reales en [a, b]: rastreo por cambios de signo y refinamiento por bisección. */
fun findRoots(f: (Double) -> Double, a: Double, b: Double, samples: Int = 4000): List<Double> {
    if (!(b > a)) throw CalcException("El intervalo debe cumplir a < b")
    val roots = mutableListOf<Double>()
    val step = (b - a) / samples
    var x0 = a
    var y0 = f(x0)
    for (i in 1..samples) {
        val x1 = a + i * step
        val y1 = f(x1)
        when {
            y0 == 0.0 -> roots += x0
            y0.isFinite() && y1.isFinite() && y0 * y1 < 0 -> {
                val r = bisect(f, x0, x1, y0)
                // Descarta saltos de asíntotas (tan, 1/x): ahí |f| no se acerca a cero.
                if (abs(f(r)) < 1e-6 * max(1.0, max(abs(y0), abs(y1)))) roots += r
            }
            // Raíz doble (tangente al eje): mínimo local de |f| muy cerca de cero.
            i < samples && y1.isFinite() && y0.isFinite() && abs(y1) < abs(y0) -> {
                val y2 = f(x1 + step)
                if (y2.isFinite() && abs(y1) <= abs(y2) && y1 * y0 > 0 && y1 * y2 > 0) {
                    val r = goldenMinAbs(f, x0, x1 + step)
                    if (abs(f(r)) < 1e-9) roots += r
                }
            }
        }
        x0 = x1
        y0 = y1
    }
    if (y0 == 0.0) roots += x0
    return roots.map { roundSig(it, 12) }.distinctBy { roundSig(it, 8) }
}

private fun bisect(f: (Double) -> Double, lo0: Double, hi0: Double, flo0: Double): Double {
    var lo = lo0
    var hi = hi0
    var flo = flo0
    repeat(200) {
        val mid = (lo + hi) / 2
        if (mid == lo || mid == hi) return mid
        val fm = f(mid)
        if (fm == 0.0) return mid
        if (fm * flo < 0) hi = mid else { lo = mid; flo = fm }
    }
    return (lo + hi) / 2
}

private fun goldenMinAbs(f: (Double) -> Double, a0: Double, b0: Double): Double {
    val g = (sqrt(5.0) - 1) / 2
    var a = a0
    var b = b0
    repeat(120) {
        val c = b - g * (b - a)
        val d = a + g * (b - a)
        if (abs(f(c)) < abs(f(d))) b = d else a = c
    }
    return (a + b) / 2
}

// ---------- Números complejos y polinomios ----------

data class Complex(val re: Double, val im: Double = 0.0) {
    operator fun plus(o: Complex) = Complex(re + o.re, im + o.im)
    operator fun minus(o: Complex) = Complex(re - o.re, im - o.im)
    operator fun times(o: Complex) = Complex(re * o.re - im * o.im, re * o.im + im * o.re)
    operator fun div(o: Complex): Complex {
        val d = o.re * o.re + o.im * o.im
        return Complex((re * o.re + im * o.im) / d, (im * o.re - re * o.im) / d)
    }
    fun abs() = hypot(re, im)
    val isReal get() = im == 0.0

    override fun toString(): String {
        val r = NumberFormatter.format(re)
        if (im == 0.0) return r
        val i = NumberFormatter.format(abs(im))
        val sign = if (im < 0) "−" else "+"
        return if (re == 0.0) "${if (im < 0) "−" else ""}${i}i" else "$r $sign ${i}i"
    }
}

/**
 * Raíces de un polinomio con coeficientes [a_n, ..., a_1, a_0] (Durand–Kerner + pulido de Newton).
 * Devuelve reales primero, luego complejas.
 */
fun polynomialRoots(coefficients: List<Double>): List<Complex> {
    var c = coefficients.dropWhile { it == 0.0 }
    if (c.size < 2) throw CalcException("El coeficiente principal no puede ser 0")
    val roots = mutableListOf<Complex>()
    // Raíces en cero (a_0 = 0).
    while (c.size > 1 && c.last() == 0.0) {
        roots += Complex(0.0)
        c = c.dropLast(1)
    }
    val n = c.size - 1
    if (n >= 1) {
        val monic = c.map { it / c[0] }
        val z = MutableList(n) { k ->
            val r = 1 + monic.drop(1).maxOf { abs(it) }
            Complex(r * 0.9 * cos(2 * Math.PI * k / n + 0.4), r * 0.9 * sin(2 * Math.PI * k / n + 0.4))
        }
        fun p(x: Complex) = monic.fold(Complex(0.0)) { acc, a -> acc * x + Complex(a) }
        for (iteration in 0 until 2000) {
            var change = 0.0
            for (i in 0 until n) {
                var den = Complex(1.0)
                for (j in 0 until n) if (j != i) den *= (z[i] - z[j])
                val delta = p(z[i]) / den
                z[i] = z[i] - delta
                change = max(change, delta.abs() / max(1.0, z[i].abs()))
            }
            if (change < 1e-15) break
        }
        roots += z.map { cleanComplex(it, monic) }
    }
    return roots.sortedWith(compareBy<Complex>({ !it.isReal }, { it.re }, { it.im }))
}

private fun cleanComplex(z: Complex, monic: List<Double>): Complex {
    val scale = max(1.0, z.abs())
    var re = roundSig(z.re, 11)
    var im = roundSig(z.im, 11)
    if (abs(z.im) < 1e-9 * scale) im = 0.0
    if (abs(z.re) < 1e-12 * scale) re = 0.0
    if (im == 0.0) {
        // Pulido de Newton en reales.
        var x = z.re
        repeat(5) {
            var pv = 0.0
            var dv = 0.0
            for (a in monic) { dv = dv * x + pv; pv = pv * x + a }
            if (dv != 0.0) x -= pv / dv
        }
        re = if (abs(x) < 1e-12) 0.0 else roundSig(x, 12)
    }
    return Complex(re, im)
}

// ---------- Sistemas lineales ----------

/** Resuelve A·x = b por eliminación de Gauss con pivoteo parcial. */
fun solveLinear(a: List<List<Double>>, b: List<Double>): List<Double> {
    val n = b.size
    val m = Array(n) { i -> DoubleArray(n + 1) { j -> if (j < n) a[i][j] else b[i] } }
    for (col in 0 until n) {
        val pivot = (col until n).maxBy { abs(m[it][col]) }
        if (abs(m[pivot][col]) < 1e-12) {
            throw CalcException("El sistema no tiene solución única (determinante = 0)")
        }
        val tmp = m[pivot]; m[pivot] = m[col]; m[col] = tmp
        for (r in 0 until n) {
            if (r == col) continue
            val factor = m[r][col] / m[col][col]
            for (k in col..n) m[r][k] -= factor * m[col][k]
        }
    }
    return List(n) { i ->
        val v = m[i][n] / m[i][i]
        if (abs(v) < 1e-13) 0.0 else roundSig(v, 12)
    }
}

fun determinant(a: List<List<Double>>): Double {
    val n = a.size
    val m = Array(n) { i -> a[i].toDoubleArray() }
    var det = 1.0
    for (col in 0 until n) {
        val pivot = (col until n).maxBy { abs(m[it][col]) }
        if (m[pivot][col] == 0.0) return 0.0
        if (pivot != col) { val t = m[pivot]; m[pivot] = m[col]; m[col] = t; det = -det }
        det *= m[col][col]
        for (r in col + 1 until n) {
            val factor = m[r][col] / m[col][col]
            for (k in col until n) m[r][k] -= factor * m[col][k]
        }
    }
    return roundSig(det, 12)
}

// ---------- Estadística ----------

data class Stats(
    val n: Int,
    val sum: Double,
    val mean: Double,
    val median: Double,
    val modes: List<Double>,
    val min: Double,
    val max: Double,
    val q1: Double,
    val q3: Double,
    val varPop: Double,
    val varSample: Double,
    val sdPop: Double,
    val sdSample: Double,
    val sumSquares: Double,
    val geometricMean: Double?,
    val cv: Double?,
)

fun parseNumberList(text: String, ctx: EvalContext = EvalContext()): List<Double> =
    text.split(Regex("[\\s,;]+")).filter { it.isNotBlank() }.map { token ->
        try {
            Evaluator.compile(token).evaluate(ctx)
        } catch (e: CalcException) {
            throw CalcException("Dato inválido: \"$token\"")
        }
    }

fun statistics(data: List<Double>): Stats {
    if (data.isEmpty()) throw CalcException("Ingresa al menos un dato")
    val s = data.sorted()
    val n = s.size
    val sum = s.sum()
    val mean = sum / n
    val ss = s.sumOf { (it - mean) * (it - mean) }
    val counts = s.groupingBy { it }.eachCount()
    val top = counts.values.max()
    val modes = if (top == 1) emptyList() else counts.filterValues { it == top }.keys.toList()
    val varPop = ss / n
    val varSample = if (n > 1) ss / (n - 1) else Double.NaN
    return Stats(
        n = n,
        sum = sum,
        mean = mean,
        median = quantile(s, 0.5),
        modes = modes,
        min = s.first(),
        max = s.last(),
        q1 = quantile(s, 0.25),
        q3 = quantile(s, 0.75),
        varPop = varPop,
        varSample = varSample,
        sdPop = sqrt(varPop),
        sdSample = sqrt(varSample),
        sumSquares = s.sumOf { it * it },
        geometricMean = if (s.all { it > 0 }) kotlin.math.exp(s.sumOf { kotlin.math.ln(it) } / n) else null,
        cv = if (mean != 0.0 && n > 1) sqrt(varSample) / abs(mean) * 100 else null,
    )
}

/** Cuantil con interpolación lineal (igual que Excel CUARTIL.INC). */
private fun quantile(sorted: List<Double>, p: Double): Double {
    val pos = (sorted.size - 1) * p
    val lo = pos.toInt()
    val frac = pos - lo
    return if (lo + 1 < sorted.size) sorted[lo] + frac * (sorted[lo + 1] - sorted[lo]) else sorted[lo]
}

enum class RegressionType(val label: String, val formula: String) {
    LINEAR("Lineal", "y = a + b·x"),
    QUADRATIC("Cuadrática", "y = a + b·x + c·x²"),
    EXPONENTIAL("Exponencial", "y = a·e^(b·x)"),
    LOGARITHMIC("Logarítmica", "y = a + b·ln(x)"),
    POWER("Potencial", "y = a·x^b"),
}

data class Regression(
    val type: RegressionType,
    val coefficients: List<Double>,
    val r2: Double,
    val r: Double?,
    val predict: (Double) -> Double,
) {
    /** Expresión lista para graficar. */
    val expression: String
        get() {
            fun f(v: Double) = "(" + NumberFormatter.format(v) + ")"
            val c = coefficients
            return when (type) {
                RegressionType.LINEAR -> "${f(c[0])}+${f(c[1])}x"
                RegressionType.QUADRATIC -> "${f(c[0])}+${f(c[1])}x+${f(c[2])}x^2"
                RegressionType.EXPONENTIAL -> "${f(c[0])}e^(${f(c[1])}x)"
                RegressionType.LOGARITHMIC -> "${f(c[0])}+${f(c[1])}ln(x)"
                RegressionType.POWER -> "${f(c[0])}x^${f(c[1])}"
            }
        }
}

fun regression(xs: List<Double>, ys: List<Double>, type: RegressionType): Regression {
    if (xs.size != ys.size) throw CalcException("X e Y deben tener la misma cantidad de datos (${xs.size} ≠ ${ys.size})")
    val minPoints = if (type == RegressionType.QUADRATIC) 3 else 2
    if (xs.size < minPoints) throw CalcException("Se necesitan al menos $minPoints puntos")
    val n = xs.size

    fun linearFit(x: List<Double>, y: List<Double>): Pair<Double, Double> {
        val mx = x.average()
        val my = y.average()
        val sxx = x.sumOf { (it - mx).pow(2) }
        if (sxx == 0.0) throw CalcException("Todos los X son iguales")
        val sxy = x.indices.sumOf { (x[it] - mx) * (y[it] - my) }
        val b = sxy / sxx
        return (my - b * mx) to b
    }

    val coefficients: List<Double>
    val predict: (Double) -> Double
    when (type) {
        RegressionType.LINEAR -> {
            val (a, b) = linearFit(xs, ys)
            coefficients = listOf(a, b)
            predict = { a + b * it }
        }
        RegressionType.QUADRATIC -> {
            val s = DoubleArray(5) { k -> xs.sumOf { it.pow(k) } }
            val t = DoubleArray(3) { k -> xs.indices.sumOf { ys[it] * xs[it].pow(k) } }
            val sol = solveLinear(
                listOf(listOf(s[0], s[1], s[2]), listOf(s[1], s[2], s[3]), listOf(s[2], s[3], s[4])),
                t.toList(),
            )
            coefficients = sol
            predict = { sol[0] + sol[1] * it + sol[2] * it * it }
        }
        RegressionType.EXPONENTIAL -> {
            if (ys.any { it <= 0 }) throw CalcException("La regresión exponencial necesita Y > 0")
            val (lnA, b) = linearFit(xs, ys.map { kotlin.math.ln(it) })
            val a = kotlin.math.exp(lnA)
            coefficients = listOf(a, b)
            predict = { a * kotlin.math.exp(b * it) }
        }
        RegressionType.LOGARITHMIC -> {
            if (xs.any { it <= 0 }) throw CalcException("La regresión logarítmica necesita X > 0")
            val (a, b) = linearFit(xs.map { kotlin.math.ln(it) }, ys)
            coefficients = listOf(a, b)
            predict = { a + b * kotlin.math.ln(it) }
        }
        RegressionType.POWER -> {
            if (xs.any { it <= 0 } || ys.any { it <= 0 }) throw CalcException("La regresión potencial necesita X > 0 e Y > 0")
            val (lnA, b) = linearFit(xs.map { kotlin.math.ln(it) }, ys.map { kotlin.math.ln(it) })
            val a = kotlin.math.exp(lnA)
            coefficients = listOf(a, b)
            predict = { a * it.pow(b) }
        }
    }
    val my = ys.average()
    val ssTot = ys.sumOf { (it - my).pow(2) }
    val ssRes = (0 until n).sumOf { (ys[it] - predict(xs[it])).pow(2) }
    val r2 = if (ssTot == 0.0) 1.0 else 1 - ssRes / ssTot
    val r = if (type == RegressionType.LINEAR) {
        val mx = xs.average()
        val sxy = xs.indices.sumOf { (xs[it] - mx) * (ys[it] - my) }
        val sxx = xs.sumOf { (it - mx).pow(2) }
        if (ssTot == 0.0) null else sxy / sqrt(sxx * ssTot)
    } else null
    return Regression(type, coefficients.map { roundSig(it, 12) }, r2, r, predict)
}
