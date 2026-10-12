package com.ian.calculadora.engine

import kotlin.math.abs
import kotlin.math.acosh
import kotlin.math.asinh
import kotlin.math.atanh
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tanh

data class SolveStep(val title: String, val math: String)

data class StepSolution(
    val method: String,
    val steps: List<SolveStep>,
    /** Soluciones finales en texto (pueden ser complejas o "infinitas"). */
    val solutions: List<String>,
    /** Soluciones reales numéricas (para usar en la calculadora). */
    val values: List<Double>,
    /** Función para graficar: izquierda − derecha. */
    val graphExpression: String?,
)

/**
 * Resolución paso a paso al estilo de un cuaderno:
 * 1) Despeje: si x aparece una sola vez, se "pelan" las operaciones de afuera hacia adentro.
 * 2) Polinomios (x aparece varias veces): se reduce a a·xⁿ + … = 0 (lineal, cuadrática con Δ, grado 3-4).
 * 3) Método numérico como último recurso.
 */
object StepSolver {

    fun solve(input: String, angle: AngleMode = AngleMode.RAD): StepSolution {
        val text = input.trim()
        if (text.isEmpty()) throw CalcException("Escribe una ecuación o expresión")
        val parts = text.split('=')
        if (parts.size > 2) throw CalcException("Solo puede haber un signo =")
        val ctx = EvalContext(angle)

        val lhs = Evaluator.compile(parts[0]).root
        val rhs = if (parts.size == 2) Evaluator.compile(parts[1]).root else NumNode(0.0)

        if (!containsX(lhs) && !containsX(rhs)) {
            // No es una ecuación en x: solo se evalúa.
            val v = Expression(lhs).evaluate(ctx)
            val steps = mutableListOf(SolveStep("Expresión", show(lhs)))
            if (parts.size == 2) {
                val r = Expression(rhs).evaluate(ctx)
                val ok = abs(v - r) <= 1e-9 * maxOf(1.0, abs(v), abs(r))
                steps += SolveStep("Comparación", "${num(v)} ${if (ok) "=" else "≠"} ${num(r)}")
                return StepSolution("Evaluación", steps, listOf(if (ok) "Verdadero" else "Falso"), emptyList(), null)
            }
            steps += SolveStep("Resultado", "= ${withFraction(v)}")
            return StepSolution("Evaluación", steps, listOf(withFraction(v)), listOf(v), null)
        }

        val steps = mutableListOf(SolveStep("Ecuación", "${show(lhs)} = ${show(rhs)}"))
        val graph = "(${show(lhs)})−(${show(rhs)})"

        isolate(lhs, rhs, ctx, steps)?.let { values ->
            return finish("Despeje", steps, values, lhs, rhs, ctx, graph)
        }

        val f = Expression(BinNode('-', lhs, rhs))
        polynomial(f, ctx, steps)?.let { return it.copy(graphExpression = graph) }

        // Método numérico.
        val numSteps = mutableListOf(steps.first())
        numSteps += SolveStep("Se busca numéricamente donde", "${show(lhs)} − (${show(rhs)}) = 0")
        val roots = findRoots({ f.at(it, ctx) }, -1000.0, 1000.0, 20000)
        numSteps += SolveStep(
            "Rastreo de cambios de signo en [−1000, 1000] y bisección",
            if (roots.isEmpty()) "No se encontraron raíces reales" else "${roots.size} raíz(ces) encontradas",
        )
        return StepSolution(
            "Numérico", numSteps, roots.map { "x = ${num(it)}" }, roots, graph,
        )
    }

    // ---------- Despeje ----------

    private fun containsX(n: Node): Boolean = when (n) {
        is ConstNode -> n.name == "x"
        is NegNode -> containsX(n.n)
        is PctNode -> containsX(n.n)
        is FactNode -> containsX(n.n)
        is BinNode -> containsX(n.l) || containsX(n.r)
        is CallNode -> n.args.any { containsX(it) }
        is NumNode -> false
    }

    /** Devuelve los valores de x, o null si no se puede despejar (x en ambos lados de una operación...). */
    private fun isolate(lhs0: Node, rhs0: Node, ctx: EvalContext, steps: MutableList<SolveStep>): List<Double>? {
        var node: Node
        var values: List<Double>
        val local = mutableListOf<SolveStep>()
        when {
            containsX(lhs0) && !containsX(rhs0) -> { node = lhs0; values = listOf(Expression(rhs0).evaluate(ctx)) }
            containsX(rhs0) && !containsX(lhs0) -> {
                node = rhs0
                values = listOf(Expression(lhs0).evaluate(ctx))
                local += SolveStep("Intercambiar lados", "${show(node)} = ${nums(values)}")
            }
            else -> return null
        }
        fun c(n: Node) = Expression(n).evaluate(ctx)
        val deg = ctx.angle == AngleMode.DEG
        val period = if (deg) "360°" else "2π"

        while (true) {
            val n = node
            if (n is ConstNode && n.name == "x") break
            var title: String
            var next: Node
            var newValues: List<Double>
            when {
                n is NegNode -> {
                    title = "Multiplicar ambos lados por −1"
                    next = n.n
                    newValues = values.map { -it }
                }
                n is PctNode -> {
                    title = "Multiplicar ambos lados por 100"
                    next = n.n
                    newValues = values.map { it * 100 }
                }
                n is BinNode -> {
                    val xl = containsX(n.l)
                    val xr = containsX(n.r)
                    if (xl && xr) return null
                    val k = c(if (xl) n.r else n.l)
                    next = if (xl) n.l else n.r
                    when (n.op) {
                        '+' -> {
                            if (n.r is PctNode) return null
                            title = if (k >= 0) "Restar ${num(k)} en ambos lados" else "Sumar ${num(-k)} en ambos lados"
                            newValues = values.map { it - k }
                        }
                        '-' -> if (xl) {
                            if (n.r is PctNode) return null
                            title = if (k >= 0) "Sumar ${num(k)} en ambos lados" else "Restar ${num(-k)} en ambos lados"
                            newValues = values.map { it + k }
                        } else {
                            title = "Despejar: ${show(next)} = ${num(k)} − (lado derecho)"
                            newValues = values.map { k - it }
                        }
                        '*' -> {
                            if (k == 0.0) return null
                            title = "Dividir ambos lados entre ${num(k)}"
                            newValues = values.map { it / k }
                        }
                        '/' -> if (xl) {
                            title = "Multiplicar ambos lados por ${num(k)}"
                            newValues = values.map { it * k }
                        } else {
                            title = "Invertir: ${show(next)} = ${num(k)} ÷ (lado derecho)"
                            newValues = values.filter { it != 0.0 }.map { k / it }
                        }
                        '^' -> if (xl) {
                            val result = rootStep(k, values) ?: return null
                            title = result.first
                            newValues = result.second
                        } else {
                            if (k <= 0 || k == 1.0) return null
                            title = if (k == Math.E) "Aplicar logaritmo natural (ln) a ambos lados" else "Aplicar logaritmo en base ${num(k)}"
                            newValues = values.filter { it > 0 }.map { ln(it) / ln(k) }
                        }
                        else -> return null
                    }
                }
                n is CallNode -> {
                    if (n.args.drop(1).any { containsX(it) }) return null
                    next = n.args[0]
                    val inv = { v: Double -> if (deg) Math.toDegrees(v) else v }
                    val fwd = { v: Double -> if (deg) Math.toRadians(v) else v }
                    when (n.name) {
                        Names.SQRT -> { title = "Elevar al cuadrado ambos lados"; newValues = values.filter { it >= 0 }.map { it * it } }
                        Names.CBRT -> { title = "Elevar al cubo ambos lados"; newValues = values.map { it * it * it } }
                        Names.ROOT -> {
                            val idx = c(n.args[1])
                            title = "Elevar ambos lados a la ${num(idx)}"
                            newValues = values.filter { it >= 0 || isInt(idx) }.map { it.pow(idx) }
                        }
                        "ln" -> { title = "Aplicar e^ a ambos lados"; newValues = values.map { exp(it) } }
                        "log" -> { title = "Aplicar 10^ a ambos lados"; newValues = values.map { 10.0.pow(it) } }
                        Names.LOG2 -> { title = "Aplicar 2^ a ambos lados"; newValues = values.map { 2.0.pow(it) } }
                        "exp" -> { title = "Aplicar ln a ambos lados"; newValues = values.filter { it > 0 }.map { ln(it) } }
                        Names.ABS -> {
                            title = "Valor absoluto: el interior es ± el lado derecho"
                            newValues = values.filter { it >= 0 }.flatMap { if (it == 0.0) listOf(0.0) else listOf(it, -it) }
                        }
                        "sin" -> {
                            title = "Aplicar sin⁻¹ (soluciones principales; se repiten cada $period)"
                            newValues = values.filter { abs(it) <= 1 }.flatMap {
                                val a = kotlin.math.asin(it)
                                listOf(inv(a), inv(Math.PI - a)).distinct()
                            }
                        }
                        "cos" -> {
                            title = "Aplicar cos⁻¹ (soluciones principales; se repiten cada $period)"
                            newValues = values.filter { abs(it) <= 1 }.flatMap {
                                val a = kotlin.math.acos(it)
                                listOf(inv(a), inv(-a)).distinct()
                            }
                        }
                        "tan" -> {
                            title = "Aplicar tan⁻¹ (se repite cada ${if (deg) "180°" else "π"})"
                            newValues = values.map { inv(kotlin.math.atan(it)) }
                        }
                        Names.ASIN -> { title = "Aplicar sin a ambos lados"; newValues = values.map { kotlin.math.sin(fwd(it)) } }
                        Names.ACOS -> { title = "Aplicar cos a ambos lados"; newValues = values.map { kotlin.math.cos(fwd(it)) } }
                        Names.ATAN -> { title = "Aplicar tan a ambos lados"; newValues = values.map { kotlin.math.tan(fwd(it)) } }
                        "sinh" -> { title = "Aplicar sinh⁻¹"; newValues = values.map { asinh(it) } }
                        "cosh" -> { title = "Aplicar cosh⁻¹ (±)"; newValues = values.filter { it >= 1 }.flatMap { listOf(acosh(it), -acosh(it)).distinct() } }
                        "tanh" -> { title = "Aplicar tanh⁻¹"; newValues = values.filter { abs(it) < 1 }.map { atanh(it) } }
                        Names.ASINH -> { title = "Aplicar sinh"; newValues = values.map { sinh(it) } }
                        Names.ACOSH -> { title = "Aplicar cosh"; newValues = values.filter { it >= 0 }.map { cosh(it) } }
                        Names.ATANH -> { title = "Aplicar tanh"; newValues = values.map { tanh(it) } }
                        else -> return null
                    }
                }
                else -> return null
            }
            values = newValues.map { clean(it) }.distinct()
            node = next
            if (values.isEmpty()) {
                local += SolveStep(title, "Sin solución real")
                steps += local
                return emptyList()
            }
            local += SolveStep(title, "${show(node)} = ${nums(values)}")
        }
        steps += local
        return values
    }

    /** g^k = v  →  g = v^(1/k), con ± si k es par. */
    private fun rootStep(k: Double, values: List<Double>): Pair<String, List<Double>>? {
        if (k == 0.0) return null
        if (isInt(k)) {
            val n = round(k).toLong()
            if (n == 1L) return "Exponente 1" to values
            if (n == -1L) return "Invertir ambos lados" to values.filter { it != 0.0 }.map { 1 / it }
            val name = when (n) { 2L -> "raíz cuadrada"; 3L -> "raíz cúbica"; else -> "raíz de índice $n" }
            return if (n % 2L == 0L) {
                "Sacar $name en ambos lados (±)" to values.filter { it >= 0 }.flatMap {
                    val r = it.pow(1.0 / n)
                    if (r == 0.0) listOf(0.0) else listOf(r, -r)
                }
            } else {
                "Sacar $name en ambos lados" to values.map { if (it < 0) -(-it).pow(1.0 / n) else it.pow(1.0 / n) }
            }
        }
        return "Elevar ambos lados a 1/${num(k)}" to values.filter { it >= 0 }.map { it.pow(1 / k) }
    }

    private fun finish(
        method: String, steps: MutableList<SolveStep>, values: List<Double>,
        lhs: Node, rhs: Node, ctx: EvalContext, graph: String,
    ): StepSolution {
        val valid = mutableListOf<Double>()
        for (v in values.sorted()) {
            ctx.vars["x"] = v
            val l = runCatching { Expression(lhs).evaluate(ctx) }.getOrNull()
            val r = runCatching { Expression(rhs).evaluate(ctx) }.getOrNull()
            ctx.vars.remove("x")
            val ok = l != null && r != null && abs(l - r) <= 1e-7 * maxOf(1.0, abs(l), abs(r))
            val check = "${show(lhs, v)} = ${show(rhs, v)}"
            if (ok) {
                steps += SolveStep("Comprobación con x = ${num(v)}", "$check ✓")
                valid += v
            } else {
                steps += SolveStep("x = ${num(v)} no cumple la ecuación original (se descarta)", "$check ✗")
            }
        }
        val solutions = if (valid.isEmpty()) listOf("Sin solución real") else valid.map { "x = ${withFraction(it)}" }
        return StepSolution(method, steps, solutions, valid, graph)
    }

    // ---------- Polinomios ----------

    private fun polynomial(f: Expression, ctx: EvalContext, steps: MutableList<SolveStep>): StepSolution? {
        val maxDeg = 4
        val xs = (0..maxDeg).map { it - 2.0 } // −2 … 2
        val ys = xs.map { f.at(it, ctx) }
        if (ys.any { !it.isFinite() }) return null
        val coeffsAsc = runCatching {
            solveLinear(xs.map { x -> (0..maxDeg).map { x.pow(it) } }, ys)
        }.getOrNull() ?: return null
        // Verifica que de verdad sea un polinomio (en puntos que no se usaron).
        for (x in listOf(0.37, -3.1, 4.6, 7.9)) {
            val p = coeffsAsc.indices.sumOf { coeffsAsc[it] * x.pow(it) }
            val y = f.at(x, ctx)
            if (!y.isFinite() || abs(p - y) > 1e-7 * maxOf(1.0, abs(y))) return null
        }
        val scale = coeffsAsc.maxOf { abs(it) }.coerceAtLeast(1e-300)
        val c = coeffsAsc.map { if (abs(it) < 1e-10 * scale) 0.0 else snap(it) }
        val degree = c.indexOfLast { it != 0.0 }
        val desc = c.take(degree + 1).reversed()
        val poly = polyText(desc)
        steps += SolveStep("Llevar todo a un lado y simplificar", "$poly = 0")

        val graph = null
        return when (degree) {
            -1 -> StepSolution("Identidad", steps + SolveStep("La ecuación se cumple siempre", "0 = 0"),
                listOf("Infinitas soluciones (todo x)"), emptyList(), graph)
            0 -> StepSolution("Contradicción", steps + SolveStep("No depende de x y no se cumple", "${num(desc[0])} ≠ 0"),
                listOf("Sin solución"), emptyList(), graph)
            1 -> {
                val (a, b) = desc
                steps += SolveStep(
                    if (b >= 0) "Restar ${num(b)} en ambos lados" else "Sumar ${num(-b)} en ambos lados",
                    "${polyText(listOf(a, 0.0))} = ${num(-b)}",
                )
                val x = clean(-b / a)
                if (a != 1.0) steps += SolveStep("Dividir entre ${num(a)}", "x = ${num(-b)} / ${num(a)}")
                steps += SolveStep("Resultado", "x = ${withFraction(x)}")
                StepSolution("Ecuación lineal", steps, listOf("x = ${withFraction(x)}"), listOf(x), graph)
            }
            2 -> {
                val (a, b, cc) = desc
                val d = snap(b * b - 4 * a * cc)
                steps += SolveStep("Coeficientes", "a = ${num(a)},  b = ${num(b)},  c = ${num(cc)}")
                steps += SolveStep("Discriminante Δ = b² − 4ac", "Δ = (${num(b)})² − 4·(${num(a)})·(${num(cc)}) = ${num(d)}")
                steps += SolveStep("Fórmula general", "x = (−b ± √Δ) / 2a = (${num(-b)} ± √${num(d)}) / ${num(2 * a)}")
                when {
                    d > 0 -> {
                        val r = listOf((-b - sqrt(d)) / (2 * a), (-b + sqrt(d)) / (2 * a)).map { clean(it) }.sorted()
                        steps += SolveStep("Δ > 0: dos soluciones reales", r.joinToString(",   ") { "x = ${num(it)}" })
                        StepSolution("Ecuación cuadrática", steps, r.map { "x = ${withFraction(it)}" }, r, graph)
                    }
                    d == 0.0 -> {
                        val r = clean(-b / (2 * a))
                        steps += SolveStep("Δ = 0: una solución doble", "x = ${num(r)}")
                        StepSolution("Ecuación cuadrática", steps, listOf("x = ${withFraction(r)}"), listOf(r), graph)
                    }
                    else -> {
                        val re = clean(-b / (2 * a))
                        val im = clean(sqrt(-d) / abs(2 * a))
                        val roots = listOf(Complex(re, -im), Complex(re, im))
                        steps += SolveStep("Δ < 0: dos soluciones complejas conjugadas", roots.joinToString(",   ") { "x = $it" })
                        StepSolution("Ecuación cuadrática", steps, roots.map { "x = $it" }, emptyList(), graph)
                    }
                }
            }
            else -> {
                val roots = polynomialRoots(desc)
                steps += SolveStep("Polinomio de grado $degree: raíces por el método de Durand–Kerner", "${roots.size} raíces")
                val real = roots.filter { it.isReal }
                if (real.isNotEmpty()) {
                    steps += SolveStep("Forma factorizada (raíces reales)",
                        real.joinToString("") { "(x ${if (it.re < 0) "+ ${num(-it.re)}" else "− ${num(it.re)}"})" } +
                            if (real.size < roots.size) " · (…)" else "")
                }
                StepSolution(
                    "Polinomio de grado $degree", steps,
                    roots.map { "x = ${if (it.isReal) withFraction(it.re) else it.toString()}" },
                    real.map { it.re }, graph,
                )
            }
        }
    }

    private fun polyText(desc: List<Double>): String {
        val deg = desc.size - 1
        val sb = StringBuilder()
        desc.forEachIndexed { i, a ->
            if (a == 0.0) return@forEachIndexed
            val p = deg - i
            val mag = abs(a)
            val coef = if (mag == 1.0 && p > 0) "" else num(mag)
            val term = coef + when (p) { 0 -> ""; 1 -> "x"; else -> "x${superscript(p)}" }
            if (sb.isEmpty()) sb.append(if (a < 0) "−$term" else term)
            else sb.append(if (a < 0) " − $term" else " + $term")
        }
        return sb.toString().ifEmpty { "0" }
    }

    // ---------- Formato ----------

    private fun snap(v: Double): Double {
        val r = round(v)
        return if (abs(v - r) < 1e-9 * maxOf(1.0, abs(v))) r else roundSig(v, 12)
    }

    private fun clean(v: Double) = if (abs(v) < 1e-12) 0.0 else snap(v)

    private fun num(v: Double) = NumberFormatter.format(v)
    private fun nums(values: List<Double>) = values.joinToString("  ó  ") { num(it) }

    /** 0.75 → "3/4 ≈ 0.75" si es una fracción sencilla. */
    fun withFraction(v: Double): String {
        val frac = toFraction(v) ?: return num(v)
        return "$frac  (${num(v)})"
    }

    fun toFraction(v: Double): String? {
        if (!v.isFinite() || isInt(v)) return null
        var h1 = 1L; var h0 = 0L; var k1 = 0L; var k0 = 1L
        var x = abs(v)
        repeat(20) {
            val a = kotlin.math.floor(x).toLong()
            val h = a * h1 + h0; val k = a * k1 + k0
            h0 = h1; h1 = h; k0 = k1; k1 = k
            if (k > 1000) return null
            if (abs(h.toDouble() / k - abs(v)) < 1e-10 * maxOf(1.0, abs(v))) {
                return if (k == 1L) null else "${if (v < 0) "−" else ""}$h/$k"
            }
            val frac = x - a
            if (frac < 1e-12) return null
            x = 1 / frac
        }
        return null
    }

    private fun superscript(n: Int) = n.toString().map { "⁰¹²³⁴⁵⁶⁷⁸⁹"[it - '0'] }.joinToString("")

    /** Texto legible de un nodo; si `x` no es null se sustituye la variable por ese valor. */
    internal fun show(n: Node, x: Double? = null): String = print(n, x)

    private fun prec(n: Node): Int = when (n) {
        is BinNode -> when (n.op) { '+', '-' -> 1; '*', '/', 'm' -> 2; else -> 4 }
        is NegNode -> 3
        is NumNode -> if (n.v < 0) 3 else 6
        is PctNode, is FactNode -> 5
        else -> 6
    }

    private fun wrap(n: Node, min: Int, x: Double?): String {
        val s = print(n, x)
        val p = if (x != null && n is ConstNode && n.name == "x" && x < 0) 3 else prec(n)
        return if (p < min) "($s)" else s
    }

    private fun print(n: Node, x: Double?): String = when (n) {
        is NumNode -> num(n.v)
        is ConstNode -> if (n.name == "x" && x != null) num(x) else n.name
        is NegNode -> "−" + wrap(n.n, 4, x)
        is PctNode -> wrap(n.n, 6, x) + "%"
        is FactNode -> wrap(n.n, 6, x) + "!"
        is CallNode -> when (n.name) {
            Names.SQRT, Names.CBRT -> n.name + wrap(n.args[0], 6, x).let { if (it.startsWith("(")) it else if (prec(n.args[0]) >= 6 && n.args[0] !is CallNode) it else "($it)" }
            else -> n.name + "(" + n.args.joinToString(", ") { print(it, x) } + ")"
        }
        is BinNode -> when (n.op) {
            '+' -> {
                val r = n.r
                if (r is NegNode) print(n.l, x) + " − " + wrap(r.n, 2, x)
                else if (r is NumNode && r.v < 0) print(n.l, x) + " − " + num(-r.v)
                else print(n.l, x) + " + " + wrap(r, 1, x)
            }
            '-' -> wrap(n.l, 1, x) + " − " + wrap(n.r, 2, x)
            '*' -> {
                val implicit = n.l is NumNode && (n.l as NumNode).v >= 0 && x == null &&
                    (n.r is ConstNode || n.r is CallNode || (n.r is BinNode && (n.r as BinNode).op == '^' && (n.r as BinNode).l is ConstNode))
                if (implicit) print(n.l, x) + wrap(n.r, 4, x) else wrap(n.l, 2, x) + "·" + wrap(n.r, 3, x)
            }
            '/' -> wrap(n.l, 2, x) + "/" + wrap(n.r, 3, x)
            'm' -> wrap(n.l, 2, x) + " mod " + wrap(n.r, 3, x)
            else -> wrap(n.l, 5, x) + "^" + wrap(n.r, 6, x)
        }
    }
}

/** Corrige errores típicos del reconocimiento de texto (OCR) en fórmulas. */
object MathText {
    fun normalizeOcr(raw: String): String {
        var s = raw.trim()
            .replace(" ", "")
            .replace('X', 'x')
            .replace('–', '−').replace('—', '−').replace('-', '−')
            .replace(':', '÷')
            .replace('·', '×')
            .replace("**", "^")
            .replace('{', '(').replace('}', ')')
        // "O" entre dígitos → 0; l, I, | entre dígitos → 1.
        s = Regex("(?<=\\d)[Oo](?![a-zA-Z])|(?<![a-zA-Z])[Oo](?=\\d)").replace(s, "0")
        s = Regex("(?<=\\d)[lI|](?=\\d)|(?<=[\\d=+−])[lI|](?=\\d|$)").replace(s, "1")
        // Raíz leída como "V" o "v" antes de un número, x o paréntesis (14Vx → 14√x).
        s = Regex("[Vv](?=[\\dx(])").replace(s, "√")
        // "x2" tras una letra x se suele leer en vez de x² (exponente pequeño).
        s = Regex("x([2-4])(?![\\d.])").replace(s) { "x^${it.groupValues[1]}" }
        return s
    }
}
