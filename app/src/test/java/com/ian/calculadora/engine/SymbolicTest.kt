package com.ian.calculadora.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolicTest {

    private fun d(s: String) = Symbolic.derivative(Symbolic.parse(s)).derivative.toString()
    private fun simp(s: String) = Symbolic.parse(s).toString()

    /** Comprueba numéricamente que la derivada simbólica coincide con la numérica. */
    private fun checkNumeric(s: String, vararg xs: Double) {
        val f = Symbolic.parse(s)
        val df = Symbolic.derivative(f).derivative
        val ctx = EvalContext(AngleMode.RAD)
        val fx = Evaluator.compile(s)
        for (x in xs) {
            val expected = derivative({ fx.at(it, ctx) }, x)
            val got = Symbolic.evaluate(df, x)
            assertEquals("d/dx $s = $df en x=$x", expected, got, 1e-6 * maxOf(1.0, Math.abs(expected)))
        }
    }

    @Test fun simplification() {
        assertEquals("2x", simp("x+x"))
        assertEquals("x²", simp("x·x"))
        assertEquals("0", simp("x−x"))
        assertEquals("2x + 2", simp("2(x+1)"))
        assertEquals("6x⁵", simp("2x^2·3x^3"))
        assertEquals("x", simp("x^1+0"))
        assertEquals("x/3", simp("x/3"))
        assertEquals("2x/3", simp("2x/3"))
        assertEquals("x", simp("ln(e^x)"))
        assertEquals("4x²", simp("(2x)^2"))
        assertEquals("5", simp("2+3"))
    }

    @Test fun derivatives() {
        assertEquals("3x²", d("x^3"))
        assertEquals("x²·cos(x) + 2x·sin(x)", d("x^2·sin(x)"))
        assertEquals("6x + 2", d("3x^2+2x+1"))
        assertEquals("1/x", d("ln(x)"))
        assertEquals("e^x", d("e^x"))
        assertEquals("−sin(x)", d("cos(x)"))
        assertEquals("2cos(2x)", d("sin(2x)"))
        assertEquals("1/(2√x)", d("√x"))
        assertEquals("0", d("π"))
        val r = Symbolic.derivative(Symbolic.parse("sin(x^2)"))
        assertTrue(r.rules.contains("cadena"))
        assertEquals("2x·cos(x²)", r.derivative.toString())
    }

    @Test fun derivativesMatchNumeric() {
        listOf(
            "x^2·sin(x)", "sin(x)/x", "e^(x^2)", "ln(x^2+1)", "tan(x)", "x^x", "√(x^2+1)",
            "asin(x/2)", "atan(x)", "2^x", "log(x)", "cosh(x)·x", "(x+1)/(x−1)", "abs(x−3)", "∛x",
        ).forEach { checkNumeric(it, 0.3, 0.7, 1.4) }
    }

    @Test fun printedOutputReparses() {
        val f = Symbolic.parse("x^2·sin(x)+1/(x+2)")
        val df = Symbolic.derivative(f).derivative
        val v = Evaluator.compile(df.toString()).at(1.3, EvalContext(AngleMode.RAD))
        assertTrue(v.isFinite())
    }

    @Test fun tangentAndPolynomials() {
        assertEquals("2x − 1", Symbolic.tangent(Symbolic.parse("x^2"), 1.0).toString())
        val c = Symbolic.polynomialCoefficients("(x+1)^2·(x−2)")!!
        assertEquals(listOf(-2.0, -3.0, 0.0, 1.0), c)
        assertEquals("x³ − 3x − 2", Symbolic.polynomialSym(c).toString())
        assertEquals("(x + 1)²(x − 2)", Symbolic.factorText(c))
        assertEquals("(x − 1)(x² + 1)", Symbolic.factorText(Symbolic.polynomialCoefficients("x^3−x^2+x−1")!!))
        assertEquals(null, Symbolic.polynomialCoefficients("sin(x)"))
        assertEquals("(x − 1)(x − 1 − √3)(x − 1 + √3)", Symbolic.factorText(Symbolic.polynomialCoefficients("x^3−3x^2+2")!!))
        assertEquals("(x − √2)(x + √2)", Symbolic.factorText(Symbolic.polynomialCoefficients("x^2−2")!!))
        assertEquals(64.0, Evaluator.evaluate("2¹⁰÷2⁴"), 0.0)
    }
}
