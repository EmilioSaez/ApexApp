# Proposal

## Why

La dificultad de la alarma no afectaba al reto «Ritmo». Debe hacerlo sin alargar ni acortar el reto: lo que cambia es la velocidad de las notas.

## What Changes

- Tabla por nivel (1..5) con el tempo (BPM): a más dificultad, las notas caen más rápido y la música sube de tempo. El nivel 3 conserva el ritmo previo (120 BPM).
- La duración de la ronda es la misma en todos los niveles (~7,5 s): a mayor tempo caben más notas.
- El número de rondas sigue siendo un parámetro por alarma (2..8) y no depende de la dificultad.

## Capabilities

### Modified Capabilities
- `rhythm-challenge`: la velocidad de las notas depende de la dificultad.

## Impact

- Código: `domain/challenge/rhythm/` (`RhythmTuning.pace`, `RhythmPace`, `RhythmPattern`), `ui/challenge/rhythm/` (`RhythmMusic`, `RhythmUi`) y sus tests.
- Sin migración ni cambios de UI: los params guardados (`{"rounds":N}`) siguen siendo válidos.
