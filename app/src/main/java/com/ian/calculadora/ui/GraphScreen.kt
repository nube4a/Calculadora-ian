package com.ian.calculadora.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.calculadora.CalculatorViewModel
import com.ian.calculadora.engine.AngleMode
import com.ian.calculadora.engine.CalcException
import com.ian.calculadora.engine.EvalContext
import com.ian.calculadora.engine.Evaluator
import com.ian.calculadora.engine.Expression
import com.ian.calculadora.engine.NumberFormatter
import com.ian.calculadora.engine.derivative
import com.ian.calculadora.engine.findRoots
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private val curveColors = listOf(Color(0xFFFF9800), Color(0xFF42A5F5), Color(0xFF66BB6A))

private data class Marker(val label: String, val x: Double, val y: Double)

@Composable
fun GraphScreen(vm: CalculatorViewModel, onBack: () -> Unit) {
    val f1 = rememberPersisted(vm, "graph_f1", "sin(x)")
    val f2 = rememberPersisted(vm, "graph_f2", "x^2/10−1")
    val f3 = rememberPersisted(vm, "graph_f3", "")
    val fields = listOf(f1, f2, f3)

    var rad by rememberSaveable { mutableStateOf(true) }
    var xMin by rememberSaveable { mutableDoubleStateOf(-10.0) }
    var xMax by rememberSaveable { mutableDoubleStateOf(10.0) }
    var yMin by rememberSaveable { mutableDoubleStateOf(-6.0) }
    var yMax by rememberSaveable { mutableDoubleStateOf(6.0) }
    var traceX by rememberSaveable { mutableStateOf<Double?>(null) }
    var markers by remember { mutableStateOf<List<Marker>>(emptyList()) }
    var analysisError by remember { mutableStateOf<String?>(null) }

    val compiled: List<Pair<Expression?, String?>> = fields.map { field ->
        val text = field.value
        if (text.isBlank()) null to null
        else try {
            Evaluator.compile(text) to null
        } catch (e: CalcException) {
            null to e.message
        }
    }
    val ctx = remember(rad) { EvalContext(if (rad) AngleMode.RAD else AngleMode.DEG) }

    fun zoom(factor: Double) {
        val cx = (xMin + xMax) / 2
        val cy = (yMin + yMax) / 2
        val hw = (xMax - xMin) / 2 * factor
        val hh = (yMax - yMin) / 2 * factor
        xMin = cx - hw; xMax = cx + hw; yMin = cy - hh; yMax = cy + hh
    }

    ToolScaffold("Gráficas", onBack) { actions ->
        fields.forEachIndexed { i, field ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(curveColors[i]))
                ExprField(
                    value = field.value,
                    onValueChange = { field.value = it; markers = emptyList() },
                    label = "f${"₁₂₃"[i]}(x) =",
                    error = compiled[i].second,
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(onClick = { rad = !rad; markers = emptyList() }, label = { Text(if (rad) "RAD" else "DEG") })
            FilledTonalIconButton(onClick = { zoom(0.5) }) { Icon(Icons.Outlined.Add, "Acercar") }
            FilledTonalIconButton(onClick = { zoom(2.0) }) { Icon(Icons.Outlined.Remove, "Alejar") }
            FilledTonalIconButton(onClick = {
                xMin = -10.0; xMax = 10.0; yMin = -6.0; yMax = 6.0; traceX = null
            }) { Icon(Icons.Outlined.CenterFocusStrong, "Restablecer") }
            FilledTonalButton(onClick = {
                analysisError = null
                markers = try {
                    analyze(compiled.map { it.first }, ctx, xMin, xMax)
                } catch (e: CalcException) {
                    analysisError = e.message
                    emptyList()
                }
                if (markers.isEmpty() && analysisError == null) analysisError = "No hay puntos notables en la vista"
            }) { Text("Analizar") }
        }

        val textMeasurer = rememberTextMeasurer()
        val gridColor = MaterialTheme.colorScheme.outlineVariant
        val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
        val labelStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val traceColor = MaterialTheme.colorScheme.primary
        val markerColor = MaterialTheme.colorScheme.error

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(380.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoomChange, _ ->
                        val w = size.width.toDouble()
                        val h = size.height.toDouble()
                        val dx = (xMax - xMin) / w
                        val dy = (yMax - yMin) / h
                        xMin -= pan.x * dx; xMax -= pan.x * dx
                        yMin += pan.y * dy; yMax += pan.y * dy
                        if (zoomChange != 1f) {
                            val s = 1.0 / zoomChange
                            val cx = xMin + centroid.x * (xMax - xMin) / w
                            val cy = yMax - centroid.y * (yMax - yMin) / h
                            xMin = cx + (xMin - cx) * s; xMax = cx + (xMax - cx) * s
                            yMin = cy + (yMin - cy) * s; yMax = cy + (yMax - cy) * s
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        traceX = xMin + offset.x / size.width * (xMax - xMin)
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            fun sx(x: Double) = ((x - xMin) / (xMax - xMin) * w).toFloat()
            fun sy(y: Double) = ((yMax - y) / (yMax - yMin) * h).toFloat()

            // Cuadrícula y etiquetas
            val stepX = niceStep((xMax - xMin) / 8)
            val stepY = niceStep((yMax - yMin) / 6)
            val axisY = sy(0.0).coerceIn(0f, h - 14f)
            val axisX = sx(0.0).coerceIn(0f, w - 30f)
            var gx = ceil(xMin / stepX) * stepX
            while (gx <= xMax) {
                val px = sx(gx)
                drawLine(gridColor, Offset(px, 0f), Offset(px, h), 1f)
                if (abs(gx) > stepX / 2) {
                    drawLabel(textMeasurer, NumberFormatter.format(roundTo(gx, stepX)), Offset(px + 2, axisY + 1), labelStyle)
                }
                gx += stepX
            }
            var gy = ceil(yMin / stepY) * stepY
            while (gy <= yMax) {
                val py = sy(gy)
                drawLine(gridColor, Offset(0f, py), Offset(w, py), 1f)
                if (abs(gy) > stepY / 2) {
                    drawLabel(textMeasurer, NumberFormatter.format(roundTo(gy, stepY)), Offset(axisX + 3, py - 14), labelStyle)
                }
                gy += stepY
            }
            drawLine(axisColor, Offset(0f, sy(0.0)), Offset(w, sy(0.0)), 2f)
            drawLine(axisColor, Offset(sx(0.0), 0f), Offset(sx(0.0), h), 2f)

            // Curvas
            compiled.forEachIndexed { i, (expr, _) ->
                if (expr == null) return@forEachIndexed
                val path = Path()
                var penDown = false
                var prevPy = 0f
                val samples = w.toInt().coerceAtLeast(2)
                for (k in 0..samples) {
                    val x = xMin + (xMax - xMin) * k / samples
                    val y = expr.at(x, ctx)
                    if (!y.isFinite()) { penDown = false; continue }
                    val py = sy(y).coerceIn(-h * 4, h * 5)
                    val px = k.toFloat() * w / samples
                    if (penDown && abs(py - prevPy) < h) path.lineTo(px, py) else path.moveTo(px, py)
                    penDown = true
                    prevPy = py
                }
                drawPath(path, curveColors[i], style = Stroke(width = 5f))
            }

            // Puntos notables
            markers.forEach { m ->
                drawCircle(markerColor, 9f, Offset(sx(m.x), sy(m.y)))
            }

            // Rastreo
            traceX?.let { tx ->
                val px = sx(tx)
                drawLine(traceColor, Offset(px, 0f), Offset(px, h), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
                compiled.forEachIndexed { i, (expr, _) ->
                    val y = expr?.at(tx, ctx) ?: return@forEachIndexed
                    if (y.isFinite()) {
                        drawCircle(Color.White, 10f, Offset(px, sy(y)))
                        drawCircle(curveColors[i], 7f, Offset(px, sy(y)))
                    }
                }
            }
        }

        HelpText("Arrastra para mover, pellizca para zoom y toca la gráfica para leer valores. Puedes escribir sin, sqrt, pi, abs, ln, e^x, |x| como abs(x)…")

        traceX?.let { tx ->
            SectionCard("Rastreo") {
                ResultRow("x", NumberFormatter.format(tx), actions)
                compiled.forEachIndexed { i, (expr, _) ->
                    if (expr != null) {
                        val y = expr.at(tx, ctx)
                        ResultRow("f${"₁₂₃"[i]}(x)", if (y.isFinite()) NumberFormatter.format(y) else "no definida", actions)
                        val d = try { derivative({ expr.at(it, ctx) }, tx) } catch (e: CalcException) { Double.NaN }
                        if (d.isFinite()) ResultRow("f${"₁₂₃"[i]}′(x)", NumberFormatter.format(d), actions)
                    }
                }
            }
        }

        analysisError?.let { ErrorText(it) }
        if (markers.isNotEmpty()) {
            SectionCard("Puntos notables (vista actual)") {
                markers.forEach { m ->
                    ResultRow(m.label, "(${NumberFormatter.format(m.x)}, ${NumberFormatter.format(m.y)})", actions)
                }
            }
        }
    }
}

/** Raíces, máximos/mínimos de cada función e intersecciones f₁∩f₂ dentro de [a, b]. */
private fun analyze(exprs: List<Expression?>, ctx: EvalContext, a: Double, b: Double): List<Marker> {
    val out = mutableListOf<Marker>()
    exprs.forEachIndexed { i, e ->
        if (e == null) return@forEachIndexed
        val name = "f${"₁₂₃"[i]}"
        val f = { x: Double -> e.at(x, ctx) }
        findRoots(f, a, b, 2000).forEach { out += Marker("Raíz $name", it, 0.0) }
        val df = { x: Double -> try { derivative(f, x) } catch (ex: CalcException) { Double.NaN } }
        findRoots(df, a, b, 600).forEach { x ->
            val y = f(x)
            if (!y.isFinite()) return@forEach
            val h = (b - a) * 1e-3
            val kind = if (f(x - h) > y && f(x + h) > y) "Mínimo" else if (f(x - h) < y && f(x + h) < y) "Máximo" else null
            if (kind != null) out += Marker("$kind $name", x, y)
        }
    }
    val present = exprs.withIndex().filter { it.value != null }
    for (p in present.indices) for (q in p + 1 until present.size) {
        val (i, ei) = present[p]
        val (j, ej) = present[q]
        val g = { x: Double -> ei!!.at(x, ctx) - ej!!.at(x, ctx) }
        findRoots(g, a, b, 2000).forEach { x ->
            out += Marker("f${"₁₂₃"[i]} ∩ f${"₁₂₃"[j]}", x, ei!!.at(x, ctx))
        }
    }
    return out.take(40)
}

private fun DrawScope.drawLabel(
    measurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    at: Offset,
    style: TextStyle,
) {
    // Se mide primero y se omite si no cabe: dibujar texto pegado al borde lanza una excepción.
    val layout = measurer.measure(text, style)
    if (at.x < 0 || at.y < 0 || at.x + layout.size.width > size.width || at.y + layout.size.height > size.height) return
    drawText(layout, topLeft = at)
}

private fun niceStep(raw: Double): Double {
    if (raw <= 0 || !raw.isFinite()) return 1.0
    val p = 10.0.pow(floor(log10(raw)))
    val m = raw / p
    return p * when {
        m < 1.5 -> 1.0
        m < 3.5 -> 2.0
        m < 7.5 -> 5.0
        else -> 10.0
    }
}

private fun roundTo(v: Double, step: Double) = Math.round(v / step) * step
