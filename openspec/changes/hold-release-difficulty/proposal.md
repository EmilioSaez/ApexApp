# Proposal

## Why

«Mantener y soltar» solo escalaba la ventana de acierto con la dificultad; el rango de mantenimiento y las rondas extra eran fijos, a diferencia de los demás juegos (p. ej. «Tocar círculos»), cuya exigencia sale de una tabla por nivel.

## What Changes

- Tabla única de calibración por nivel (1..5) con: rango de mantenimiento antes de la señal, ventana de acierto y tope de rondas extra. El nivel 3 conserva los valores previos.
- Al subir la dificultad: mantenimiento más largo, ventana más estrecha y menos rondas extra de margen.
- El número de rondas sigue siendo un parámetro por alarma (2..6), como en los demás juegos con rondas.

## Capabilities

### Modified Capabilities
- `hold-release-challenge`: señal aleatoria, ventana de acierto y penalización por fallo dependen del nivel.

## Impact

- Código: `domain/challenge/holdrelease/` (`HoldReleaseTuning` sustituye a `HoldReleaseTiming`; `HoldReleaseGame` recibe el nivel de exigencia) y `HoldReleaseTest`.
- Sin migración ni cambios de UI: los params guardados (`{"rounds":N}`) siguen siendo válidos.
