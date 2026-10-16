package com.ian.calculadora.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ian.calculadora.CalculatorViewModel
import com.ian.calculadora.engine.CalcException
import com.ian.calculadora.engine.EvalContext
import com.ian.calculadora.engine.Evaluator
import com.ian.calculadora.engine.NumberFormatter
import com.ian.calculadora.engine.Units

@Composable
fun UnitsScreen(vm: CalculatorViewModel, onBack: () -> Unit) {
    var categoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var fromIndex by rememberSaveable { mutableIntStateOf(0) }
    val input = rememberPersisted(vm, "units_value", "1")
    val category = Units.categories[categoryIndex]
    val from = category.units[fromIndex.coerceIn(0, category.units.lastIndex)]

    val parsed: Result<Double> = runCatching {
        Evaluator.compile(input.value).evaluate(EvalContext(vm.state.angleMode))
    }

    ToolScaffold("Conversor de unidades", onBack) { actions ->
        ChoiceChips(Units.categories.indices.toList(), categoryIndex, { Units.categories[it].name }) {
            categoryIndex = it
            fromIndex = 0
        }
        ExprField(
            input.value,
            { input.value = it },
            "Valor en ${from.symbol} (${from.name})",
            error = parsed.exceptionOrNull()?.let { (it as? CalcException)?.message ?: "Valor inválido" }
                .takeIf { input.value.isNotBlank() },
        )
        HelpText("Elige la unidad de origen (puedes escribir operaciones, por ejemplo 3/8 o 2^10):")
        ChoiceChips(category.units.indices.toList(), fromIndex, { category.units[it].symbol }) { fromIndex = it }

        val value = parsed.getOrNull()
        if (value != null) {
            SectionCard("Equivalencias") {
                category.units.forEachIndexed { i, unit ->
                    if (i == fromIndex) return@forEachIndexed
                    val converted = Units.convert(value, from, unit)
                    val text = NumberFormatter.formatGrouped(converted)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Tocar una fila la vuelve la unidad de origen con su valor.
                                input.value = NumberFormatter.format(converted)
                                fromIndex = i
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                            Text(
                                text,
                                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = com.ian.calculadora.ui.theme.TABULAR),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${unit.symbol} · ${unit.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { actions.copy(NumberFormatter.format(converted)) }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar")
                        }
                    }
                }
            }
        }
    }
}
