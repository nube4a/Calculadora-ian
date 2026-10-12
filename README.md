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
- Copiar resultado (botón o mantener pulsada la pantalla), vibración al tocar, tema claro/oscuro y
  colores dinámicos (Android 12+), diseño horizontal.

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
