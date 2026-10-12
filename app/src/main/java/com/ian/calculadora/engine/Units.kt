package com.ian.calculadora.engine

/** Unidad con conversión lineal a la unidad base: base = valor × factor + offset. */
data class MeasureUnit(val symbol: String, val name: String, val factor: Double, val offset: Double = 0.0) {
    fun toBase(v: Double) = v * factor + offset
    fun fromBase(v: Double) = (v - offset) / factor
}

data class UnitCategory(val name: String, val units: List<MeasureUnit>)

object Units {

    fun convert(value: Double, from: MeasureUnit, to: MeasureUnit): Double = to.fromBase(from.toBase(value))

    private const val INCH = 0.0254
    private const val FOOT = 0.3048
    private const val POUND = 0.45359237
    private const val G0 = 9.80665
    private const val GALLON = 3.785411784e-3

    val categories = listOf(
        UnitCategory(
            "Longitud", listOf(
                MeasureUnit("m", "metro", 1.0),
                MeasureUnit("km", "kilómetro", 1e3),
                MeasureUnit("cm", "centímetro", 1e-2),
                MeasureUnit("mm", "milímetro", 1e-3),
                MeasureUnit("µm", "micrómetro", 1e-6),
                MeasureUnit("nm", "nanómetro", 1e-9),
                MeasureUnit("in", "pulgada", INCH),
                MeasureUnit("ft", "pie", FOOT),
                MeasureUnit("yd", "yarda", 0.9144),
                MeasureUnit("mi", "milla", 1609.344),
                MeasureUnit("nmi", "milla náutica", 1852.0),
                MeasureUnit("mil", "milésima de pulgada", INCH / 1000),
            )
        ),
        UnitCategory(
            "Área", listOf(
                MeasureUnit("m²", "metro cuadrado", 1.0),
                MeasureUnit("km²", "kilómetro cuadrado", 1e6),
                MeasureUnit("cm²", "centímetro cuadrado", 1e-4),
                MeasureUnit("mm²", "milímetro cuadrado", 1e-6),
                MeasureUnit("ha", "hectárea", 1e4),
                MeasureUnit("acre", "acre", 4046.8564224),
                MeasureUnit("ft²", "pie cuadrado", FOOT * FOOT),
                MeasureUnit("in²", "pulgada cuadrada", INCH * INCH),
            )
        ),
        UnitCategory(
            "Volumen", listOf(
                MeasureUnit("m³", "metro cúbico", 1.0),
                MeasureUnit("L", "litro", 1e-3),
                MeasureUnit("mL", "mililitro", 1e-6),
                MeasureUnit("cm³", "centímetro cúbico", 1e-6),
                MeasureUnit("gal", "galón (EE. UU.)", GALLON),
                MeasureUnit("ft³", "pie cúbico", FOOT * FOOT * FOOT),
                MeasureUnit("in³", "pulgada cúbica", INCH * INCH * INCH),
                MeasureUnit("bbl", "barril de petróleo", 0.158987294928),
            )
        ),
        UnitCategory(
            "Masa", listOf(
                MeasureUnit("kg", "kilogramo", 1.0),
                MeasureUnit("g", "gramo", 1e-3),
                MeasureUnit("mg", "miligramo", 1e-6),
                MeasureUnit("t", "tonelada", 1e3),
                MeasureUnit("lb", "libra", POUND),
                MeasureUnit("oz", "onza", POUND / 16),
                MeasureUnit("slug", "slug", 14.59390294),
            )
        ),
        UnitCategory(
            "Temperatura", listOf(
                MeasureUnit("°C", "Celsius", 1.0, 273.15),
                MeasureUnit("K", "Kelvin", 1.0),
                MeasureUnit("°F", "Fahrenheit", 5.0 / 9.0, 273.15 - 32 * 5.0 / 9.0),
                MeasureUnit("°R", "Rankine", 5.0 / 9.0),
            )
        ),
        UnitCategory(
            "Presión", listOf(
                MeasureUnit("Pa", "pascal", 1.0),
                MeasureUnit("kPa", "kilopascal", 1e3),
                MeasureUnit("MPa", "megapascal", 1e6),
                MeasureUnit("bar", "bar", 1e5),
                MeasureUnit("atm", "atmósfera", 101325.0),
                MeasureUnit("psi", "libra/pulgada²", POUND * G0 / (INCH * INCH)),
                MeasureUnit("mmHg", "milímetro de mercurio", 133.322387415),
                MeasureUnit("kgf/cm²", "kilogramo-fuerza/cm²", G0 * 1e4),
                MeasureUnit("mH₂O", "metro de columna de agua", 9806.65),
            )
        ),
        UnitCategory(
            "Fuerza", listOf(
                MeasureUnit("N", "newton", 1.0),
                MeasureUnit("kN", "kilonewton", 1e3),
                MeasureUnit("kgf", "kilogramo-fuerza", G0),
                MeasureUnit("lbf", "libra-fuerza", POUND * G0),
                MeasureUnit("dyn", "dina", 1e-5),
                MeasureUnit("tf", "tonelada-fuerza", G0 * 1e3),
            )
        ),
        UnitCategory(
            "Energía", listOf(
                MeasureUnit("J", "julio", 1.0),
                MeasureUnit("kJ", "kilojulio", 1e3),
                MeasureUnit("MJ", "megajulio", 1e6),
                MeasureUnit("cal", "caloría", 4.184),
                MeasureUnit("kcal", "kilocaloría", 4184.0),
                MeasureUnit("Wh", "vatio-hora", 3600.0),
                MeasureUnit("kWh", "kilovatio-hora", 3.6e6),
                MeasureUnit("BTU", "BTU", 1055.05585262),
                MeasureUnit("eV", "electronvoltio", 1.602176634e-19),
                MeasureUnit("ft·lbf", "pie-libra", FOOT * POUND * G0),
            )
        ),
        UnitCategory(
            "Potencia", listOf(
                MeasureUnit("W", "vatio", 1.0),
                MeasureUnit("kW", "kilovatio", 1e3),
                MeasureUnit("MW", "megavatio", 1e6),
                MeasureUnit("hp", "caballo de fuerza (mecánico)", 745.69987158227),
                MeasureUnit("CV", "caballo de vapor", 735.49875),
                MeasureUnit("BTU/h", "BTU por hora", 1055.05585262 / 3600),
                MeasureUnit("TR", "tonelada de refrigeración", 3516.8528420667),
            )
        ),
        UnitCategory(
            "Velocidad", listOf(
                MeasureUnit("m/s", "metro/segundo", 1.0),
                MeasureUnit("km/h", "kilómetro/hora", 1 / 3.6),
                MeasureUnit("mph", "milla/hora", 0.44704),
                MeasureUnit("ft/s", "pie/segundo", FOOT),
                MeasureUnit("kn", "nudo", 1852.0 / 3600),
                MeasureUnit("Mach", "Mach (nivel del mar)", 340.3),
            )
        ),
        UnitCategory(
            "Caudal", listOf(
                MeasureUnit("m³/s", "metro cúbico/segundo", 1.0),
                MeasureUnit("m³/h", "metro cúbico/hora", 1 / 3600.0),
                MeasureUnit("L/s", "litro/segundo", 1e-3),
                MeasureUnit("L/min", "litro/minuto", 1e-3 / 60),
                MeasureUnit("gpm", "galón/minuto", GALLON / 60),
                MeasureUnit("cfm", "pie cúbico/minuto", FOOT * FOOT * FOOT / 60),
            )
        ),
        UnitCategory(
            "Par / torque", listOf(
                MeasureUnit("N·m", "newton-metro", 1.0),
                MeasureUnit("kN·m", "kilonewton-metro", 1e3),
                MeasureUnit("kgf·m", "kilogramo-fuerza-metro", G0),
                MeasureUnit("lbf·ft", "libra-fuerza-pie", POUND * G0 * FOOT),
                MeasureUnit("lbf·in", "libra-fuerza-pulgada", POUND * G0 * INCH),
            )
        ),
        UnitCategory(
            "Densidad", listOf(
                MeasureUnit("kg/m³", "kilogramo/metro cúbico", 1.0),
                MeasureUnit("g/cm³", "gramo/centímetro cúbico", 1e3),
                MeasureUnit("kg/L", "kilogramo/litro", 1e3),
                MeasureUnit("lb/ft³", "libra/pie cúbico", POUND / (FOOT * FOOT * FOOT)),
            )
        ),
        UnitCategory(
            "Tiempo", listOf(
                MeasureUnit("s", "segundo", 1.0),
                MeasureUnit("ms", "milisegundo", 1e-3),
                MeasureUnit("µs", "microsegundo", 1e-6),
                MeasureUnit("min", "minuto", 60.0),
                MeasureUnit("h", "hora", 3600.0),
                MeasureUnit("d", "día", 86400.0),
                MeasureUnit("sem", "semana", 604800.0),
                MeasureUnit("año", "año (365.25 d)", 31557600.0),
            )
        ),
        UnitCategory(
            "Ángulo", listOf(
                MeasureUnit("°", "grado", Math.PI / 180),
                MeasureUnit("rad", "radián", 1.0),
                MeasureUnit("grad", "gradián", Math.PI / 200),
                MeasureUnit("′", "minuto de arco", Math.PI / 10800),
                MeasureUnit("″", "segundo de arco", Math.PI / 648000),
                MeasureUnit("rev", "revolución", 2 * Math.PI),
            )
        ),
        UnitCategory(
            "Frecuencia", listOf(
                MeasureUnit("Hz", "hercio", 1.0),
                MeasureUnit("kHz", "kilohercio", 1e3),
                MeasureUnit("MHz", "megahercio", 1e6),
                MeasureUnit("GHz", "gigahercio", 1e9),
                MeasureUnit("rpm", "revoluciones/minuto", 1 / 60.0),
                MeasureUnit("rad/s", "radián/segundo", 1 / (2 * Math.PI)),
            )
        ),
        UnitCategory(
            "Datos", listOf(
                MeasureUnit("B", "byte", 1.0),
                MeasureUnit("bit", "bit", 0.125),
                MeasureUnit("kB", "kilobyte", 1e3),
                MeasureUnit("MB", "megabyte", 1e6),
                MeasureUnit("GB", "gigabyte", 1e9),
                MeasureUnit("TB", "terabyte", 1e12),
                MeasureUnit("KiB", "kibibyte", 1024.0),
                MeasureUnit("MiB", "mebibyte", 1024.0 * 1024),
                MeasureUnit("GiB", "gibibyte", 1024.0 * 1024 * 1024),
            )
        ),
    )
}

data class PhysicalConstant(val name: String, val symbol: String, val value: Double, val unit: String)

object PhysicalConstants {
    val all = listOf(
        PhysicalConstant("Velocidad de la luz", "c", 299792458.0, "m/s"),
        PhysicalConstant("Gravedad estándar", "g", 9.80665, "m/s²"),
        PhysicalConstant("Constante gravitacional", "G", 6.67430e-11, "N·m²/kg²"),
        PhysicalConstant("Constante de Planck", "h", 6.62607015e-34, "J·s"),
        PhysicalConstant("Planck reducida", "ħ", 1.054571817e-34, "J·s"),
        PhysicalConstant("Carga elemental", "e", 1.602176634e-19, "C"),
        PhysicalConstant("Masa del electrón", "mₑ", 9.1093837015e-31, "kg"),
        PhysicalConstant("Masa del protón", "mₚ", 1.67262192369e-27, "kg"),
        PhysicalConstant("Masa del neutrón", "mₙ", 1.67492749804e-27, "kg"),
        PhysicalConstant("Unidad de masa atómica", "u", 1.66053906660e-27, "kg"),
        PhysicalConstant("Número de Avogadro", "Nₐ", 6.02214076e23, "1/mol"),
        PhysicalConstant("Constante de Boltzmann", "k", 1.380649e-23, "J/K"),
        PhysicalConstant("Constante de los gases", "R", 8.314462618, "J/(mol·K)"),
        PhysicalConstant("Constante de Faraday", "F", 96485.33212, "C/mol"),
        PhysicalConstant("Permitividad del vacío", "ε₀", 8.8541878128e-12, "F/m"),
        PhysicalConstant("Permeabilidad del vacío", "μ₀", 1.25663706212e-6, "N/A²"),
        PhysicalConstant("Impedancia del vacío", "Z₀", 376.730313668, "Ω"),
        PhysicalConstant("Constante de Coulomb", "kₑ", 8.9875517923e9, "N·m²/C²"),
        PhysicalConstant("Stefan-Boltzmann", "σ", 5.670374419e-8, "W/(m²·K⁴)"),
        PhysicalConstant("Constante de Rydberg", "R∞", 10973731.568160, "1/m"),
        PhysicalConstant("Radio de Bohr", "a₀", 5.29177210903e-11, "m"),
        PhysicalConstant("Atmósfera estándar", "atm", 101325.0, "Pa"),
        PhysicalConstant("Cero absoluto", "T₀", -273.15, "°C"),
        PhysicalConstant("Velocidad del sonido (20 °C)", "vₛ", 343.0, "m/s"),
        PhysicalConstant("Densidad del agua (4 °C)", "ρ", 1000.0, "kg/m³"),
        PhysicalConstant("Masa de la Tierra", "M⊕", 5.9722e24, "kg"),
        PhysicalConstant("Radio de la Tierra", "R⊕", 6.371e6, "m"),
        PhysicalConstant("Razón áurea", "φ", 1.6180339887498949, ""),
    )
}
