---
name: design-taste-compose
description: >-
  Dirección de arte anti-genérico para pantallas de ApexApp (Jetpack Compose + Material 3). Úsalo al
  crear, rediseñar, pulir o revisar cualquier pantalla, componente o tema de la UI: cuando se pida que
  se vea "diseñada", "premium", "menos genérica" o "menos hecha por IA", o antes de tocar
  `ui/theme`, `@Composable` de pantalla, colores, tipografía, espaciado o animaciones.
---

# design-taste-compose

Adaptación a Android (Jetpack Compose + Material 3) de [taste-skill](https://github.com/Leonxlnx/taste-skill)
(MIT, Leonxlnx). La original es para web (HTML/CSS/React/Tailwind) y usa scripts con navegador; aquí se
conservan los principios y se traducen a Compose. No hay scripts: la verificación es compilar, ver
`@Preview` y mirar la app en el emulador.

Una pantalla generada por un modelo es la media de todas las que ha visto: `Scaffold` + `TopAppBar`
genérica, una lista de `Card` iguales con icono en círculo tintado, color primario por defecto, nada
decidido. Este skill sustituye los valores por defecto por decisiones.

## Flujo

1. **Lee el proyecto**: `ui/theme/` (Color, Type, Theme), componentes existentes y pantallas vecinas.
   Reutiliza el tema y los componentes antes de crear otros. No introduzcas librerías nuevas sin pedirlo.
2. **Decide el tema una vez**, no por pantalla: esquema de color, tipografía, formas, espaciado. Todo
   vive en `ui/theme` y se consume vía `MaterialTheme`, nunca con valores sueltos en las pantallas.
3. **Resuelve la idea de la pantalla**: qué pregunta responde, cuál es la acción principal. Esa acción
   y el dato clave van primero y más grandes; el resto, subordinado.
4. **Construye** con las reglas de abajo y comprueba con la lista final.

## Color

- Define un `ColorScheme` propio (`lightColorScheme` / `darkColorScheme`); no entregues el morado/violeta
  por defecto de M3 ni indigo+degradado.
- **El color va en los datos y en la acción principal, no en el cromo.** Superficies neutras
  (`surface`, `surfaceContainer*`), un solo acento (`primary`) y colores semánticos para estado.
- Separa niveles con **escalones de tonalidad** (`surfaceContainerLow/High`), no con sombras
  (`elevation`) ni bordes por todas partes.
- Sin degradados decorativos, sin glows, sin texto con degradado.
- Un cambio de valor se colorea como **bueno o malo**, no como sube o baja.
- Soporta modo oscuro con esquema propio (no invertir); respeta `dynamicColor` solo si se decide
  explícitamente. Contraste mínimo WCAG AA (4.5:1 texto, 3:1 componentes).
- Acceso siempre vía `MaterialTheme.colorScheme.*`, nunca `Color(0xFF…)` en pantallas.

## Tipografía

- Define `Typography` en el tema, con **pocos tamaños, bien separados** (display, título, cuerpo, etiqueta).
  Evita una escala de nueve tamaños casi iguales: si dos se parecen, que sean el mismo.
- El contraste de tamaño es la palanca principal; el peso no. Un título grande en `Normal`/`Medium`
  suele verse más seguro que en `Bold`.
- Tipo grande: interlineado ajustado (`lineHeight` ≈ 1.0–1.1× el tamaño) y `letterSpacing` negativo
  leve; comprueba que no choquen descendentes y ascendentes.
- Máximo dos familias. Cargarlas como recursos de fuente (`res/font`) con `FontFamily`; no dejar la
  fuente del sistema como decisión por omisión si el diseño pide carácter.
- Cifras tabulares/alineadas a la derecha en datos; sin mayúsculas diminutas con tracking por todas
  partes ni micro-etiquetas decorativas.
- Texto en `sp` (respeta el tamaño de fuente del usuario); el layout debe aguantar `fontScale` 1.3+.

## Layout y formas

- Espaciado desde una escala corta (4/8/12/16/24/32/48 dp) definida como constantes del tema; márgenes
  de pantalla coherentes. El espacio y el tono estructuran, no las líneas.
- **Una sola decisión de radio** (`Shapes` en el tema): cuadrado o muy redondeado, no 12 dp medio para todo.
- **Sin rejilla de tarjetas idénticas con icono en cuadrado/círculo tintado.** Si una lista es lista,
  usa filas (`ListItem`) con alineación y separación por espacio; usa tarjetas solo cuando agrupen algo
  con sentido propio.
- Varía la composición entre secciones; no todo centrado. Ancla a la izquierda y deja un único
  momento centrado.
- KPIs como **tipografía** (cifra grande + etiqueta), no tarjetas con icono ni sparkline.
- Sin cabecera "Hola de nuevo 👋". La pantalla responde su pregunta principal arriba.
- Respeta `WindowInsets` / edge-to-edge, áreas táctiles ≥ 48 dp, y adapta a `WindowSizeClass`
  (compact/medium/expanded) en lugar de estirar el layout de móvil.
- Estados honestos: vacío (mantiene su marco con mensaje útil), carga, error; ceros como ceros,
  faltantes como guion gris.

## Iconos e imágenes

- Iconos solo si añaden significado; sin emoji como iconos ni viñetas. Usa `Icons`/vector drawables
  coherentes en trazo y tamaño.
- Imágenes reales del sujeto o nada; no rellenar con ilustraciones genéricas, ni gráficas falsas de
  barras grises como "dashboard". Sin datos inventados como prueba social.

## Movimiento

- Una **firma de movimiento**, no un fade-in en cada bloque. Usa `animate*AsState`,
  `AnimatedVisibility`, `updateTransition`, `SharedTransitionLayout` donde expliquen un cambio de estado
  o relación espacial.
- Duraciones cortas (150–300 ms) con easing de M3 (`FastOutSlowInEasing` / emphasized); sin rebotes
  ni bucles infinitos sin significado.
- Respeta la preferencia de animaciones reducidas del sistema.

## Texto de interfaz (copy)

- Etiquetas con el verbo de la acción ("Guardar sesión"), no "Empezar" / "Saber más".
- Sin lenguaje de marketing genérico (seamless, potencia, todo en uno…), sin lorem ipsum, sin
  "No es solo X, es Y".
- Todos los textos en `strings.xml` (respeta el idioma ya usado en el proyecto).

## Tells a evitar (resumen)

Degradados violeta/índigo · paneles de vidrio/blur · sombras suaves bajo cada tarjeta · tres tarjetas
iguales con icono · emoji como icono · píldoras "Nuevo/Beta" · puntos de estado decorativos ·
mono-etiquetas y numeración 01/02/03 decorativas · líneas finas por todas partes · fondo crema +
terracota · titular de frase completa en 3–4 líneas · prueba social inventada · fade-up en todo.

## Verificación antes de entregar

- [ ] Compila: `./gradlew assembleDebug`; tests: `./gradlew test`.
- [ ] Colores, tipografía y formas salen de `MaterialTheme`; no hay literales sueltos en pantallas.
- [ ] `@Preview` en claro/oscuro y con `fontScale` grande para el componente nuevo.
- [ ] Contraste AA, áreas táctiles ≥ 48 dp, `contentDescription` en iconos/imágenes con significado.
- [ ] Estados vacío/carga/error cubiertos.
- [ ] Ningún tell de la lista; la pantalla tiene una acción principal clara y un dato clave primero.
- [ ] Revisada en emulador (`shogun emulator start`) en compact y, si aplica, tablet/plegable.
