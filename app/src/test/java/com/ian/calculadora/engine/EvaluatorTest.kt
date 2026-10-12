package com.ian.calculadora.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EvaluatorTest {

    private fun eval(s: String, mode: AngleMode = AngleMode.DEG, ans: Double = 0.0) =
        Evaluator.evaluate(s, mode, ans) { 0.5 }

    private fun check(expected: Double, s: String, mode: AngleMode = AngleMode.DEG) =
        assertEquals(s, expected, eval(s, mode), 1e-9)

    private fun fails(s: String) {
        assertThrows(s, CalcException::class.java) { eval(s) }
    }

    @Test fun basicArithmetic() {
        check(7.0, "1+2×3")
        check(9.0, "(1+2)×3")
        check(2.5, "5÷2")
        check(-1.0, "2−3")
        check(1.0, "10 mod 3")
        check(2.0, "−10 mod 3")
        check(5.0, "--5")
    }

    @Test fun powerPrecedence() {
        check(-4.0, "−2^2")
        check(4.0, "(−2)^2")
        check(512.0, "2^3^2")
        check(0.125, "2^−3")
        check(-2.0, "(−8)^(1÷3)")
    }

    @Test fun implicitMultiplication() {
        check(2 * Math.PI, "2π")
        check(12.0, "3(4)")
        check(6.0, "(2)(3)")
        check(1.0, "2sin(30)")
        check(6.0, "2√9")
    }

    @Test fun autoClosesParentheses() {
        check(12.0, "3×(2+2")
        check(0.5, "sin(30")
    }

    @Test fun trigonometry() {
        check(0.5, "sin(30)")
        check(0.0, "sin(180)")
        check(0.0, "cos(90)")
        check(1.0, "tan(45)")
        check(30.0, "sin⁻¹(0.5)")
        check(0.0, "sin(π)", AngleMode.RAD)
        check(Math.PI / 2, "cos⁻¹(0)", AngleMode.RAD)
        fails("tan(90)")
        fails("sin⁻¹(2)")
    }

    @Test fun hyperbolic() {
        check(0.0, "sinh(0)")
        check(1.0, "cosh(0)")
        check(1.0, "sinh⁻¹(sinh(1))")
        fails("cosh⁻¹(0.5)")
    }

    @Test fun logsAndRoots() {
        check(2.0, "log(100)")
        check(1.0, "ln(e)")
        check(10.0, "log₂(1024)")
        check(3.0, "√9")
        check(-3.0, "∛(−27)")
        check(2.0, "root(16,4)")
        check(-2.0, "root(−32,5)")
        check(1500.0, "1.5E3")
        check(0.0002, "2E−4")
        fails("log(0)")
        fails("√(−1)")
        fails("root(−16,2)")
    }

    @Test fun factorialAndCombinatorics() {
        check(120.0, "5!")
        check(1.0, "0!")
        check(720.0, "3!!")
        check(Math.sqrt(Math.PI) / 2, "0.5!")
        check(10.0, "nCr(5,2)")
        check(20.0, "nPr(5,2)")
        fails("(−1)!")
        fails("nCr(2,5)")
        fails("171!")
    }

    @Test fun percent() {
        check(0.5, "50%")
        check(220.0, "200+10%")
        check(180.0, "200−10%")
        check(50.0, "200×25%")
    }

    @Test fun constantsAndAns() {
        check(Math.E, "e")
        assertEquals(42.0, eval("Ans", ans = 42.0), 0.0)
        assertEquals(84.0, eval("2Ans", ans = 42.0), 0.0)
        check(0.5, "Rand")
        check(Math.PI * Math.E, "πe")
    }

    @Test fun absolute() {
        check(5.0, "abs(−5)")
    }

    @Test fun errors() {
        fails("")
        fails("5÷0")
        fails("5+")
        fails("()")
        fails("2)")
        fails("1..2")
        fails("×5")
        fails("nCr(5)")
        fails("0^−1")
    }

    @Test fun formatter() {
        assertEquals("0.3", NumberFormatter.format(eval("0.1+0.2")))
        assertEquals("−5", NumberFormatter.format(-5.0))
        assertEquals("1.5E20", NumberFormatter.format(1.5e20))
        assertEquals("1E−12", NumberFormatter.format(1e-12))
        assertEquals("0.333333333333", NumberFormatter.format(1.0 / 3))
        assertEquals("1,234,567.89", NumberFormatter.formatGrouped(1234567.89))
        assertEquals("−1,000", NumberFormatter.formatGrouped(-1000.0))
        // El texto formateado se puede volver a evaluar.
        assertEquals(-1.5e-12, eval(NumberFormatter.format(-1.5e-12)), 1e-24)
    }
}
