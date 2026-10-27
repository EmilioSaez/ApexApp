# Design

## Context

Los retos implementan `Challenge` (fábrica sin estado) y `ChallengeInstance`, se registran en `ChallengeModule` con `@Binds @IntoSet` junto a su `ChallengeRenderer` y, si tienen parámetros, su `ChallengeParamsEditor`. `ChallengeSession` pide una instancia por reto y la da por superada cuando `check(answer)` devuelve verdadero; ante una respuesta falsa genera otra. Mates y movimiento no tienen parámetros propios (el objetivo sale del nivel); QR sí, en `ChallengeSpec.params` (JSON, `QrParams`). El anti-trampas por alarma actúa sobre la pantalla de sonar (lockdown) y `session-integrity` cuenta salidas; los retos no tienen un nivel propio.

Otros dos juegos (Tocar objetivos, Mantener y soltar) se desarrollan en paralelo y tocan `ChallengeModule` y `strings.xml`.

## Goals / Non-Goals

**Goals:**
- Un reto de ritmo jugable en ~1 min con una sola perilla por alarma (rondas).
- Lógica de ronda pura y determinista (sin reloj ni Compose) para poder probarla en JVM.
- Superficie compartida mínima para facilitar los merges.

**Non-Goals:**
- Audio/música, velocidad variable según la dificultad 1–5, estadísticas propias.
- Cambiar `ChallengeSession`, `ChallengeAnswer` más allá de lo indicado, Room o el editor.

## Decisions

**Parámetro `rounds` en `params`.** `{"rounds":N}` como `QrParams`, con `RhythmParams.parse/paramsFor`, rango 2..8 y defecto 4 (`ignoreUnknownKeys`; todo lo ilegible cae al defecto). Alternativa descartada: derivarlo de la dificultad de la alarma como Mates/Pasos; el requisito pide un parámetro propio del reto y la dificultad 1–5 es del conjunto de retos. 

**Constantes de una ronda.** 3 carriles, 10 notas, separación 1,3 s, caída de 1,5 s hasta la línea, ventana de impacto ±0,25 s. Ronda ≈ 14,5 s → 4 rondas ≈ 1 min. Todas en un único objeto `RhythmTuning` para calibrar.

**Lógica pura `RhythmRound`.** Recibe tiempo en ms desde el inicio de la ronda: `tap(lane, t)` y `advance(t)` (las notas pasadas de ventana son fallos). Estado: notas acertadas, fallos, `Cleared` / `Restart`. Cada nota se resuelve una sola vez. A 3 fallos devuelve `Restart`. El UI marca el tiempo con `withFrameMillis`; la lógica nunca lee el reloj.

**Regla de fallo.** Fallo = nota perdida o toque en carril sin nota en ventana; 3 fallos reinician solo la ronda con patrón nuevo (rondas superadas conservadas). Sin rondas extra: mantiene la duración acotada. Documentada en el spec.

**Patrones aleatorios.** `RhythmPattern.generate(random)` elige carril por nota con `Random` inyectado (el de la sesión) y evita repetir el mismo carril más de 3 veces seguidas. Se regenera en cada reinicio.

**Estado de rondas en la instancia/UI.** `RhythmInstance` guarda `rounds` y el prompt (`"rhythm:$rounds"`); el progreso vive en el estado de Compose (`remember(instance)`) igual que Math. Al superar la última ronda el renderer llama a `onAnswer(ChallengeAnswer.GameVerified(type))`, la variante común que introduce `touch-games-c9`; `check` solo acepta esa respuesta de su tipo.

**Disponibilidad y anti-trampas.** `availability()` siempre `Available`. Sin nivel propio: el renderer recibe el nivel efectivo (plumbing de `touch-games-c9`) y alimenta `TouchPlausibility(level)` con cada toque de carril (tiempo, carril, posición). Un toque que el validador descarta no llega a `RhythmRound.tap`. La detección de «ritmo demasiado regular» es del validador compartido, no se duplica aquí; la marca `SYNTHETIC_INPUT` en `STRICT` también. Si el renderer sale de composición, la sesión lo regenera y empieza de cero.

**Integración mínima.** Tres `@Binds @IntoSet` al final del bloque de retos en `ChallengeModule`; editor de rondas como paso `-`/`+` con texto «Rondas: N» en un único composable; strings en un bloque contiguo y comentado en `values` (es) y `values-en`. Sin nuevos botones en `ChallengesSection`: el tipo aparece solo por el registro.

## Risks / Trade-offs

- **Dependencia del commit compartido**: renderer y `check` no compilan hasta integrar `touch-games-c9`; la lógica pura sí. Si cambia la API de `TouchPlausibility`, solo afecta al renderer.
- **Sin escala por dificultad**: alarmas con dificultad 5 no tienen un ritmo más duro; es decisión de producto (una sola perilla).
- **Reloj de la UI**: la precisión depende de `withFrameMillis`; la ventana de ±0,25 s tolera jank. Con la pantalla en segundo plano el juego se detiene y reanuda por composición.
- **Fallos acumulados sin tope de reintentos**: el usuario puede reiniciar una ronda indefinidamente; es coherente con los demás retos (no hay tope), y el lockdown/tiempo máximo de sonado limitan la sesión.
