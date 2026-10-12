package com.ian.calculadora.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ian.calculadora.CalculatorViewModel
import com.ian.calculadora.engine.CalcException
import com.ian.calculadora.engine.EvalContext
import com.ian.calculadora.engine.NumberFormatter
import com.ian.calculadora.engine.RegressionType
import com.ian.calculadora.engine.parseNumberList
import com.ian.calculadora.engine.regression
import com.ian.calculadora.engine.statistics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(vm: CalculatorViewModel, onBack: () -> Unit, onPlot: (String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    ToolScaffold("Estadística", onBack) { actions ->
        PrimaryTabRow(selectedTabIndex = tab) {
            listOf("Una variable", "Regresión").forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
        val ctx = EvalContext(vm.state.angleMode)
        if (tab == 0) OneVariable(vm, ctx, actions) else RegressionTab(vm, ctx, actions, onPlot)
    }
}

private fun fmt(v: Double) = if (v.isNaN()) "—" else NumberFormatter.formatGrouped(v)

@Composable
private fun OneVariable(vm: CalculatorViewModel, ctx: EvalContext, actions: ToolActions) {
    val data = rememberPersisted(vm, "stats_data", "12, 15, 11, 18, 15, 20, 14")
    HelpText("Separa los datos con comas, espacios o saltos de línea.")
    ExprField(data.value, { data.value = it }, "Datos", singleLine = false, minLines = 3)
    val result = runCatching { statistics(parseNumberList(data.value, ctx)) }
    result.exceptionOrNull()?.let { ErrorText((it as? CalcException)?.message ?: "Datos inválidos") }
    result.getOrNull()?.let { s ->
        SectionCard("Resumen") {
            ResultRow("n", s.n.toString(), actions)
            ResultRow("Media x̄", fmt(s.mean), actions, highlight = true)
            ResultRow("Mediana", fmt(s.median), actions)
            ResultRow("Moda", if (s.modes.isEmpty()) "sin moda" else s.modes.joinToString(", ") { fmt(it) }, actions)
            ResultRow("Σx", fmt(s.sum), actions)
            ResultRow("Σx²", fmt(s.sumSquares), actions)
        }
        SectionCard("Dispersión") {
            ResultRow("σ (población)", fmt(s.sdPop), actions, highlight = true)
            ResultRow("s (muestra)", fmt(s.sdSample), actions, highlight = true)
            ResultRow("σ² (población)", fmt(s.varPop), actions)
            ResultRow("s² (muestra)", fmt(s.varSample), actions)
            s.cv?.let { ResultRow("Coef. de variación", fmt(it) + " %", actions) }
            ResultRow("Mínimo", fmt(s.min), actions)
            ResultRow("Máximo", fmt(s.max), actions)
            ResultRow("Rango", fmt(s.max - s.min), actions)
            ResultRow("Q1", fmt(s.q1), actions)
            ResultRow("Q3", fmt(s.q3), actions)
            ResultRow("Rango intercuartil", fmt(s.q3 - s.q1), actions)
            s.geometricMean?.let { ResultRow("Media geométrica", fmt(it), actions) }
        }
    }
}

@Composable
private fun RegressionTab(vm: CalculatorViewModel, ctx: EvalContext, actions: ToolActions, onPlot: (String) -> Unit) {
    val xs = rememberPersisted(vm, "reg_x", "1, 2, 3, 4, 5")
    val ys = rememberPersisted(vm, "reg_y", "2.1, 3.9, 6.2, 7.8, 10.1")
    var type by rememberSaveable { mutableStateOf(RegressionType.LINEAR) }
    val xPredict = rememberPersisted(vm, "reg_predict", "6")

    ExprField(xs.value, { xs.value = it }, "Datos X", singleLine = false, minLines = 2)
    ExprField(ys.value, { ys.value = it }, "Datos Y", singleLine = false, minLines = 2)
    ChoiceChips(RegressionType.entries, type, { it.label }) { type = it }

    val result = runCatching { regression(parseNumberList(xs.value, ctx), parseNumberList(ys.value, ctx), type) }
    result.exceptionOrNull()?.let { ErrorText((it as? CalcException)?.message ?: "Datos inválidos") }
    result.getOrNull()?.let { r ->
        SectionCard("${type.label}: ${type.formula}") {
            "abc".take(r.coefficients.size).forEachIndexed { i, ch ->
                ResultRow(ch.toString(), NumberFormatter.format(r.coefficients[i]), actions, highlight = true)
            }
            r.r?.let { ResultRow("r (correlación)", NumberFormatter.format(it), actions) }
            ResultRow("R²", NumberFormatter.format(r.r2), actions)
            ResultRow("Ecuación", r.expression, actions)
        }
        SectionCard("Predicción") {
            ExprField(xPredict.value, { xPredict.value = it }, "x")
            val px = runCatching { parseNumberList(xPredict.value, ctx).single() }.getOrNull()
            if (px != null) {
                val y = r.predict(px)
                ResultRow("ŷ", if (y.isFinite()) NumberFormatter.format(y) else "no definida", actions, highlight = true)
            }
        }
        OutlinedButton(onClick = { onPlot(r.expression) }, modifier = Modifier.fillMaxWidth()) {
            Text("Ver curva en Gráficas")
        }
    }
}
