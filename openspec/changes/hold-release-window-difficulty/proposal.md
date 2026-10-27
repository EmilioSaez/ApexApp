# Proposal

## Why

La dificultad de «Mantener y soltar» alargaba el reto (mantenimiento más largo y menos rondas extra de margen). La dificultad no debe cambiar la duración del reto, sino lo justo que es el timing de soltar.

## What Changes

- El rango de mantenimiento antes de la señal (1,5–4 s) y el tope de rondas extra (3) pasan a ser constantes, iguales para todos los niveles.
- La tabla por nivel (1..5) solo conserva la ventana de acierto, que se estrecha al subir la dificultad (1200 → 500 ms).

## Capabilities

### Modified Capabilities
- `hold-release-challenge`: solo la ventana de acierto depende del nivel.

## Impact

- Código: `domain/challenge/holdrelease/` (`HoldReleaseLevel` solo lleva `windowMs`; `HoldReleaseGame` define las constantes) y `HoldReleaseTest`.
- Sin migración ni cambios de UI.
