package com.ian.calculadora.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.calculadora.CalcState
import com.ian.calculadora.CalculatorViewModel
import com.ian.calculadora.engine.AngleMode
import com.ian.calculadora.engine.Names

private enum class KeyKind { NUMBER, OPERATOR, FUNCTION, ACTION, EQUALS, MEMORY }

private data class Key(
    val label: String,
    val kind: KeyKind,
    val active: Boolean = false,
    val icon: ImageVector? = null,
    val onLongClick: (() -> Unit)? = null,
    val onClick: () -> Unit,
)

@Composable
fun CalculatorScreen(
    vm: CalculatorViewModel,
    onOpenHistory: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenScan: () -> Unit,
) {
    val state = vm.state
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(state.message) {
        state.message?.let {
            vm.messageShown()
            snackbar.showSnackbar(it)
        }
    }

    val copy = {
        vm.copyableResult()?.let {
            copyToClipboard(context, it)
            vm.showMessage("Copiado: $it")
        }
        Unit
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            TopBar(vm, state, onOpenHistory, onOpenMenu, onOpenScan, onCopy = copy)
            Display(
                state = state,
                onCopy = copy,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (landscape) 0.8f else 1f),
            )
            val sci = scientificKeys(vm, state)
            val basic = basicKeys(vm)
            val memory = memoryKeys(vm, state)
            if (landscape) {
                Row(Modifier.fillMaxWidth().weight(2.2f)) {
                    Column(Modifier.weight(5f).fillMaxHeight()) {
                        KeyRow(memory, Modifier.weight(0.7f))
                        KeyGrid(sci, Modifier.weight(5f), compact = true)
                    }
                    Spacer(Modifier.width(8.dp))
                    KeyGrid(basic, Modifier.weight(4f).fillMaxHeight())
                }
            } else {
                KeyRow(memory, Modifier.weight(0.38f))
                KeyGrid(sci, Modifier.weight(2.6f), compact = true)
                KeyGrid(basic, Modifier.weight(3.4f))
            }
        }
    }
}

@Composable
private fun TopBar(
    vm: CalculatorViewModel,
    state: CalcState,
    onOpenHistory: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenScan: () -> Unit,
    onCopy: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(onClick = onOpenMenu) {
            Icon(Icons.Outlined.Menu, contentDescription = "Herramientas")
        }
        AssistChip(onClick = { vm.toggleAngle() }, label = { Text(state.angleMode.name) })
        AssistChip(onClick = { vm.cycleDisplayMode() }, label = { Text(state.displayMode.label) })
        if (state.hasMemory) Text("M", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onOpenScan) {
            Icon(Icons.Outlined.PhotoCamera, contentDescription = "Escanear operación")
        }
        IconButton(onClick = onOpenHistory) {
            Icon(Icons.Outlined.History, contentDescription = "Historial")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Display(state: CalcState, onCopy: () -> Unit, modifier: Modifier) {
    val scroll = rememberScrollState()
    LaunchedEffect(state.expression) { scroll.animateScrollTo(scroll.maxValue) }
    val exprSize = when (state.expression.length) {
        in 0..10 -> 44.sp
        in 11..16 -> 36.sp
        in 17..24 -> 30.sp
        else -> 26.sp
    }
    val ghost = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = {}, onLongClick = onCopy)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        val finished = state.result != null
        Text(
            text = buildAnnotatedString {
                append(state.expression.ifEmpty { "0" })
                withStyle(SpanStyle(color = ghost)) { append(")".repeat(state.openParens)) }
            },
            fontSize = if (finished) 24.sp else exprSize,
            color = if (finished) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.horizontalScroll(scroll),
        )
        Spacer(Modifier.padding(top = 4.dp))
        val (text, color, size) = when {
            state.error != null -> Triple(state.error.orEmpty(), MaterialTheme.colorScheme.error, 22.sp)
            state.result != null -> Triple(
                "= ${state.result}",
                MaterialTheme.colorScheme.primary,
                if (state.result.orEmpty().length > 14) 34.sp else 48.sp,
            )
            else -> Triple(state.preview, MaterialTheme.colorScheme.onSurfaceVariant, 26.sp)
        }
        Text(
            text = text,
            color = color,
            fontSize = size,
            fontWeight = if (state.result != null) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
        )
        state.siHint?.let {
            Text("≈ $it", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp)
        }
    }
}

// ---------- Definición de teclas ----------

private fun scientificKeys(vm: CalculatorViewModel, s: CalcState): List<List<Key>> {
    val inv = s.second
    val h = if (s.hyp) "h" else ""
    fun trig(base: String) = base + h + if (inv) "⁻¹" else ""
    return listOf(
        listOf(
            Key("2nd", KeyKind.ACTION, active = inv) { vm.toggleSecond() },
            Key("hyp", KeyKind.ACTION, active = s.hyp) { vm.toggleHyp() },
            Key(if (s.angleMode == AngleMode.DEG) "DEG" else "RAD", KeyKind.ACTION) { vm.toggleAngle() },
            Key("(", KeyKind.FUNCTION) { vm.insert("(") },
            Key(")", KeyKind.FUNCTION) { vm.insert(")") },
        ),
        listOf(
            trig("sin").let { Key(it, KeyKind.FUNCTION) { vm.insert("$it(") } },
            trig("cos").let { Key(it, KeyKind.FUNCTION) { vm.insert("$it(") } },
            trig("tan").let { Key(it, KeyKind.FUNCTION) { vm.insert("$it(") } },
            if (inv) Key("eˣ", KeyKind.FUNCTION) { vm.insert("e^(") }
            else Key("ln", KeyKind.FUNCTION) { vm.insert("ln(") },
            if (inv) Key("10ˣ", KeyKind.FUNCTION) { vm.insert("10^(") }
            else Key("log", KeyKind.FUNCTION) { vm.insert("log(") },
        ),
        listOf(
            if (inv) Key("x³", KeyKind.FUNCTION) { vm.postfix("^3") }
            else Key("x²", KeyKind.FUNCTION) { vm.postfix("^2") },
            if (inv) Key("ʸ√x", KeyKind.FUNCTION) { vm.insert("${Names.ROOT}(") }
            else Key("xʸ", KeyKind.FUNCTION) { vm.operator("^") },
            if (inv) Key("∛", KeyKind.FUNCTION) { vm.insert("${Names.CBRT}(") }
            else Key("√", KeyKind.FUNCTION) { vm.insert("${Names.SQRT}(") },
            if (inv) Key("|x|", KeyKind.FUNCTION) { vm.insert("${Names.ABS}(") }
            else Key("1/x", KeyKind.FUNCTION) { vm.postfix("^(−1)") },
            if (inv) Key("log₂", KeyKind.FUNCTION) { vm.insert("${Names.LOG2}(") }
            else Key("n!", KeyKind.FUNCTION) { vm.postfix("!") },
        ),
        listOf(
            if (inv) Key("Rand", KeyKind.FUNCTION) { vm.insert(Names.RAND) }
            else Key("π", KeyKind.FUNCTION) { vm.insert("π") },
            if (inv) Key("nCr", KeyKind.FUNCTION) { vm.insert("${Names.NCR}(") }
            else Key("e", KeyKind.FUNCTION) { vm.insert("e") },
            if (inv) Key("nPr", KeyKind.FUNCTION) { vm.insert("${Names.NPR}(") }
            else Key("EXP", KeyKind.FUNCTION) { vm.digit("E") },
            if (inv) Key(",", KeyKind.FUNCTION) { vm.insert(",") }
            else Key("Ans", KeyKind.FUNCTION) { vm.insert(Names.ANS) },
            Key("mod", KeyKind.FUNCTION) { vm.operator(Names.MOD) },
        ),
        listOf(
            if (inv) Key("Π", KeyKind.FUNCTION) { vm.insert("${Names.PROD}(") }
            else Key("x", KeyKind.FUNCTION) { vm.insert("x") },
            if (inv) Key("⌊x⌋", KeyKind.FUNCTION) { vm.insert("floor(") }
            else Key("∫", KeyKind.FUNCTION) { vm.insert("${Names.INTEGRAL}(") },
            if (inv) Key("⌈x⌉", KeyKind.FUNCTION) { vm.insert("ceil(") }
            else Key("d/dx", KeyKind.FUNCTION) { vm.insert("${Names.DERIV}(") },
            if (inv) Key("gcd", KeyKind.FUNCTION) { vm.insert("gcd(") }
            else Key("Σ", KeyKind.FUNCTION) { vm.insert("${Names.SUM}(") },
            if (inv) Key("lcm", KeyKind.FUNCTION) { vm.insert("lcm(") }
            else Key(",", KeyKind.FUNCTION) { vm.insert(",") },
        ),
    )
}

private fun basicKeys(vm: CalculatorViewModel): List<List<Key>> {
    fun num(d: String) = Key(d, KeyKind.NUMBER) { vm.digit(d) }
    fun op(label: String) = Key(label, KeyKind.OPERATOR) { vm.operator(label) }
    return listOf(
        listOf(
            Key("AC", KeyKind.ACTION) { vm.clear() },
            Key("⌫", KeyKind.ACTION, icon = Icons.AutoMirrored.Outlined.Backspace, onLongClick = { vm.clear() }) {
                vm.backspace()
            },
            Key("%", KeyKind.OPERATOR) { vm.postfix("%") },
            op("÷"),
        ),
        listOf(num("7"), num("8"), num("9"), op("×")),
        listOf(num("4"), num("5"), num("6"), Key("−", KeyKind.OPERATOR) { vm.operator("−") }),
        listOf(num("1"), num("2"), num("3"), op("+")),
        listOf(
            Key("±", KeyKind.NUMBER) { vm.toggleSign() },
            num("0"),
            num("."),
            Key("=", KeyKind.EQUALS) { vm.equals() },
        ),
    )
}

private fun memoryKeys(vm: CalculatorViewModel, s: CalcState) = listOf(
    Key("MC", KeyKind.MEMORY) { vm.memoryClear() },
    Key("MR", KeyKind.MEMORY, active = s.hasMemory) { vm.memoryRecall() },
    Key("M+", KeyKind.MEMORY) { vm.memoryAdd() },
    Key("M−", KeyKind.MEMORY) { vm.memorySubtract() },
    Key("MS", KeyKind.MEMORY) { vm.memoryStore() },
)

// ---------- Componentes de teclado ----------

@Composable
private fun KeyGrid(rows: List<List<Key>>, modifier: Modifier, compact: Boolean = false) {
    Column(modifier) {
        rows.forEach { KeyRow(it, Modifier.weight(1f), compact) }
    }
}

@Composable
private fun KeyRow(keys: List<Key>, modifier: Modifier, compact: Boolean = false) {
    Row(modifier.fillMaxWidth()) {
        keys.forEach { KeyButton(it, compact) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.KeyButton(key: Key, compact: Boolean) {
    val view = LocalView.current
    val colors = MaterialTheme.colorScheme
    val (bg, fg) = when {
        key.active -> colors.primary to colors.onPrimary
        else -> when (key.kind) {
            KeyKind.NUMBER -> colors.secondaryContainer to colors.onSecondaryContainer
            KeyKind.OPERATOR -> colors.primaryContainer to colors.onPrimaryContainer
            KeyKind.FUNCTION -> colors.tertiaryContainer to colors.onTertiaryContainer
            KeyKind.ACTION -> colors.surfaceVariant to colors.primary
            KeyKind.EQUALS -> colors.primary to colors.onPrimary
            KeyKind.MEMORY -> Color.Transparent to colors.onSurfaceVariant
        }
    }
    val fontSize = when {
        key.kind == KeyKind.MEMORY -> 14.sp
        compact -> if (key.label.length > 3) 14.sp else 17.sp
        key.kind == KeyKind.NUMBER || key.kind == KeyKind.OPERATOR || key.kind == KeyKind.EQUALS -> 28.sp
        else -> 22.sp
    }
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(if (compact || key.kind == KeyKind.MEMORY) 3.dp else 4.dp)
            .clip(RoundedCornerShape(if (compact) 14.dp else 22.dp))
            .background(bg)
            .combinedClickable(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    key.onClick()
                },
                onLongClick = key.onLongClick?.let { long ->
                    {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        long()
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (key.icon != null) {
            Icon(key.icon, contentDescription = "Borrar", tint = fg)
        } else {
            Text(
                key.label,
                color = fg,
                fontSize = fontSize,
                fontWeight = if (key.kind == KeyKind.FUNCTION) FontWeight.Normal else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(ClipboardManager::class.java)
    cm.setPrimaryClip(ClipData.newPlainText("Resultado", text))
}
