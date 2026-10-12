# Calculadora IAN

Calculadora científica completa para **Android**, hecha con Kotlin y Jetpack Compose (Material 3).

## Funciones

- **Básicas:** + − × ÷, %, ±, decimales, paréntesis (se cierran solos al calcular).
- **Científicas:** sin, cos, tan y sus inversas (sin⁻¹…), hiperbólicas (sinh, cosh, tanh e inversas),
  ln, log, log₂, eˣ, 10ˣ, x², x³, xʸ, √, ∛, ʸ√x, 1/x, |x|, n! (también decimales con la función gamma),
  nCr, nPr, mod, π, e, notación científica (EXP), número aleatorio (Rand).
- **Modos:** grados (DEG) o radianes (RAD); teclas **2nd** e **hyp** para las funciones secundarias.
- **Ans:** reutiliza el último resultado.
- **Memoria:** MC, MR, M+, M−, MS (se guarda al cerrar la app).
- **Historial:** hasta 500 cálculos guardados, agrupados por día, con búsqueda. Toca la expresión para
  editarla o el resultado para usarlo; copiar, borrar uno o borrar todo.
- **Vista previa** del resultado mientras escribes, multiplicación implícita (`2π`, `3(4)`),
  porcentajes como en el teléfono (`200 + 10% = 220`).
- **Cálculo en la calculadora:** integrales `∫(f,a,b)`, derivadas `d/dx(f,a)`, sumatorias `Σ(f,a,b)` y
  productorias `Π(f,a,b)` usando la variable `x`; también `floor`, `ceil`, `gcd`, `lcm`, `max`, `min`.
- **Formato NORM / SCI / ENG:** la notación de ingeniería muestra además el prefijo SI (4.7 m, 2.2 M).
- Copiar resultado (botón o mantener pulsada la pantalla), vibración al tocar, tema claro/oscuro y
  colores dinámicos (Android 12+), diseño horizontal.

## Herramientas de ingeniería (menú ☰)

- **Gráficas:** hasta 3 funciones a la vez, mover con el dedo, zoom con pellizco, tocar para leer
  f(x) y f′(x), y **Analizar** para marcar raíces, máximos, mínimos e intersecciones.
- **Ecuaciones:** cualquier ecuación f(x) = 0 (todas las raíces reales en un intervalo), polinomios
  de grado 2 a 6 con raíces complejas, y sistemas lineales de 2×2, 3×3 y 4×4 con determinante.
- **Conversor de unidades:** 17 categorías (longitud, área, volumen, masa, temperatura, presión,
  fuerza, energía, potencia, velocidad, caudal, torque, densidad, tiempo, ángulo, frecuencia, datos).
- **Programador:** HEX / DEC / OCT / BIN de 64 bits, AND, OR, XOR, NOT, NAND, NOR, desplazamientos
  y rotaciones, vista de bits.
- **Estadística:** media, mediana, moda, desviación estándar (población y muestra), varianza,
  cuartiles, coeficiente de variación; **regresión** lineal, cuadrática, exponencial, logarítmica y
  potencial con R², predicción y gráfica de la curva.
- **Constantes físicas** (CODATA): c, g, G, h, ħ, e, Nₐ, k, R, ε₀, μ₀ y más; tócalas para usarlas.

En los campos de texto de las herramientas puedes escribir con el teclado normal: `sqrt`, `pi`,
`asin`, `*`, `/`, `^`.

## Descargar el APK

Cada push compila la app en GitHub Actions: pestaña **Actions** → última ejecución de
“Compilar APK” → artefacto **calculadora-ian-apk**. Descomprímelo e instala `app-release.apk` en el teléfono
(hay que permitir “instalar apps de origen desconocido”).

## Compilar en tu PC

Abre la carpeta con Android Studio, o desde la terminal:

```bash
./gradlew testDebugUnitTest   # pruebas del motor matemático
./gradlew assembleDebug       # APK en app/build/outputs/apk/debug/
```

Requiere Android 8.0 (API 26) o superior.
