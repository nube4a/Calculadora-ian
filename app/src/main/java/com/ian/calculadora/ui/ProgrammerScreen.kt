package com.ian.calculadora.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.calculadora.CalculatorViewModel
import com.ian.calculadora.engine.Base
import com.ian.calculadora.engine.BitOp
import com.ian.calculadora.engine.CalcException
import com.ian.calculadora.engine.Programmer

@Composable
fun ProgrammerScreen(vm: CalculatorViewModel, onBack: () -> Unit) {
    var base by rememberSaveable { mutableStateOf(Base.DEC) }
    var op by rememberSaveable { mutableStateOf(BitOp.AND) }
    val a = rememberPersisted(vm, "prog_a", "202")
    val b = rememberPersisted(vm, "prog_b", "15")

    fun parse(text: String, name: String): Result<Long> = runCatching {
        try {
            Programmer.parse(text, base)
        } catch (e: CalcException) {
            throw CalcException("$name: ${e.message}")
        } catch (e: NumberFormatException) {
            throw CalcException("$name: número inválido")
        }
    }

    ToolScaffold("Programador", onBack) { actions ->
        HelpText("Enteros de 64 bits en complemento a dos. Cambiar la base convierte los valores escritos.")
        ChoiceChips(Base.entries, base, { it.label }) { newBase ->
            // Convierte lo escrito a la nueva base para no perderlo.
            parse(a.value, "A").onSuccess { a.value = Programmer.format(it, newBase).replace(" ", "") }
            parse(b.value, "B").onSuccess { b.value = Programmer.format(it, newBase).replace(" ", "") }
            base = newBase
        }
        val pa = parse(a.value, "A")
        val pb = parse(b.value, "B")
        ExprField(
            a.value, { a.value = it.uppercase() }, "A (${base.label})",
            error = pa.exceptionOrNull()?.message.takeIf { a.value.isNotBlank() },
        )
        ChoiceChips(BitOp.entries, op, { it.label }) { op = it }
        if (!op.unary) {
            ExprField(
                b.value, { b.value = it.uppercase() }, "B (${base.label})",
                error = pb.exceptionOrNull()?.message.takeIf { b.value.isNotBlank() },
            )
        }

        val result: Result<Long>? = pa.getOrNull()?.let { va ->
            if (op.unary) runCatching { Programmer.apply(op, va, 0) }
            else pb.getOrNull()?.let { vb -> runCatching { Programmer.apply(op, va, vb) } }
        }

        result?.exceptionOrNull()?.let { ErrorText(it.message ?: "Error") }
        result?.getOrNull()?.let { r ->
            SectionCard(if (op.unary) "${op.label} A" else "A ${op.label} B") {
                Base.entries.forEach { bs ->
                    ResultRow(bs.label, Programmer.format(r, bs), actions, highlight = bs == base)
                }
                ResultRow("Bits en 1", Programmer.popCount(r).toString(), actions)
                ResultRow("Sin signo", java.lang.Long.toUnsignedString(r), actions)
            }
            SectionCard("Bits (63 … 0)") {
                val bits = java.lang.Long.toBinaryString(r).padStart(64, '0')
                bits.chunked(16).forEachIndexed { row, chunk ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${63 - row * 16}".padStart(2),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text(
                            chunk.chunked(4).joinToString(" "),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }

        pa.getOrNull()?.let { va ->
            SectionCard("A en todas las bases") {
                Base.entries.forEach { bs -> ResultRow(bs.label, Programmer.format(va, bs), actions) }
            }
        }
    }
}
