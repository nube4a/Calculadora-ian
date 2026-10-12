package com.ian.calculadora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.ian.calculadora.ui.CalculatorScreen
import com.ian.calculadora.ui.HistoryScreen
import com.ian.calculadora.ui.theme.CalculadoraTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculadoraTheme {
                var showHistory by rememberSaveable { mutableStateOf(false) }
                BackHandler(enabled = showHistory) { showHistory = false }
                AnimatedContent(
                    targetState = showHistory,
                    transitionSpec = {
                        if (targetState) {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 3 }
                        } else {
                            slideInHorizontally { -it / 3 } togetherWith slideOutHorizontally { it }
                        }
                    },
                    label = "pantalla",
                ) { history ->
                    if (history) {
                        HistoryScreen(viewModel, onBack = { showHistory = false })
                    } else {
                        CalculatorScreen(viewModel, onOpenHistory = { showHistory = true })
                    }
                }
            }
        }
    }
}
