package com.ian.calculadora.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.calculadora.engine.Names
import com.ian.calculadora.ui.theme.LocalCalcColors

/** Texto de la ecuación con cursor; las teclas insertan en la posición del cursor. */
class EquationState(initial: String, private val onChange: (String) -> Unit) {
    var text by mutableStateOf(initial)
        private set
    var cursor by mutableIntStateOf(initial.length)

    private fun set(newText: String, newCursor: Int) {
        text = newText
        cursor = newCursor.coerceIn(0, newText.length)
        onChange(newText)
    }

    /** Inserta [s] y deja el cursor en [cursorAt] (por defecto al final de lo insertado). */
    fun insert(s: String, cursorAt: Int = s.length) {
        val c = cursor.coerceIn(0, text.length)
        set(text.substring(0, c) + s + text.substring(c), c + cursorAt)
    }

    fun replaceAll(s: String) = set(s, s.length)
    fun left() { cursor = (cursor - 1).coerceAtLeast(0) }
    fun right() { cursor = (cursor + 1).coerceAtMost(text.length) }

    fun backspace() {
        val c = cursor.coerceIn(0, text.length)
        if (c == 0) return
        val before = text.substring(0, c)
        val after = text.substring(c)
        // "(|)" vacío: borra los dos paréntesis.
        if (before.endsWith("(") && after.startsWith(")")) {
            val word = Names.words.firstOrNull { before.endsWith("$it(") }
            val cut = (word?.length ?: 0) + 1
            set(before.dropLast(cut) + after.drop(1), c - cut)
            return
        }
        // Borra funciones completas ("sin(", "log₂") de una vez.
        val word = Names.words.firstOrNull { before.endsWith("$it(") }?.let { "$it(" }
            ?: Names.words.firstOrNull { it.length > 1 && before.endsWith(it) }
        val cut = word?.length ?: 1
        set(before.dropLast(cut) + after, c - cut)
    }
}

/** Muestra la ecuación con un cursor; tocar el texto mueve el cursor a ese punto. */
@Composable
fun EquationEditor(state: EquationState, onFocus: () -> Unit, modifier: Modifier = Modifier) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val cursorColor = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(LocalCalcColors.current.display)
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .clickable { state.cursor = state.text.length; onFocus() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (state.text.isEmpty()) {
            Text("Escribe una ecuación, por ejemplo 2x+3=11", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp)
        }
        Text(
            state.text,
            fontSize = 30.sp,
            color = MaterialTheme.colorScheme.onSurface,
            onTextLayout = { layout = it },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(state) {
                    detectTapGestures { pos ->
                        layout?.let { state.cursor = it.getOffsetForPosition(pos) }
                        onFocus()
                    }
                }
                .drawWithContent {
                    drawContent()
                    val l = layout ?: return@drawWithContent
                    val r = l.getCursorRect(state.cursor.coerceIn(0, state.text.length))
                    drawLine(cursorColor, Offset(r.left, r.top), Offset(r.left, r.bottom), strokeWidth = 2.5.dp.toPx())
                },
        )
    }
}

private data class MathKey(val label: String, val insert: String, val cursorAt: Int = insert.length)

private val tabs = listOf("+ −\n× ÷", "f(x) e\nlog ln", "sin cos\ntan", "∫ Σ\nd/dx")

private val tabKeys: List<List<MathKey>> = listOf(
    listOf(
        MathKey("( )", "()", 1), MathKey("x", "x"),
        MathKey("□⁄□", "()/()", 1), MathKey("√□", "√()", 2),
        MathKey("□²", "^2"), MathKey("□ʸ", "^()", 2),
        MathKey("π", "π"), MathKey("%", "%"),
    ),
    listOf(
        MathKey("e", "e"), MathKey("eˣ", "e^()", 3),
        MathKey("ln", "ln()", 3), MathKey("log", "log()", 4),
        MathKey("log₂", "log₂()", 5), MathKey("|x|", "abs()", 4),
        MathKey("∛□", "∛()", 2), MathKey("n!", "!"),
    ),
    listOf(
        MathKey("sin", "sin()", 4), MathKey("cos", "cos()", 4),
        MathKey("tan", "tan()", 4), MathKey("sin⁻¹", "sin⁻¹()", 6),
        MathKey("cos⁻¹", "cos⁻¹()", 6), MathKey("tan⁻¹", "tan⁻¹()", 6),
        MathKey("π", "π"), MathKey("x", "x"),
    ),
    listOf(
        MathKey("∫", "∫(,,)", 2), MathKey("d/dx", "d/dx(,)", 5),
        MathKey("Σ", "Σ(,,)", 2), MathKey("Π", "Π(,,)", 2),
        MathKey(",", ","), MathKey("x", "x"),
        MathKey("e", "e"), MathKey("^", "^"),
    ),
)

private val numberRows = listOf(
    listOf("7", "8", "9", "÷"),
    listOf("4", "5", "6", "×"),
    listOf("1", "2", "3", "−"),
    listOf("0", ".", "=", "+"),
)

/** Teclado matemático: pestañas de funciones a la izquierda y números/operaciones a la derecha. */
@Composable
fun MathKeyboard(state: EquationState, onSolve: () -> Unit, modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val colors = MaterialTheme.colorScheme
    val calc = LocalCalcColors.current
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        // Fila de edición
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { state.left() }, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Mover cursor a la izquierda")
            }
            IconButton(onClick = { state.right() }, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Mover cursor a la derecha")
            }
            IconButton(onClick = onSolve, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardReturn, "Resolver", tint = colors.primary)
            }
            BackspaceButton(state, Modifier.weight(1f))
        }
        // Pestañas
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tabs.forEachIndexed { i, label ->
                val selected = i == tab
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) colors.primary else colors.surface)
                        .border(1.dp, if (selected) colors.primary else colors.outlineVariant, RoundedCornerShape(50))
                        .clickable { tab = i },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        // Teclas: 2 columnas de la pestaña + 4 columnas de números
        val left = tabKeys[tab]
        numberRows.forEachIndexed { row, numbers ->
            Row(Modifier.fillMaxWidth().height(54.dp)) {
                listOf(left[row * 2], left[row * 2 + 1]).forEach { k ->
                    MathKeyCell(k.label, calc.function, calc.onFunction) { state.insert(k.insert, k.cursorAt) }
                }
                numbers.forEach { n ->
                    val isOp = n in "÷×−+="
                    MathKeyCell(
                        n,
                        if (isOp) calc.operator else calc.number,
                        if (isOp) calc.onOperator else calc.onNumber,
                        big = true,
                    ) { state.insert(n) }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BackspaceButton(state: EquationState, modifier: Modifier) {
    val view = LocalView.current
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .combinedClickable(
                onClick = { state.backspace() },
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    state.replaceAll("")
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Outlined.Backspace, "Borrar (mantén para borrar todo)")
    }
}

@Composable
private fun RowScope.MathKeyCell(label: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, big: Boolean = false, onClick: () -> Unit) {
    val view = LocalView.current
    Box(
        Modifier
            .weight(1f)
            .padding(3.dp)
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontSize = if (big) 24.sp else 17.sp, maxLines = 1)
    }
}
