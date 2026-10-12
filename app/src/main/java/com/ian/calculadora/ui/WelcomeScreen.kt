package com.ian.calculadora.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.calculadora.R
import kotlinx.coroutines.launch

private data class WelcomePage(val icon: ImageVector?, val title: String, val body: String)

private val pages = listOf(
    WelcomePage(
        null,
        "¡Bienvenido a Calculadora IAN!",
        "Una calculadora científica completa, con historial y memoria, hecha para estudiar y trabajar.",
    ),
    WelcomePage(
        Icons.Outlined.DocumentScanner,
        "Escanea y resuelve",
        "Toma una foto de una ecuación y mira la solución paso a paso, sin necesidad de internet.",
    ),
    WelcomePage(
        Icons.Outlined.Functions,
        "Herramientas de ingeniería",
        "Gráficas, derivadas, ecuaciones, unidades, modo programador y estadística. Todo en el menú ☰.",
    ),
)

@Composable
fun WelcomeScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!last) TextButton(onClick = onFinish) { Text("Saltar") } else Spacer(Modifier.height(48.dp))
        }
        HorizontalPager(pager, Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(168.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2128)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (page.icon == null) {
                        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, Modifier.size(168.dp))
                    } else {
                        Icon(page.icon, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(84.dp))
                    }
                }
                Spacer(Modifier.height(36.dp))
                Text(
                    page.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    page.body,
                    fontSize = 17.sp,
                    lineHeight = 25.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
        Row(
            Modifier.padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(pages.size) { i ->
                val active = i == pager.currentPage
                Box(
                    Modifier
                        .size(width = if (active) 26.dp else 8.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Button(
            onClick = {
                if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(if (last) "Empezar" else "Siguiente", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
