package com.ian.calculadora.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.calculadora.CalcState
import com.ian.calculadora.CalculatorViewModel
import com.ian.calculadora.engine.DisplayMode
import com.ian.calculadora.engine.Names
import com.ian.calculadora.ui.theme.LocalCalcColors
import com.ian.calculadora.ui.theme.Shapes
import com.ian.calculadora.ui.theme.Space
import com.ian.calculadora.ui.theme.TABULAR
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.shadow

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
    // Panel científico plegable (se recuerda entre sesiones).
    var expanded by rememberSaveable { mutableStateOf(vm.loadText("sci_expanded") == "1") }
    val toggleExpanded = {
        expanded = !expanded
        vm.saveText("sci_expanded", if (expanded) "1" else "0")
    }

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
                .padding(horizontal = Space.md, vertical = Space.xs)
        ) {
            TopBar(vm, state, onOpenHistory, onOpenMenu, onOpenScan)
            Display(
                state = state,
                onCopy = copy,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            val sci = scientificKeys(vm, state)
            val basic = basicKeys(vm)
            val memory = memoryKeys(vm, state)
            if (landscape) {
                Row(Modifier.fillMaxWidth().weight(2.2f)) {
                    Column(Modifier.weight(5f).fillMaxHeight()) {
                        KeyRow(memory, Modifier.weight(0.6f), small = true)
                        sci.forEach { KeyRow(it, Modifier.weight(1f), small = true) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(4f).fillMaxHeight()) {
                        basic.forEach { KeyRow(it, Modifier.weight(1f)) }
                    }
                }
            } else {
                ExpandToggle(expanded, state.hasMemory, toggleExpanded)
                if (expanded) {
                    // Desplegado: teclas en forma de píldora para que todo quepa.
                    Column(Modifier.fillMaxWidth().weight(2.4f)) {
                        KeyRow(memory, Modifier.weight(0.7f), small = true)
                        sci.forEach { KeyRow(it, Modifier.weight(0.8f), small = true) }
                        basic.forEach { KeyRow(it, Modifier.weight(1f)) }
                    }
                } else {
                    // Plegado: botones redondos, cada fila tan alta como ancha es cada tecla.
                    basic.forEach { KeyRow(it, Modifier.aspectRatio(4f)) }
                }
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
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        RoundIcon(Icons.Outlined.Menu, "Herramientas", onOpenMenu)
        ModeText(state.angleMode.name) { vm.toggleAngle() }
        if (state.displayMode != DisplayMode.NORMAL) {
            ModeText(state.displayMode.label, highlighted = true) { vm.setDisplayMode(DisplayMode.NORMAL) }
        }
        Spacer(Modifier.weight(1f))
        RoundIcon(Icons.Outlined.PhotoCamera, "Escanear operación", onOpenScan)
        RoundIcon(Icons.Outlined.History, "Historial", onOpenHistory)
    }
}

/** Botón redondo tonal de la barra superior. */
@Composable
private fun RoundIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
    }
}

/** Indicador del modo (DEG, SCI…) en forma de pastilla; cambia al tocarlo. */
@Composable
private fun ModeText(text: String, highlighted: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(Shapes.pill)
            .background(if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun ExpandToggle(expanded: Boolean, hasMemory: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Row(
            Modifier
                .clip(Shapes.pill)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onToggle)
                .padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (expanded) "Ocultar funciones" else "Funciones",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                if (expanded) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                contentDescription = "Funciones científicas",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        if (hasMemory) {
            Text(
                "M",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.clip(Shapes.pill).background(MaterialTheme.colorScheme.primary).padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Buenos días"
        hour < 19 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Display(state: CalcState, onCopy: () -> Unit, modifier: Modifier) {
    val scroll = rememberScrollState()
    LaunchedEffect(state.expression) { scroll.animateScrollTo(scroll.maxValue) }
    val exprSize = when (state.expression.length) {
        in 0..9 -> 60.sp
        in 10..13 -> 48.sp
        in 14..18 -> 38.sp
        else -> 30.sp
    }
    val ghost = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val typo = MaterialTheme.typography
    Column(
        modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onCopy,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        val finished = state.result != null
        if (state.expression.isEmpty() && !finished && state.error == null) {
            Text(
                greeting(),
                style = typo.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(Modifier.weight(1f))
        }
        Text(
            text = buildAnnotatedString {
                append(state.expression.ifEmpty { "0" })
                withStyle(SpanStyle(color = ghost)) { append(")".repeat(state.openParens)) }
            },
            style = typo.displaySmall.copy(fontFeatureSettings = TABULAR),
            fontSize = if (finished) 28.sp else exprSize,
            fontWeight = if (finished) FontWeight.Medium else FontWeight.Normal,
            color = if (finished) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.horizontalScroll(scroll),
        )
        Spacer(Modifier.height(6.dp))
        val (text, color, size) = when {
            state.error != null -> Triple(state.error.orEmpty(), MaterialTheme.colorScheme.error, 22.sp)
            state.result != null -> Triple(
                state.result.orEmpty(),
                MaterialTheme.colorScheme.onSurface,
                if (state.result.orEmpty().length > 12) 44.sp else 68.sp,
            )
            else -> Triple(
                if (state.preview.isEmpty()) "" else "= ${state.preview}",
                MaterialTheme.colorScheme.primary,
                28.sp,
            )
        }
        Text(
            text = text,
            style = typo.displayLarge.copy(fontFeatureSettings = TABULAR),
            color = color,
            fontSize = size,
            fontWeight = if (state.result != null) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
        )
        state.siHint?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 20.sp)
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
            Key("2nd", KeyKind.FUNCTION, active = inv) { vm.toggleSecond() },
            Key("hyp", KeyKind.FUNCTION, active = s.hyp) { vm.toggleHyp() },
            Key(s.displayMode.label.lowercase().replaceFirstChar { it.uppercase() }, KeyKind.FUNCTION, active = s.displayMode != DisplayMode.NORMAL) {
                vm.cycleDisplayMode()
            },
            Key("±", KeyKind.FUNCTION) { vm.toggleSign() },
            Key(",", KeyKind.FUNCTION) { vm.insert(",") },
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
            Key("Ans", KeyKind.FUNCTION) { vm.insert(Names.ANS) },
            Key("mod", KeyKind.FUNCTION) { vm.operator(Names.MOD) },
        ),
        listOf(
            if (inv) Key("⌊x⌋", KeyKind.FUNCTION) { vm.insert("floor(") }
            else Key("x", KeyKind.FUNCTION) { vm.insert("x") },
            if (inv) Key("⌈x⌉", KeyKind.FUNCTION) { vm.insert("ceil(") }
            else Key("∫", KeyKind.FUNCTION) { vm.insert("${Names.INTEGRAL}(") },
            if (inv) Key("gcd", KeyKind.FUNCTION) { vm.insert("gcd(") }
            else Key("d/dx", KeyKind.FUNCTION) { vm.insert("${Names.DERIV}(") },
            if (inv) Key("lcm", KeyKind.FUNCTION) { vm.insert("lcm(") }
            else Key("Σ", KeyKind.FUNCTION) { vm.insert("${Names.SUM}(") },
            Key("Π", KeyKind.FUNCTION) { vm.insert("${Names.PROD}(") },
        ),
    )
}

private fun basicKeys(vm: CalculatorViewModel): List<List<Key>> {
    fun num(d: String) = Key(d, KeyKind.NUMBER) { vm.digit(d) }
    fun op(label: String) = Key(label, KeyKind.OPERATOR) { vm.operator(label) }
    return listOf(
        listOf(
            Key("AC", KeyKind.ACTION) { vm.clear() },
            Key("( )", KeyKind.OPERATOR) { vm.smartParen() },
            Key("%", KeyKind.OPERATOR) { vm.postfix("%") },
            op("÷"),
        ),
        listOf(num("7"), num("8"), num("9"), op("×")),
        listOf(num("4"), num("5"), num("6"), Key("−", KeyKind.OPERATOR) { vm.operator("−") }),
        listOf(num("1"), num("2"), num("3"), op("+")),
        listOf(
            num("0"),
            num("."),
            Key("⌫", KeyKind.NUMBER, icon = Icons.AutoMirrored.Outlined.Backspace, onLongClick = { vm.clear() }) {
                vm.backspace()
            },
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
private fun KeyRow(keys: List<Key>, modifier: Modifier, small: Boolean = false) {
    Row(modifier.fillMaxWidth()) {
        keys.forEach { KeyButton(it, small) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.KeyButton(key: Key, small: Boolean) {
    val view = LocalView.current
    val c = LocalCalcColors.current
    val (bg, fg) = when {
        key.active && key.kind == KeyKind.MEMORY -> Color.Transparent to MaterialTheme.colorScheme.primary
        key.active -> c.equals to c.onEquals
        else -> when (key.kind) {
            KeyKind.NUMBER -> c.number to c.onNumber
            KeyKind.OPERATOR -> c.operator to c.onOperator
            KeyKind.FUNCTION -> c.function to c.onFunction
            KeyKind.ACTION -> c.action to c.onAction
            KeyKind.EQUALS -> c.equals to c.onEquals
            KeyKind.MEMORY -> Color.Transparent to MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    // La tecla se "hunde" un poco al tocarla.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "tecla")
    val fontSize: TextUnit = when {
        key.kind == KeyKind.MEMORY -> 13.sp
        small -> if (key.label.length > 3) 14.sp else 17.sp
        key.label.length > 2 -> 22.sp
        else -> 30.sp
    }
    val shape = Shapes.key
    val raised = c.raisedKeys && key.kind == KeyKind.NUMBER
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(if (small) 3.dp else 6.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (raised) Modifier.shadow(1.5.dp, shape, ambientColor = Color(0x22283A66), spotColor = Color(0x33283A66)) else Modifier)
            .clip(shape)
            .then(
                if (key.kind == KeyKind.EQUALS && !key.active) Modifier.background(c.equalsBrush)
                else Modifier.background(bg)
            )
            .combinedClickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
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
                fontFamily = com.ian.calculadora.ui.theme.Manrope,
                fontWeight = when (key.kind) {
                    KeyKind.ACTION, KeyKind.EQUALS, KeyKind.OPERATOR -> FontWeight.SemiBold
                    else -> FontWeight.Medium
                },
                maxLines = 1,
            )
        }
    }
}

fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(ClipboardManager::class.java)
    cm.setPrimaryClip(ClipData.newPlainText("Resultado", text))
}
