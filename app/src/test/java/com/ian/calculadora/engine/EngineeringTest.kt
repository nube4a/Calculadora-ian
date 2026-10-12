package com.ian.calculadora.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineeringTest {

    private fun rad(s: String) = Evaluator.evaluate(s, AngleMode.RAD)

    @Test fun calculus() {
        assertEquals(2.0, rad("∫(sin(x),0,π)"), 1e-10)
        assertEquals(1.0 / 3, rad("∫(x^2,0,1)"), 1e-12)
        assertEquals(Math.sqrt(Math.PI), rad("∫(e^(−x^2),−10,10)"), 1e-9)
        assertEquals(6.0, rad("d/dx(x^2,3)"), 0.0)
        assertEquals(1.0, rad("d/dx(sin(x),0)"), 0.0)
        assertEquals(5050.0, rad("Σ(x,1,100)"), 0.0)
        assertEquals(120.0, rad("Π(x,1,5)"), 0.0)
        // Anidado: ∫₀¹ ∫₀¹ (x) ... la x interior no pisa a la exterior.
        assertEquals(0.5, rad("∫(∫(x,0,1),0,1)"), 1e-12)
        assertEquals(15.0, rad("Σ(Σ(1,1,x),1,5)"), 0.0)
        assertThrows(CalcException::class.java) { rad("x+1") }
        assertThrows(CalcException::class.java) { rad("Σ(x,1,2.5)") }
    }

    @Test fun aliasesAndNewFunctions() {
        assertEquals(Math.PI, rad("pi"), 0.0)
        assertEquals(3.0, rad("sqrt(9)"), 0.0)
        assertEquals(Math.PI / 2, rad("asin(1)"), 1e-15)
        assertEquals(Math.E, rad("exp(1)"), 0.0)
        assertEquals(6.0, rad("gcd(12,18)"), 0.0)
        assertEquals(36.0, rad("lcm(12,18)"), 0.0)
        assertEquals(9.0, rad("max(3,9,−1)"), 0.0)
        assertEquals(-2.0, rad("floor(−1.5)"), 0.0)
        assertEquals(25.0, rad("5²"), 0.0)
        assertEquals(2.0, rad("4*3/6"), 0.0)
    }

    @Test fun compiledExpressionAndEquations() {
        val f = Evaluator.compile("x^2−2")
        val ctx = EvalContext(AngleMode.RAD)
        assertEquals(7.0, f.at(3.0, ctx), 0.0)
        assertTrue(Evaluator.compile("1/x").at(0.0, ctx).isNaN())
        val eq = Evaluator.compileEquation("x^2 = 2")
        val roots = findRoots({ eq.at(it, ctx) }, -10.0, 10.0)
        assertEquals(2, roots.size)
        assertEquals(-Math.sqrt(2.0), roots[0], 1e-10)
        assertEquals(Math.sqrt(2.0), roots[1], 1e-10)
    }

    @Test fun rootsSkipAsymptotesAndFindDoubleRoots() {
        val ctx = EvalContext(AngleMode.RAD)
        val tan = Evaluator.compile("tan(x)")
        val r = findRoots({ tan.at(it, ctx) }, -2.0, 2.0)
        assertEquals(listOf(0.0), r)
        val sq = Evaluator.compile("(x−1)^2")
        val r2 = findRoots({ sq.at(it, ctx) }, -5.0, 5.0)
        assertEquals(1, r2.size)
        assertEquals(1.0, r2[0], 1e-6)
    }

    @Test fun polynomials() {
        // x² − 5x + 6 = 0 → 2, 3
        assertEquals(listOf(Complex(2.0), Complex(3.0)), polynomialRoots(listOf(1.0, -5.0, 6.0)))
        // x² + 1 = 0 → ±i
        val c = polynomialRoots(listOf(1.0, 0.0, 1.0))
        assertEquals(Complex(0.0, -1.0), c[0])
        assertEquals(Complex(0.0, 1.0), c[1])
        // x³ − 6x² + 11x − 6 → 1, 2, 3
        assertEquals(listOf(Complex(1.0), Complex(2.0), Complex(3.0)), polynomialRoots(listOf(1.0, -6.0, 11.0, -6.0)))
        // x⁴ − 1 → −1, 1, ±i
        val q = polynomialRoots(listOf(1.0, 0.0, 0.0, 0.0, -1.0))
        assertEquals(listOf(Complex(-1.0), Complex(1.0), Complex(0.0, -1.0), Complex(0.0, 1.0)), q)
        // x³ = 0 con raíz en cero
        assertEquals(listOf(Complex(0.0), Complex(0.0), Complex(1.0)), polynomialRoots(listOf(1.0, -1.0, 0.0, 0.0)))
        assertEquals("1 + 2i", Complex(1.0, 2.0).toString())
        assertEquals("−3i", Complex(0.0, -3.0).toString())
    }

    @Test fun linearSystems() {
        // 2x + y = 5 ; x − y = 1 → x=2, y=1
        assertEquals(listOf(2.0, 1.0), solveLinear(listOf(listOf(2.0, 1.0), listOf(1.0, -1.0)), listOf(5.0, 1.0)))
        val s = solveLinear(
            listOf(listOf(1.0, 1.0, 1.0), listOf(0.0, 2.0, 5.0), listOf(2.0, 5.0, -1.0)),
            listOf(6.0, -4.0, 27.0),
        )
        assertEquals(listOf(5.0, 3.0, -2.0), s)
        assertThrows(CalcException::class.java) {
            solveLinear(listOf(listOf(1.0, 2.0), listOf(2.0, 4.0)), listOf(1.0, 2.0))
        }
        assertEquals(-3.0, determinant(listOf(listOf(1.0, 2.0), listOf(3.0, 3.0))), 0.0)
    }

    @Test fun statisticsAndRegression() {
        val st = statistics(parseNumberList("2, 4 4 4\n5;5 7 9"))
        assertEquals(8, st.n)
        assertEquals(5.0, st.mean, 0.0)
        assertEquals(2.0, st.sdPop, 1e-12)
        assertEquals(4.5, st.median, 0.0)
        assertEquals(listOf(4.0), st.modes)
        assertEquals(4.0, st.q1, 0.0)
        assertEquals(5.5, st.q3, 0.0)

        val lin = regression(listOf(1.0, 2.0, 3.0, 4.0), listOf(3.0, 5.0, 7.0, 9.0), RegressionType.LINEAR)
        assertEquals(listOf(1.0, 2.0), lin.coefficients)
        assertEquals(1.0, lin.r!!, 1e-12)
        assertEquals(1.0, lin.r2, 1e-12)

        val quad = regression(listOf(0.0, 1.0, 2.0, 3.0), listOf(1.0, 2.0, 5.0, 10.0), RegressionType.QUADRATIC)
        assertEquals(listOf(1.0, 0.0, 1.0), quad.coefficients)
        assertNull(quad.r)

        val exp = regression(listOf(0.0, 1.0, 2.0), listOf(2.0, 2 * Math.E, 2 * Math.E * Math.E), RegressionType.EXPONENTIAL)
        assertEquals(2.0, exp.coefficients[0], 1e-10)
        assertEquals(1.0, exp.coefficients[1], 1e-10)
        // La expresión generada se puede graficar/evaluar.
        val f = Evaluator.compile(lin.expression)
        assertEquals(11.0, f.at(5.0, EvalContext(AngleMode.RAD)), 1e-12)
    }

    @Test fun units() {
        fun conv(cat: String, v: Double, from: String, to: String): Double {
            val units = Units.categories.first { it.name == cat }.units
            return Units.convert(v, units.first { it.symbol == from }, units.first { it.symbol == to })
        }
        assertEquals(212.0, conv("Temperatura", 100.0, "°C", "°F"), 1e-9)
        assertEquals(-40.0, conv("Temperatura", -40.0, "°F", "°C"), 1e-9)
        assertEquals(273.15, conv("Temperatura", 0.0, "°C", "K"), 1e-9)
        assertEquals(25.4, conv("Longitud", 1.0, "in", "mm"), 1e-9)
        assertEquals(14.6959, conv("Presión", 1.0, "atm", "psi"), 1e-4)
        assertEquals(1.0, conv("Energía", 3.6e6, "J", "kWh"), 1e-12)
        assertEquals(100.0, conv("Velocidad", 27.7777777778, "m/s", "km/h"), 1e-6)
        assertEquals(180.0, conv("Ángulo", Math.PI, "rad", "°"), 1e-9)
    }

    @Test fun programmer() {
        assertEquals(255L, Programmer.parse("FF", Base.HEX))
        assertEquals(-1L, Programmer.parse("FFFFFFFFFFFFFFFF", Base.HEX))
        assertEquals(10L, Programmer.parse("1010", Base.BIN))
        assertEquals("FF", Programmer.format(255, Base.HEX))
        assertEquals("1010 1010", Programmer.format(170, Base.BIN))
        assertEquals("1 0000", Programmer.format(65536, Base.HEX))
        assertEquals("−5", Programmer.format(-5, Base.DEC))
        assertEquals(0b1000L, Programmer.apply(BitOp.AND, 0b1100, 0b1010))
        assertEquals(0b0110L, Programmer.apply(BitOp.XOR, 0b1100, 0b1010))
        assertEquals(-1L, Programmer.apply(BitOp.NOT, 0, 0))
        assertEquals(40L, Programmer.apply(BitOp.SHL, 5, 3))
        assertEquals(Long.MIN_VALUE, Programmer.apply(BitOp.ROR, 1, 1))
        assertThrows(CalcException::class.java) { Programmer.parse("12G", Base.HEX) }
        assertThrows(CalcException::class.java) { Programmer.parse("2", Base.BIN) }
    }

    @Test fun displayModes() {
        assertEquals("1.2345E4", NumberFormatter.format(12345.0, DisplayMode.SCI))
        assertEquals("12.345E3", NumberFormatter.format(12345.0, DisplayMode.ENG))
        assertEquals("4.7E−3", NumberFormatter.format(0.0047, DisplayMode.ENG))
        assertEquals("470E−6", NumberFormatter.format(0.00047, DisplayMode.ENG))
        assertEquals("5", NumberFormatter.format(5.0, DisplayMode.SCI))
        assertEquals("4.7 m", NumberFormatter.siPrefix(0.0047))
        assertEquals("2.2 M", NumberFormatter.siPrefix(2.2e6))
        assertNull(NumberFormatter.siPrefix(5.0))
        // ENG es re-evaluable
        assertEquals(0.00047, Evaluator.evaluate(NumberFormatter.format(0.00047, DisplayMode.ENG)), 1e-18)
    }
}
