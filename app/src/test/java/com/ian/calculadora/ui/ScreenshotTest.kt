package com.ian.calculadora.ui

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ian.calculadora.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Recorre la app real y guarda capturas de cada pantalla (como en un Pixel). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    private fun key(label: String) {
        compose.onAllNodes(hasText(label) and hasClickAction()).onFirst().performClick()
    }

    private fun keys(vararg labels: String) = labels.forEach { key(it) }

    private fun back() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private fun openTool(title: String) {
        compose.onNodeWithContentDescription("Herramientas").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(title).performClick()
        compose.waitForIdle()
    }

    @Test
    fun recorrido() {
        // Bienvenida (primera vez)
        shot("00_bienvenida_1")
        key("Siguiente")
        shot("00_bienvenida_2")
        key("Siguiente")
        shot("00_bienvenida_3")
        key("Empezar")
        shot("00_calculadora_inicio")
        keys("2", "sin", "3", "0", ")", "+", "√", "1", "6")
        shot("01_calculadora_escribiendo")
        key("=")
        shot("02_resultado")
        key("2nd")
        shot("03_funciones_2nd")
        key("2nd")

        compose.onNodeWithContentDescription("Herramientas").performClick()
        shot("04_menu_herramientas")
        compose.onNodeWithText("Escanear y resolver (paso a paso)").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Resolver paso a paso").performClick()
        shot("05_escanear")
        compose.onNodeWithText("Pasos").performScrollTo()
        shot("06_paso_a_paso")
        back()

        openTool("Gráficas")
        shot("07_graficas")
        back()

        openTool("Derivadas y álgebra")
        shot("08_derivadas")
        back()

        openTool("Ecuaciones")
        shot("09_ecuaciones")
        back()

        openTool("Conversor de unidades")
        shot("10_unidades")
        back()

        openTool("Programador (HEX/BIN)")
        shot("11_programador")
        back()

        openTool("Estadística y regresión")
        shot("12_estadistica")
        back()

        compose.onNodeWithContentDescription("Historial").performClick()
        shot("13_historial")
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi-night")
    fun modoOscuro() {
        shot("20_oscuro_bienvenida")
        key("Saltar")
        shot("21_oscuro_inicio")
        keys("2", "sin", "3", "0", ")", "+", "√", "1", "6")
        key("=")
        shot("22_oscuro_resultado")
        compose.onNodeWithContentDescription("Herramientas").performClick()
        shot("23_oscuro_menu")
    }
}
