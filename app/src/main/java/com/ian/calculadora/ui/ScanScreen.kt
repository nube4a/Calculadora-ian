package com.ian.calculadora.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ian.calculadora.CalculatorViewModel
import com.ian.calculadora.engine.CalcException
import com.ian.calculadora.engine.MathText
import com.ian.calculadora.engine.StepSolution
import com.ian.calculadora.engine.StepSolver
import java.io.File

@Composable
fun ScanScreen(vm: CalculatorViewModel, onBack: () -> Unit, onGraph: (String) -> Unit) {
    val context = LocalContext.current
    val equation = rememberPersisted(vm, "scan_eq", "14√x+15=71")
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var solution by remember { mutableStateOf<StepSolution?>(null) }

    // El reconocedor se crea solo al escanear por primera vez (carga el modelo de ML Kit).
    val recognizerHolder = remember { lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) } }
    DisposableEffect(Unit) { onDispose { if (recognizerHolder.isInitialized()) recognizerHolder.value.close() } }

    fun solve() {
        error = null
        solution = try {
            StepSolver.solve(equation.value, vm.state.angleMode)
        } catch (e: CalcException) {
            error = e.message
            null
        }
    }

    fun recognize(uri: Uri) {
        busy = true
        error = null
        solution = null
        preview = loadPreview(context, uri)
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            busy = false
            error = "No se pudo abrir la imagen"
            return
        }
        recognizerHolder.value.process(image)
            .addOnSuccessListener { text ->
                val found = text.textBlocks.flatMap { it.lines }
                    .map { MathText.normalizeOcr(it.text) }
                    .filter { line -> line.any { it.isDigit() || it == 'x' } }
                    .distinct()
                lines = found
                if (found.isEmpty()) {
                    error = "No se reconoció ninguna operación. Prueba con más luz, enfoca de cerca o escríbela abajo."
                } else {
                    // Preferimos la línea que parece una ecuación.
                    equation.value = found.firstOrNull { '=' in it } ?: found.first()
                    solve()
                }
            }
            .addOnFailureListener { error = "Error al reconocer: ${it.message}" }
            .addOnCompleteListener { busy = false }
    }

    val photoUri = remember {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(dir, "escaneo.jpg"))
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) recognize(photoUri)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) recognize(uri)
    }

    ToolScaffold("Escanear y resolver", onBack) { actions ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    try {
                        takePicture.launch(photoUri)
                    } catch (e: ActivityNotFoundException) {
                        error = "No hay una app de cámara disponible"
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Tomar foto")
            }
            FilledTonalButton(
                onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Galería")
            }
        }
        HelpText("Encuadra solo la operación, con buena luz. Funciona mejor con texto impreso o letra clara; siempre puedes corregir lo reconocido.")

        preview?.let {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    it,
                    contentDescription = "Foto escaneada",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                if (busy) CircularProgressIndicator()
            }
        }

        if (lines.size > 1) {
            HelpText("Texto detectado — toca la línea correcta:")
            lines.take(8).forEach { line ->
                AssistChip(
                    onClick = { equation.value = line; solve() },
                    label = { Text(line, fontFamily = FontFamily.Monospace) },
                )
            }
        }

        ExprField(
            equation.value,
            { equation.value = it; solution = null },
            "Ecuación o expresión (puedes corregirla)",
        )
        Button(onClick = { solve() }, modifier = Modifier.fillMaxWidth()) { Text("Resolver paso a paso") }
        error?.let { ErrorText(it) }

        solution?.let { sol ->
            SectionCard("Solución · ${sol.method}") {
                sol.solutions.forEach { s ->
                    Text(
                        s,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    sol.graphExpression?.let { g ->
                        OutlinedButton(onClick = { onGraph(g) }) { Text("Graficar") }
                    }
                    sol.values.firstOrNull()?.let { v ->
                        OutlinedButton(onClick = {
                            vm.insertValue(v)
                            vm.showMessage("Resultado enviado a la calculadora")
                            onBack()
                        }) { Text("Usar resultado") }
                    }
                }
            }
            SectionCard("Pasos") {
                sol.steps.forEachIndexed { i, step ->
                    StepRow(i + 1, step.title, step.math) { actions.copy(step.math) }
                }
            }
        }
    }
}

@Composable
private fun StepRow(number: Int, title: String, math: String, onCopy: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 0.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.padding(start = 12.dp, end = 12.dp).weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                math,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Carga una versión reducida de la foto para mostrarla (la original puede pesar varios MB). */
private fun loadPreview(context: Context, uri: Uri): ImageBitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > 1200) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }?.asImageBitmap()
} catch (e: Exception) {
    null
}
