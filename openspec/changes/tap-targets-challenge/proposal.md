# Proposal

## Why

Los retos actuales exigen cálculo, moverse o escanear un código; ninguno es un reto de reflejos sobre la pantalla, que despierta la atención sin permisos ni sensores y funciona en cualquier dispositivo. Este change añade el primero de una familia de juegos táctiles: **Tocar objetivos**. Ritmo y Mantener y soltar se proponen en changes propios, en paralelo.

## What Changes

- Nuevo reto **Tocar objetivos**: en cada ronda aparecen varios círculos en posiciones aleatorias y hay que tocarlos todos antes de que desaparezcan.
- **Escala con la dificultad 1–5** como Mates (círculos por ronda, tamaño y tiempo, en una tabla única), también con la dificultad adaptativa.
- Un único parámetro propio por alarma: el **número de rondas** (por defecto ~1 min de juego), guardado en `ChallengeSpec.params`.
- Editor del parámetro en la sección «Retos» (visible solo con el tipo habilitado y la alarma en modo «Con retos»).
- **Penalización por fallo**: una ronda fallida no cuenta y añade una ronda extra (con tope); el progreso nunca se reinicia por completo.
- **Anti-trampas** según el nivel de la alarma (`OFF`/`STANDARD`/`STRICT`): validación de plausibilidad de los toques; en `STRICT`, 3 toques descartados marcan la sesión como sospechosa (la alarma sigue apagándose). No se crea un nivel propio.
- Aleatoriedad por sesión: posiciones y tiempos salen del `Random` de la sesión, sin semillas fijas.
- Textos en español e inglés, como los retos actuales.

## Capabilities

### New Capabilities
- `tap-targets-challenge`: reto de tocar círculos aleatorios por rondas, con penalización por fallo y plausibilidad de toques según el nivel anti-trampas.

### Modified Capabilities
- `challenge-editor`: el editor de parámetros por tipo incluye el número de rondas del reto Tocar objetivos.
- `session-integrity`: nuevo motivo de sospecha por entrada táctil sintética en `STRICT`.
- `wake-event-log`: esas sesiones se registran y excluyen como sospechosas.

## Impact

- **Código**: nuevo `domain/challenge/touch/` (lógica pura, `Challenge` y parámetros), renderer y editor en `ui/challenge/`, registro en `ChallengeModule`, etiquetas y strings (es/en), `ChallengeAnswer.GameVerified`. Las piezas compartidas con los otros dos juegos (`TouchPlausibility`, plumbing del nivel, `SYNTHETIC_INPUT`, `GameVerified`) van en un commit aparte.
- **Archivos compartidos con Ritmo y Mantener y soltar** (para facilitar merges, el cambio en ellos es de una línea por sitio): `ChallengeModule` (3 `@Binds`), `ChallengeAnswer` (1 variante), `ChallengeLabels`/`ImportScreen` (1 rama de etiqueta), `strings.xml` de `values` y `values-en` (bloque propio al final).
- **Sin** nuevas dependencias, permisos ni cambios de Room o de `minSdk`.
- **Fuera de alcance**: Ritmo, Mantener y soltar, refactors del motor.
