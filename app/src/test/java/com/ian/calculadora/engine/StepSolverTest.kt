package com.ian.calculadora.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepSolverTest {

    private fun values(s: String, angle: AngleMode = AngleMode.RAD) = StepSolver.solve(s, angle).values

    @Test fun photoExample() {
        val sol = StepSolver.solve("14√x+15=71")
        assertEquals("Despeje", sol.method)
        assertEquals(listOf(16.0), sol.values)
        val math = sol.steps.map { it.math }
        assertEquals("14√x + 15 = 71", math[0])
        assertEquals("14√x = 56", math[1])
        assertEquals("√x = 4", math[2])
        assertEquals("x = 16", math[3])
        assertTrue(math.last().endsWith("✓"))
        sol.steps.forEach { println("${it.title}:  ${it.math}") }
    }

    @Test fun isolation() {
        assertEquals(listOf(4.0), values("2x+3=11"))
        assertEquals(listOf(-3.0, 3.0), values("x^2=9"))
        assertEquals(listOf(-4.0, 2.0), values("(x+1)^2=9"))
        assertEquals(listOf(1000.0), values("log(x)=3"))
        assertEquals(listOf(3.0), values("2^x=8"))
        assertEquals(listOf(-2.0), values("x^3=−8"))
        assertEquals(listOf(-5.0, 5.0), values("abs(x)=5"))
        assertEquals(listOf(30.0, 150.0), values("sin(x)=0.5", AngleMode.DEG))
        assertEquals(listOf(5.0), values("10/x=2"))
        assertEquals(listOf(2.0), values("7−x=5"))
    }

    @Test fun swappedSidesAndNoSolution() {
        assertEquals(listOf(16.0), values("71=14√x+15"))
        assertEquals(emptyList<Double>(), values("√x=−2"))
        // x² = −4 no tiene solución real, pero sí complejas (vía cuadrática no aplica: se despeja).
        assertEquals(emptyList<Double>(), values("x^2=−4"))
    }

    @Test fun polynomials() {
        val lin = StepSolver.solve("3x+2=x+10")
        assertEquals("Ecuación lineal", lin.method)
        assertEquals(listOf(4.0), lin.values)

        val quad = StepSolver.solve("x^2−5x+6=0")
        assertEquals("Ecuación cuadrática", quad.method)
        assertEquals(listOf(2.0, 3.0), quad.values)
        assertTrue(quad.steps.any { it.math.contains("Δ") && it.math.endsWith("= 1") })

        val complex = StepSolver.solve("x^2+2x+5=0")
        assertEquals(listOf("x = −1 − 2i", "x = −1 + 2i"), complex.solutions)

        val cubic = StepSolver.solve("x^3−6x^2+11x=6")
        assertEquals(listOf(1.0, 2.0, 3.0), cubic.values)

        assertEquals("Identidad", StepSolver.solve("2(x+1)=2x+2").method)
        assertEquals("Contradicción", StepSolver.solve("x+1=x+2").method)
    }

    @Test fun numericFallbackAndEvaluation() {
        val sol = StepSolver.solve("e^x=3−x")
        assertEquals("Numérico", sol.method)
        assertEquals(0.792059968, sol.values.single(), 1e-8)
        assertEquals(listOf(7.0), StepSolver.solve("3+4").values)
        assertEquals(listOf("Verdadero"), StepSolver.solve("2+2=4").solutions)
    }

    @Test fun fractions() {
        assertEquals("3/4", StepSolver.toFraction(0.75))
        assertEquals("−1/3", StepSolver.toFraction(-1.0 / 3))
        assertEquals(null, StepSolver.toFraction(Math.PI))
        assertEquals(listOf("x = 1/3  (0.333333333333)"), StepSolver.solve("3x=1").solutions)
    }

    @Test fun ocrNormalization() {
        assertEquals("14√x+15=71", MathText.normalizeOcr("14Vx +15 =71"))
        assertEquals("x^2−4=0", MathText.normalizeOcr("X2 - 4 = 0"))
        assertEquals("10÷2", MathText.normalizeOcr("1O : 2"))
        assertEquals(listOf(16.0), values(MathText.normalizeOcr("14 Vx + 15 = 71")))
    }
}
