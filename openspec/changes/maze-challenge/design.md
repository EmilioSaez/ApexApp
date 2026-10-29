# Design

## Context

El motor de retos define `Challenge` (fábrica sin estado) y `ChallengeInstance` (`prompt`, `check(answer)`, `keepOnFailure`); los tipos se registran vía Hilt `@IntoSet` en `ChallengeModule` junto con su `ChallengeRenderer` y `ChallengeParamsEditor`. Los retos de memoria y movimiento sacan su objetivo de una tabla por nivel e ignoran los `params` guardados. La sesión filtra los tipos desconocidos y recurre a `math` si falta uno. Ver proposal.md para la motivación.

## Goals / Non-Goals

**Goals:**
- Reto jugable offline, sin permisos y determinista en tests (semilla tomada del `Random` inyectado).
- Lógica pura y testeable: generación, validez del trazo, detección de pared y llegada, sin depender de Compose.
- Mismo patrón de registro, renderer y editor que los retos existentes.

**Non-Goals:**
- Sonido, vibración al chocar, temporizador o accesibilidad avanzada más allá de contentDescription.
- Cambiar el motor de sesión ni la política anti-trampas.

## Decisions

- **Backtracker iterativo con semilla**: `Maze.generate(cols, rows, seed)` usa `Random(seed)` y una pila explícita (sin recursión). Produce un árbol de expansión: sin ciclos y con exactamente un camino entre dos celdas, así que la solución es única y se puede comprobar en test (`cellCount - 1` pasos abiertos, todas las celdas alcanzables). Alternativas descartadas: Kruskal/Prim (pasillos más cortos y ramificados, menos legibles) y laberintos con ciclos (varias soluciones, más difíciles de validar).
- **Entrada y salida**: la entrada es la celda (0,0), donde se dibuja el icono de alarma; la salida es la celda más lejana de la entrada por BFS (la solución es el camino hasta ella), lo que garantiza un recorrido largo en cualquier semilla sin colocar la salida al azar.
- **Tabla única de tamaños** (`MazeLevels`): 4×5, 5×6, 6×7, 7×8, 7×9 (columnas × filas) para los niveles 1..5. Alternativa descartada: parámetro libre en `params`, que permitiría dejar la alarma en trivial.
- **Tocable en pantallas pequeñas**: celda mínima de 40 dp y pared de 4 dp, es decir, pasillo libre de 36 dp. El trazo sigue el centro del dedo, no su huella (~48 dp), de modo que quedan ±18 dp de margen para un pulso torpe. El nivel 5 mide 7×40 = 280 dp de ancho (pantalla de 320 dp menos 2×20 dp de margen) y 9×40 = 360 dp de alto, así que cabe sin escalar por debajo del mínimo. `MazeLayout.cellDp` toma el menor de ancho/columnas y alto/filas, lo acota a 40..64 dp y por tanto nunca baja de 40 dp; en pantallas grandes la celda crece hasta 64 dp. Un test comprueba que el tamaño máximo cabe en 280×360 dp.
- **Trazo por geometría continua en celdas**: `MazeTracker` trabaja con coordenadas en unidades de celda (independientes de la densidad). Cada movimiento se interpola en pasos de media pared como mucho, de modo que un gesto rápido no atraviesa una pared entre dos eventos táctiles. Un punto choca si cae dentro de una pared cerrada (rectángulo del segmento ensanchado media pared) o fuera del laberinto; los postes de las esquinas cuentan como pared.
- **Inicio del trazo**: solo vale apoyar el dedo dentro de la celda de inicio (el icono de alarma); apoyar en cualquier otro sitio se ignora sin fallo.
- **Al soltar el dedo antes de llegar**: el trazo se borra y el dedo debe volver a empezar desde el icono, **sin contar como intento fallido**. Razones: soltar no es un error de pulso sino un gesto (o un rozamiento accidental) y penalizarlo castigaría a un usuario dormido sin evidencia de fallo; reanudar el trazo desde donde quedó permitiría resolver el laberinto a trozos y rompería el requisito de línea continua. El reinicio es gratuito, pero el usuario no gana nada soltando: debe volver a recorrer todo el camino.
- **Pared = fallo y reinicio**: al chocar se borra el trazo y se emite una `MazeTrace` con las celdas recorridas hasta el choque; `check` la rechaza (no llega a la salida) y la sesión cuenta el fallo. Así el fallo pasa por el mismo camino que en los demás retos y alimenta la dificultad adaptativa.
- **Verificación local con respuesta propia**: `MazeTrace(type, cells)` en archivo aparte (`MazeAnswer.kt`) para no tocar `Challenge.kt`; `check` exige empezar en la entrada, acabar en la salida y que cada paso entre celdas consecutivas esté abierto. Volver sobre el propio trazo (retroceder) es válido mientras la línea siga siendo continua.
- **Fallo = mismo laberinto** (`keepOnFailure = true`): regenerar tras cada choque cambiaría el tamaño efectivo de la prueba y desorientaría al usuario a mitad de un gesto. Como todos los laberintos de un nivel tienen el mismo tamaño, repetir el mismo no facilita el reto de forma significativa. El estado del trazo vive en el renderer.
- **Anti-trampas**: la UI pasa el pointer por `TouchGuard`/`TouchPlausibility` en el apoyo inicial (origen táctil y ventana no tapada); con nivel `OFF` acepta todo. No se aplica el intervalo mínimo entre pulsaciones a los puntos del movimiento, que no son pulsaciones. Como la UI vive dentro de un `verticalScroll`, el gesto consume los eventos del puntero para que el desplazamiento no robe el arrastre.
- **Identificador `maze`**: `ChallengeType("maze")`; los `params` son opacos y se ignoran.

## Risks / Trade-offs

- Un laberinto de 7×9 con pasillos de 36 dp exige pulso firme; es intencionado en el nivel más alto, y el fallo no es castigo acumulativo (se reintenta de inmediato).
- Pantallas de menos de 320 dp de ancho no son un objetivo: la celda no baja de 40 dp, así que el laberinto del nivel 5 podría recortarse en ese caso extremo.
- Con keepOnFailure el usuario puede memorizar la ruta tras un choque; es aceptable porque el reto sigue exigiendo ejecutar el trazo sin rozar paredes.
- Alarmas compartidas con este tipo abiertas en versiones antiguas quedan sin ese reto y recurren a mates.
