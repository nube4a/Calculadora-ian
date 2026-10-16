# Sistema de diseño — Calculadora IAN

Identidad propia: **azul eléctrico** para la acción principal y **coral** para borrar, sobre fondos
limpios (gris azulado claro / azul marino profundo). Formas **"squircle"** (cuadrados muy redondeados),
no círculos ni píldoras, para diferenciarse de otras calculadoras.

Código: `app/src/main/java/com/ian/calculadora/ui/theme/` (`Theme.kt`, `Type.kt`, `Tokens.kt`).

## Tokens

| Categoría | Tokens |
|---|---|
| Color de marca | `primary` azul (#2F6BFF claro / #7FA6FF oscuro), coral para AC y errores |
| Teclado | `CalcColors`: number, operator, function, action (AC), equals (degradado azul) |
| Tipografía | **Manrope** (variable, licencia OFL en `docs/fonts`), escala `AppTypography`; cifras tabulares (`TABULAR`) en todo número |
| Espaciado | `Space`: 4 · 8 · 12 · 16 · 24 · 32 dp |
| Radios | `Radius`: 12 · 18 · 24 · 32 dp |
| Formas | `Shapes.key` (30 %), `card` (24 dp), `field` (18 dp), `chip` (12 dp), `pill` |
| Elevación | Solo las teclas de cifras en modo claro (sombra suave azulada); el resto es plano con bordes de 1 dp |

## Componentes

| Componente | Uso | Notas |
|---|---|---|
| Tecla (`KeyButton`, `MathKeyCell`) | Calculadora y teclado matemático | Squircle; se hunde al tocar; `=` con degradado |
| Botón redondo (`RoundIcon`) | Barra superior | 44 dp, fondo tonal |
| Pastilla de modo (`ModeText`) | DEG / RAD / SCI / ENG | Resaltada en azul cuando no es el modo normal |
| Encabezado (`ToolScaffold`) | Todas las herramientas | Botón atrás redondo + título `headlineSmall` |
| Tarjeta (`SectionCard`) | Agrupar resultados | Superficie + borde 1 dp; título en VERSALITAS azul |
| Chips (`ChoiceChips`) | Elegir opción | Seleccionado = azul relleno |
| Control segmentado | Pestañas del teclado matemático | Contenedor pastilla; seleccionado azul |
| Editor de ecuaciones | Resolver paso a paso | Borde azul 2 dp, cursor azul, toque para mover el cursor |

## Reglas

- El **rojo/coral solo** significa borrar o error. Lo seleccionado siempre es azul.
- Todo número usa cifras tabulares para que no "baile" al escribir.
- Nada de colores sueltos en las pantallas: se usan los del tema.
