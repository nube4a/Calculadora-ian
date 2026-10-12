package com.ian.calculadora.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ian.calculadora.CalculatorViewModel
import com.ian.calculadora.engine.AngleMode
import com.ian.calculadora.engine.CalcException
import com.ian.calculadora.engine.Complex
import com.ian.calculadora.engine.EvalContext
import com.ian.calculadora.engine.Evaluator
import com.ian.calculadora.engine.NumberFormatter
import com.ian.calculadora.engine.determinant
import com.ian.calculadora.engine.findRoots
import com.ian.calculadora.engine.polynomialRoots
import com.ian.calculadora.engine.solveLinear

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverScreen(vm: CalculatorViewModel, onBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    ToolScaffold("Ecuaciones", onBack) { actions ->
        PrimaryTabRow(selectedTabIndex = tab) {
            listOf("f(x) = 0", "Polinomio", "Sistema").forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
        val ctx = EvalContext(vm.state.angleMode)
        when (tab) {
            0 -> EquationTab(vm, ctx, actions)
            1 -> PolynomialTab(vm, ctx, actions)
            else -> SystemTab(vm, ctx, actions)
        }
    }
}

private fun evalField(text: String, ctx: EvalContext, name: String): Double {
    if (text.isBlank()) return 0.0
    return try {
        Evaluator.compile(text).evaluate(ctx)
    } catch (e: CalcException) {
        throw CalcException("$name: ${e.message}")
    }
}

@Composable
private fun EquationTab(vm: CalculatorViewModel, ctx: EvalContext, actions: ToolActions) {
    val equation = rememberPersisted(vm, "solve_eq", "x^3 − 2x = 5")
    val from = rememberPersisted(vm, "solve_from", "−100")
    val to = rememberPersisted(vm, "solve_to", "100")
    var result by remember { mutableStateOf<List<Double>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    HelpText("Escribe una ecuación en x (con o sin \"=\"). Se buscan todas las soluciones reales en el intervalo. Modo de ángulo: ${ctx.angle.name}.")
    ExprField(equation.value, { equation.value = it; result = null }, "Ecuación")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExprField(from.value, { from.value = it }, "Desde", Modifier.weight(1f))
        ExprField(to.value, { to.value = it }, "Hasta", Modifier.weight(1f))
    }
    Button(onClick = {
        error = null
        result = try {
            val f = Evaluator.compileEquation(equation.value)
            val a = evalField(from.value, ctx, "Desde")
            val b = evalField(to.value, ctx, "Hasta")
            val local = EvalContext(ctx.angle)
            findRoots({ f.at(it, local) }, a, b).take(50)
        } catch (e: CalcException) {
            error = e.message
            null
        }
    }, modifier = Modifier.fillMaxWidth()) { Text("Resolver") }

    error?.let { ErrorText(it) }
    result?.let { roots ->
        SectionCard(if (roots.isEmpty()) "Sin soluciones reales en el intervalo" else "${roots.size} solución(es)") {
            roots.forEachIndexed { i, r -> ResultRow("x${sub(i + 1)}", NumberFormatter.format(r), actions, highlight = true) }
            if (roots.isNotEmpty()) {
                Button(
                    onClick = { vm.insertValue(roots.first()) },
                    modifier = Modifier.padding(top = 4.dp),
                ) { Text("Usar x₁ en la calculadora") }
            }
        }
    }
}

@Composable
private fun PolynomialTab(vm: CalculatorViewModel, ctx: EvalContext, actions: ToolActions) {
    var degree by rememberSaveable { mutableIntStateOf(2) }
    val coefficients = (0..6).map { rememberPersisted(vm, "poly_c$it", if (it == 0) "1" else "") }
    var result by remember { mutableStateOf<List<Complex>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    ChoiceChips((2..6).toList(), degree, { "Grado $it" }) { degree = it; result = null }
    HelpText(polynomialTemplate(degree))
    // Coeficientes a (de x^n) ... hasta el término independiente.
    val letters = "abcdefg"
    (0..degree).chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { k ->
                ExprField(
                    coefficients[k].value,
                    { coefficients[k].value = it; result = null },
                    "${letters[k]}  (x${sup(degree - k)})",
                    Modifier.weight(1f),
                )
            }
            repeat(3 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
        }
    }
    Button(onClick = {
        error = null
        result = try {
            val c = (0..degree).map { evalField(coefficients[it].value, ctx, "${letters[it]}") }
            if (c[0] == 0.0) throw CalcException("El coeficiente a no puede ser 0")
            polynomialRoots(c)
        } catch (e: CalcException) {
            error = e.message
            null
        }
    }, modifier = Modifier.fillMaxWidth()) { Text("Calcular raíces") }

    error?.let { ErrorText(it) }
    result?.let { roots ->
        val real = roots.count { it.isReal }
        SectionCard("$real real(es), ${roots.size - real} compleja(s)") {
            roots.forEachIndexed { i, r ->
                ResultRow("x${sub(i + 1)}", r.toString(), actions, highlight = r.isReal)
            }
        }
        if (degree == 2) {
            val c = (0..2).map { runCatching { evalField(coefficients[it].value, ctx, "") }.getOrDefault(0.0) }
            val disc = c[1] * c[1] - 4 * c[0] * c[2]
            SectionCard("Discriminante y vértice") {
                ResultRow("Δ = b² − 4ac", NumberFormatter.format(disc), actions)
                val vx = -c[1] / (2 * c[0])
                ResultRow("Vértice", "(${NumberFormatter.format(vx)}, ${NumberFormatter.format(c[0] * vx * vx + c[1] * vx + c[2])})", actions)
            }
        }
    }
}

@Composable
private fun SystemTab(vm: CalculatorViewModel, ctx: EvalContext, actions: ToolActions) {
    var n by rememberSaveable { mutableIntStateOf(2) }
    val cells = (0 until 4).map { i -> (0..4).map { j -> rememberPersisted(vm, "sys_${i}_$j", "") } }
    var result by remember { mutableStateOf<Pair<List<Double>, Double>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val vars = listOf("x", "y", "z", "w")

    ChoiceChips(listOf(2, 3, 4), n, { "$it×$it" }) { n = it; result = null }
    HelpText("Coeficientes de cada ecuación: a·x + b·y + … = término independiente. Admite expresiones (por ejemplo, sqrt(2) o 1/3).")
    for (i in 0 until n) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (j in 0..n) {
                val col = if (j == n) 4 else j
                ExprField(
                    cells[i][col].value,
                    { cells[i][col].value = it; result = null },
                    if (j == n) "=" else vars[j],
                    Modifier.weight(1f),
                    numeric = false,
                )
            }
        }
    }
    Button(onClick = {
        error = null
        result = try {
            val a = (0 until n).map { i -> (0 until n).map { j -> evalField(cells[i][j].value, ctx, "Fila ${i + 1}") } }
            val b = (0 until n).map { i -> evalField(cells[i][4].value, ctx, "Fila ${i + 1}") }
            solveLinear(a, b) to determinant(a)
        } catch (e: CalcException) {
            error = e.message
            null
        }
    }, modifier = Modifier.fillMaxWidth()) { Text("Resolver sistema") }

    error?.let { ErrorText(it) }
    result?.let { (sol, det) ->
        SectionCard("Solución") {
            sol.forEachIndexed { i, v -> ResultRow(vars[i], NumberFormatter.format(v), actions, highlight = true) }
            ResultRow("det(A)", NumberFormatter.format(det), actions)
        }
    }
}

private fun polynomialTemplate(degree: Int): String {
    val letters = "abcdefg"
    val terms = (0..degree).map { k ->
        val p = degree - k
        when (p) {
            0 -> "${letters[k]}"
            1 -> "${letters[k]}x"
            else -> "${letters[k]}x${sup(p)}"
        }
    }
    return terms.joinToString(" + ") + " = 0"
}

private fun sub(n: Int) = n.toString().map { "₀₁₂₃₄₅₆₇₈₉"[it - '0'] }.joinToString("")
private fun sup(n: Int) = if (n == 1) "" else n.toString().map { "⁰¹²³⁴⁵⁶⁷⁸⁹"[it - '0'] }.joinToString("")
