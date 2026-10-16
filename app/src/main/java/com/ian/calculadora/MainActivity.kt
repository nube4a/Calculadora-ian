package com.ian.calculadora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.ian.calculadora.ui.WelcomeScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ian.calculadora.ui.AlgebraScreen
import com.ian.calculadora.ui.CalculatorScreen
import com.ian.calculadora.ui.ConstantsScreen
import com.ian.calculadora.ui.GraphScreen
import com.ian.calculadora.ui.HistoryScreen
import com.ian.calculadora.ui.ProgrammerScreen
import com.ian.calculadora.ui.ScanScreen
import com.ian.calculadora.ui.SolverScreen
import com.ian.calculadora.ui.StatsScreen
import com.ian.calculadora.ui.UnitsScreen
import com.ian.calculadora.ui.theme.CalculadoraTheme
import kotlinx.coroutines.launch

enum class Screen(val title: String, val icon: ImageVector) {
    CALC("Calculadora", Icons.Outlined.Calculate),
    SCAN("Resolver paso a paso", Icons.Outlined.DocumentScanner),
    GRAPH("Gráficas", Icons.Outlined.ShowChart),
    ALGEBRA("Derivadas y álgebra", Icons.Outlined.Insights),
    SOLVER("Ecuaciones", Icons.Outlined.Functions),
    UNITS("Conversor de unidades", Icons.Outlined.SwapHoriz),
    PROGRAMMER("Programador (HEX/BIN)", Icons.Outlined.Memory),
    STATS("Estadística y regresión", Icons.Outlined.QueryStats),
    CONSTANTS("Constantes físicas", Icons.Outlined.Science),
    HISTORY("Historial", Icons.Outlined.History),
}

class MainActivity : ComponentActivity() {

    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculadoraTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.CALC) }
                val drawer = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val goBack = { screen = Screen.CALC }
                var showWelcome by rememberSaveable { mutableStateOf(viewModel.loadText("welcome_done") != "1") }
                val finishWelcome = {
                    viewModel.saveText("welcome_done", "1")
                    showWelcome = false
                }
                Box(Modifier.fillMaxSize()) {
                BackHandler(enabled = screen != Screen.CALC) { goBack() }
                BackHandler(enabled = drawer.isOpen) { scope.launch { drawer.close() } }

                ModalNavigationDrawer(
                    drawerState = drawer,
                    gesturesEnabled = screen == Screen.CALC || drawer.isOpen,
                    drawerContent = {
                        ModalDrawerSheet {
                            androidx.compose.foundation.layout.Column(Modifier.verticalScroll(rememberScrollState())) {
                                Row(
                                    Modifier.padding(24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFF13203A))) {
                                        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, Modifier.size(52.dp))
                                    }
                                    Text(
                                        "Calculadora IAN",
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.padding(start = 14.dp),
                                    )
                                }
                                // Herramientas agrupadas por tema para encontrarlas más rápido.
                                listOf(
                                    "Calcular" to listOf(Screen.CALC, Screen.SCAN, Screen.HISTORY),
                                    "Matemáticas" to listOf(Screen.GRAPH, Screen.ALGEBRA, Screen.SOLVER, Screen.STATS),
                                    "Ingeniería" to listOf(Screen.UNITS, Screen.PROGRAMMER, Screen.CONSTANTS),
                                ).forEachIndexed { index, (section, items) ->
                                    if (index > 0) {
                                        androidx.compose.material3.HorizontalDivider(
                                            Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                    Text(
                                        section,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 28.dp, top = 8.dp, bottom = 6.dp),
                                    )
                                    items.forEach { s ->
                                        NavigationDrawerItem(
                                            icon = { Icon(s.icon, contentDescription = null) },
                                            label = { Text(s.title) },
                                            selected = s == screen,
                                            onClick = {
                                                screen = s
                                                scope.launch { drawer.close() }
                                            },
                                            colors = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            ),
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                        )
                                    }
                                }
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val version = remember {
                                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
                                }
                                Text(
                                    "Versión ${version ?: "?"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                                )
                            }
                        }
                    },
                ) {
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            if (targetState != Screen.CALC) {
                                slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 3 }
                            } else {
                                slideInHorizontally { -it / 3 } togetherWith slideOutHorizontally { it }
                            }
                        },
                        label = "pantalla",
                    ) { current ->
                        when (current) {
                            Screen.CALC -> CalculatorScreen(
                                viewModel,
                                onOpenHistory = { screen = Screen.HISTORY },
                                onOpenMenu = { scope.launch { drawer.open() } },
                                onOpenScan = { screen = Screen.SCAN },
                            )
                            Screen.SCAN -> ScanScreen(viewModel, onBack = goBack, onGraph = { expr ->
                                viewModel.saveText("graph_f1", expr)
                                screen = Screen.GRAPH
                            })
                            Screen.HISTORY -> HistoryScreen(viewModel, onBack = goBack)
                            Screen.GRAPH -> GraphScreen(viewModel, onBack = goBack)
                            Screen.ALGEBRA -> AlgebraScreen(viewModel, onBack = goBack, onGraph = { exprs ->
                                listOf("graph_f1", "graph_f2", "graph_f3").forEachIndexed { i, key ->
                                    viewModel.saveText(key, exprs.getOrElse(i) { "" })
                                }
                                screen = Screen.GRAPH
                            })
                            Screen.SOLVER -> SolverScreen(viewModel, onBack = goBack)
                            Screen.UNITS -> UnitsScreen(viewModel, onBack = goBack)
                            Screen.PROGRAMMER -> ProgrammerScreen(viewModel, onBack = goBack)
                            Screen.STATS -> StatsScreen(viewModel, onBack = goBack, onPlot = { expr ->
                                viewModel.saveText("graph_f1", expr)
                                screen = Screen.GRAPH
                            })
                            Screen.CONSTANTS -> ConstantsScreen(viewModel, onBack = goBack)
                        }
                    }
                }
                // Bienvenida de la primera vez: se muestra encima y se desvanece al terminar.
                AnimatedVisibility(visible = showWelcome, exit = fadeOut()) {
                    BackHandler { finishWelcome() }
                    WelcomeScreen(onFinish = finishWelcome)
                }
                }
            }
        }
    }
}
