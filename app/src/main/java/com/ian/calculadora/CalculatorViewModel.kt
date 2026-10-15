package com.ian.calculadora

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.ian.calculadora.data.HistoryItem
import com.ian.calculadora.data.HistoryRepository
import com.ian.calculadora.engine.AngleMode
import com.ian.calculadora.engine.CalcException
import com.ian.calculadora.engine.DisplayMode
import com.ian.calculadora.engine.Evaluator
import com.ian.calculadora.engine.Names
import com.ian.calculadora.engine.NumberFormatter

data class CalcState(
    val expression: String = "",
    /** Resultado provisional mientras se escribe. */
    val preview: String = "",
    /** Resultado final tras pulsar "=". */
    val result: String? = null,
    val error: String? = null,
    val angleMode: AngleMode = AngleMode.DEG,
    val displayMode: DisplayMode = DisplayMode.NORMAL,
    /** Resultado con prefijo SI (4.7 k, 220 µ) en modo ENG. */
    val siHint: String? = null,
    val second: Boolean = false,
    val hyp: Boolean = false,
    val memory: Double = 0.0,
    val history: List<HistoryItem> = emptyList(),
    val message: String? = null,
) {
    val hasMemory get() = memory != 0.0
    val openParens get() = Evaluator.openParens(expression)
}

class CalculatorViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = HistoryRepository(app)
    private var ans = repo.ans
    /** Tras pulsar "=", escribir un número empieza de cero y un operador continúa desde el resultado. */
    private var justEvaluated = false

    var state by mutableStateOf(
        CalcState(
            angleMode = repo.angleMode,
            displayMode = repo.displayMode,
            memory = repo.memory,
            history = repo.loadHistory(),
        )
    )
        private set

    // ---------- Entrada ----------

    fun digit(d: String) {
        var expr = startFresh()
        if (d == ".") {
            val number = expr.takeLastWhile { it.isDigit() || it == '.' }
            if (number.contains('.')) return
            if (number.isEmpty()) expr += "0"
        } else if (d == "E") {
            if (expr.isEmpty() || !expr.last().isDigit()) return
            if (expr.takeLastWhile { it.isDigit() || it == '.' || it == 'E' }.contains('E')) return
        }
        setExpression(expr + d)
    }

    /** Operadores binarios: + − × ÷ ^ mod y la coma de argumentos. */
    fun operator(op: String) {
        var expr = continueFromResult()
        val binary = listOf("+", "×", "÷", "^", Names.MOD)
        if (expr.isEmpty() && op in binary) expr = Names.ANS
        val last = expr.lastOrNull()
        if (last != null && op in binary && last in "+−×÷^") {
            // Reemplaza el operador anterior (excepto "×−" para negativos).
            expr = expr.dropLast(1)
        }
        setExpression(expr + if (op == Names.MOD) " mod " else op)
    }

    /** Postfijos y potencias rápidas: "!", "%", "^2", "^3", "^(−1)". */
    fun postfix(p: String) {
        var expr = continueFromResult()
        if (expr.isEmpty()) expr = Names.ANS
        setExpression(expr + p)
    }

    /** Funciones (insertan "sin(" etc.) y constantes (π, e, Ans, Rand) o texto libre como "(" ")". */
    fun insert(text: String) {
        setExpression(startFresh() + text)
    }

    /** Un solo botón "( )": cierra un paréntesis si hay alguno abierto y tiene sentido, si no abre uno. */
    fun smartParen() {
        val expr = if (justEvaluated) "" else state.expression
        val last = expr.trimEnd().lastOrNull()
        val closes = Evaluator.openParens(expr) > 0 && last != null &&
            (last.isDigit() || last in ").!%πex" || expr.endsWith(Names.ANS) || expr.endsWith(Names.RAND))
        insert(if (closes) ")" else "(")
    }

    fun toggleSign() {
        if (justEvaluated) {
            justEvaluated = false
            val negated = NumberFormatter.format(-ans)
            setExpression(if (negated.startsWith("−")) "(${negated}" else negated)
            return
        }
        val expr = state.expression
        val number = expr.takeLastWhile { it.isDigit() || it == '.' }
        val head = expr.dropLast(number.length)
        val newExpr = when {
            head.endsWith("(−") -> head.dropLast(2) + number
            else -> "$head(−$number"
        }
        setExpression(newExpr)
    }

    fun backspace() {
        if (justEvaluated) {
            justEvaluated = false
            update(state.copy(result = null, siHint = null))
            return
        }
        val expr = state.expression
        if (expr.isEmpty()) return
        val word = Names.words.firstOrNull { expr.endsWith("$it(") }?.let { "$it(" }
            ?: Names.words.firstOrNull { expr.endsWith(it) }
            ?: if (expr.endsWith(" mod ")) " mod " else null
        setExpression(expr.dropLast(word?.length ?: 1))
    }

    fun clear() {
        justEvaluated = false
        update(state.copy(expression = "", preview = "", result = null, siHint = null, error = null))
    }

    fun equals() {
        val expr = state.expression
        if (expr.isBlank() || justEvaluated) return
        try {
            val value = Evaluator.evaluate(expr, state.angleMode, ans)
            val raw = NumberFormatter.format(value)
            ans = value
            repo.ans = value
            justEvaluated = true
            val closed = expr + ")".repeat(Evaluator.openParens(expr))
            val item = HistoryItem(
                id = System.nanoTime(),
                expression = closed,
                result = raw,
                value = value,
                timestamp = System.currentTimeMillis(),
            )
            val history = (listOf(item) + state.history).take(HistoryRepository.MAX_ITEMS)
            repo.saveHistory(history)
            update(
                state.copy(
                    expression = closed,
                    result = NumberFormatter.formatGrouped(value, state.displayMode),
                    siHint = siHint(value),
                    preview = "",
                    error = null,
                    history = history,
                )
            )
        } catch (e: CalcException) {
            update(state.copy(error = e.message))
        }
    }

    // ---------- Modos ----------

    fun toggleAngle() {
        val mode = if (state.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
        repo.angleMode = mode
        update(state.copy(angleMode = mode), recompute = true)
    }

    fun cycleDisplayMode() {
        val mode = DisplayMode.entries[(state.displayMode.ordinal + 1) % DisplayMode.entries.size]
        repo.displayMode = mode
        val newState = state.copy(displayMode = mode)
        state = if (justEvaluated) {
            newState.copy(result = NumberFormatter.formatGrouped(ans, mode), siHint = siHint(ans, mode))
        } else {
            newState.copy(preview = preview(newState))
        }
    }

    private fun siHint(value: Double, mode: DisplayMode = state.displayMode) =
        if (mode == DisplayMode.ENG) NumberFormatter.siPrefix(value) else null

    fun toggleSecond() = update(state.copy(second = !state.second))
    fun toggleHyp() = update(state.copy(hyp = !state.hyp))

    // ---------- Memoria ----------

    fun memoryClear() = setMemory(0.0, "Memoria borrada")
    fun memoryRecall() {
        if (!state.hasMemory) return
        insert(asOperand(NumberFormatter.format(state.memory)))
    }
    fun memoryAdd() = currentValue()?.let { setMemory(state.memory + it, "M+ ${NumberFormatter.format(it)}") }
    fun memorySubtract() = currentValue()?.let { setMemory(state.memory - it, "M− ${NumberFormatter.format(it)}") }
    fun memoryStore() = currentValue()?.let { setMemory(it, "Guardado en memoria") }

    private fun setMemory(value: Double, msg: String) {
        repo.memory = value
        update(state.copy(memory = value, message = msg))
    }

    private fun currentValue(): Double? {
        if (justEvaluated) return ans
        if (state.expression.isBlank()) return null
        return try {
            Evaluator.evaluate(state.expression, state.angleMode, ans)
        } catch (e: CalcException) {
            update(state.copy(error = e.message))
            null
        }
    }

    // ---------- Historial ----------

    fun useHistoryExpression(item: HistoryItem) {
        justEvaluated = false
        setExpression(item.expression)
    }

    fun useHistoryResult(item: HistoryItem) {
        insert(asOperand(item.result))
    }

    /** Inserta un número (de constantes, herramientas...) como operando. */
    fun insertValue(value: Double) {
        insert(asOperand(NumberFormatter.format(value)))
    }

    fun loadText(key: String, default: String = "") = repo.getText(key, default)
    fun saveText(key: String, value: String) = repo.putText(key, value)

    /** Reemplaza la expresión (por ejemplo, desde otra herramienta). */
    fun setExpressionFrom(text: String) {
        justEvaluated = false
        setExpression(text)
    }

    fun deleteHistoryItem(item: HistoryItem) {
        val history = state.history.filterNot { it.id == item.id }
        repo.saveHistory(history)
        update(state.copy(history = history))
    }

    fun clearHistory() {
        repo.saveHistory(emptyList())
        update(state.copy(history = emptyList(), message = "Historial borrado"))
    }

    fun showMessage(msg: String) = update(state.copy(message = msg))
    fun messageShown() = update(state.copy(message = null))

    /** Texto del resultado actual (o vista previa) para copiar. */
    fun copyableResult(): String? = when {
        justEvaluated -> NumberFormatter.format(ans, state.displayMode)
        state.preview.isNotEmpty() -> state.preview.replace(",", "")
        else -> null
    }

    // ---------- Internos ----------

    private fun asOperand(raw: String) = if (raw.startsWith("−")) "($raw)" else raw

    private fun startFresh(): String {
        if (justEvaluated) {
            justEvaluated = false
            return ""
        }
        return state.expression
    }

    private fun continueFromResult(): String {
        if (justEvaluated) {
            justEvaluated = false
            return asOperand(NumberFormatter.format(ans))
        }
        return state.expression
    }

    private fun setExpression(expr: String) {
        update(state.copy(expression = expr, result = null, siHint = null, error = null), recompute = true)
    }

    private fun update(newState: CalcState, recompute: Boolean = false) {
        state = if (recompute && !justEvaluated) newState.copy(preview = preview(newState)) else newState
    }

    private fun preview(s: CalcState): String {
        val expr = s.expression
        // Sin vista previa si es solo un número o si usa Rand (cambiaría en cada tecla).
        if (expr.isBlank() || expr.all { it.isDigit() || it == '.' } || expr.contains(Names.RAND)) return ""
        return try {
            NumberFormatter.formatGrouped(Evaluator.evaluate(expr, s.angleMode, ans), s.displayMode)
        } catch (e: CalcException) {
            ""
        }
    }
}
