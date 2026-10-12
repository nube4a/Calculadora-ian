package com.ian.calculadora.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ian.calculadora.CalculatorViewModel
import com.ian.calculadora.engine.CalcException
import com.ian.calculadora.engine.EvalContext
import com.ian.calculadora.engine.AngleMode
import com.ian.calculadora.engine.Evaluator
import com.ian.calculadora.engine.NumberFormatter
import com.ian.calculadora.engine.Sym
import com.ian.calculadora.engine.Symbolic
import com.ian.calculadora.engine.findRoots

private data class Analysis(
    val f: Sym,
    val df: Sym,
    val rules: List<String>,
    val d2f: Sym,
    val expanded: String?,
    val factored: String?,
)

@Composable
fun AlgebraScreen(vm: CalculatorViewModel, onBack: () -> Unit, onGraph: (List<String>) -> Unit) {
    val input = rememberPersisted(vm, "alg_f", "x^3 − 3x^2 + 2")
    val point = rememberPersisted(vm, "alg_a", "1")
    val from = rememberPersisted(vm, "alg_from", "−10")
    val to = rememberPersisted(vm, "alg_to", "10")

    val result: Result<Analysis> = remember(input.value) {
        runCatching {
            val f = Symbolic.parse(input.value)
            val d = Symbolic.derivative(f)
            val d2 = Symbolic.derivative(d.derivative).derivative
            val coeffs = Symbolic.polynomialCoefficients(input.value)
            Analysis(
                f = f,
                df = d.derivative,
                rules = d.rules,
                d2f = d2,
                expanded = coeffs?.let { Symbolic.polynomialSym(it).toString() },
                factored = coeffs?.let { Symbolic.factorText(it) },
            )
        }
    }

    ToolScaffold("Derivadas y álgebra", onBack) { actions ->
        ExprField(input.value, { input.value = it }, "f(x) =",
            error = result.exceptionOrNull()?.let { (it as? CalcException)?.message ?: "Expresión inválida" }
                .takeIf { input.value.isNotBlank() })
        HelpText("Derivación simbólica exacta (las funciones trigonométricas se derivan en radianes). Escribe con el teclado: x^2, sqrt(x), sin(x), e^x, ln(x)…")

        val a = result.getOrNull() ?: return@ToolScaffold

        SectionCard("Resultado") {
            ResultRow("f(x)", a.f.toString(), actions)
            ResultRow("f′(x)", a.df.toString(), actions, highlight = true)
            ResultRow("f″(x)", a.d2f.toString(), actions)
            if (a.rules.isNotEmpty()) {
                HelpText("Reglas usadas: " + a.rules.joinToString(", "))
            }
        }

        if (a.expanded != null) {
            SectionCard("Polinomio") {
                ResultRow("Expandido", a.expanded, actions)
                a.factored?.let { ResultRow("Factorizado", it, actions, highlight = true) }
            }
        }

        SectionCard("En un punto") {
            ExprField(point.value, { point.value = it }, "x =")
            val x = runCatching { Evaluator.compile(point.value).evaluate(EvalContext(AngleMode.RAD)) }.getOrNull()
            if (x != null) {
                val fx = Symbolic.evaluate(a.f, x)
                val dfx = Symbolic.evaluate(a.df, x)
                val d2fx = Symbolic.evaluate(a.d2f, x)
                ResultRow("f(a)", fmtOrUndefined(fx), actions)
                ResultRow("f′(a)  (pendiente)", fmtOrUndefined(dfx), actions, highlight = true)
                ResultRow("f″(a)", fmtOrUndefined(d2fx), actions)
                val tangent = runCatching { Symbolic.tangent(a.f, x).toString() }.getOrNull()
                tangent?.let {
                    ResultRow("Recta tangente", "y = $it", actions)
                    OutlinedButton(onClick = { onGraph(listOf(input.value, it)) }) { Text("Graficar f y su tangente") }
                }
            }
        }

        SectionCard("Estudio de la función") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExprField(from.value, { from.value = it }, "Desde", Modifier.weight(1f))
                ExprField(to.value, { to.value = it }, "Hasta", Modifier.weight(1f))
            }
            val study = remember(a, from.value, to.value) {
                runCatching {
                    val ctx = EvalContext(AngleMode.RAD)
                    val lo = Evaluator.compile(from.value).evaluate(ctx)
                    val hi = Evaluator.compile(to.value).evaluate(ctx)
                    val f = Evaluator.compile(a.f.toString())
                    val df = Evaluator.compile(a.df.toString())
                    val d2f = Evaluator.compile(a.d2f.toString())
                    val roots = findRoots({ f.at(it, ctx) }, lo, hi)
                    val critical = findRoots({ df.at(it, ctx) }, lo, hi).map { x ->
                        val c = d2f.at(x, ctx)
                        val kind = when {
                            c > 1e-9 -> "Mínimo"
                            c < -1e-9 -> "Máximo"
                            else -> "Punto crítico"
                        }
                        Triple(kind, x, f.at(x, ctx))
                    }
                    val inflections = findRoots({ d2f.at(it, ctx) }, lo, hi).filter { x ->
                        val h = 1e-4 * maxOf(1.0, kotlin.math.abs(x))
                        d2f.at(x - h, ctx) * d2f.at(x + h, ctx) < 0
                    }.map { x -> Triple("Inflexión", x, f.at(x, ctx)) }
                    Triple(roots, critical, inflections)
                }
            }
            study.exceptionOrNull()?.let { ErrorText((it as? CalcException)?.message ?: "Intervalo inválido") }
            study.getOrNull()?.let { (roots, critical, inflections) ->
                ResultRow(
                    "Raíces",
                    if (roots.isEmpty()) "ninguna" else roots.take(10).joinToString(", ") { NumberFormatter.format(it) },
                    actions,
                )
                (critical + inflections).sortedBy { it.second }.take(20).forEach { (kind, x, y) ->
                    ResultRow(kind, "(${NumberFormatter.format(x)}, ${fmtOrUndefined(y)})", actions)
                }
                if (critical.isEmpty() && inflections.isEmpty()) HelpText("Sin máximos, mínimos ni inflexiones en el intervalo.")
            }
        }

        OutlinedButton(
            onClick = { onGraph(listOf(input.value, a.df.toString())) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Graficar f y f′") }
    }
}

private fun fmtOrUndefined(v: Double) = if (v.isFinite()) NumberFormatter.format(v) else "no definida"
