package com.ian.calculadora.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ian.calculadora.CalculatorViewModel
import com.ian.calculadora.engine.NumberFormatter
import com.ian.calculadora.engine.PhysicalConstants

@Composable
fun ConstantsScreen(vm: CalculatorViewModel, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    ToolScaffold("Constantes físicas", onBack) { actions ->
        ExprField(query, { query = it }, "Buscar")
        HelpText("Toca una constante para insertarla en la calculadora. Valores CODATA 2018.")
        val list = PhysicalConstants.all.filter {
            query.isBlank() || it.name.contains(query, true) || it.symbol.contains(query, true)
        }
        SectionCard {
            list.forEachIndexed { i, c ->
                if (i > 0) HorizontalDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            vm.insertValue(c.value)
                            vm.showMessage("${c.symbol} insertada")
                            onBack()
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        c.symbol,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(56.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${NumberFormatter.format(c.value)} ${c.unit}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    IconButton(onClick = { actions.copy(NumberFormatter.format(c.value)) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar")
                    }
                }
            }
        }
    }
}
