# Proposal

## Why

Cada alarma ya guarda un máximo de posposiciones (`SnoozePolicy.maxCount`, slider 1..10), pero no se puede dejar sin tope y la pantalla de alarma sonando no dice cuántas posposiciones quedan: el botón desaparece sin aviso al agotarse.

## What Changes

- Opción **«Sin límite»** además de 0..10: `maxCount = -1` (`SnoozePolicy.UNLIMITED`).
- La pantalla de alarma sonando muestra las **posposiciones restantes** junto al botón; al agotarse, el botón se oculta y se indica que se agotaron.
- Lógica pura en `SnoozePolicy` (`remaining`, `canSnooze`) usada por `SnoozeController` como única vía de posponer.
- El valor por defecto no cambia (3), por lo que las alarmas existentes se comportan igual.
- Textos en español e inglés.

## Capabilities

### Modified Capabilities
- `alarm-snooze`: el máximo admite «sin límite» y la pantalla de sonar informa de las posposiciones restantes.

## Impact

- **Código**: `domain/AlarmConfig.kt`, `anticheat/Snooze.kt`, `ui/ring`, `ui/anticheat/AntiCheatSection.kt`, validador de alarmas compartidas, strings es/en.
- **Datos**: sin migración de Room ni columna nueva; el valor vive en `antiCheatJson` (campo existente). Un JSON antiguo sigue deserializando igual.
- **Fuera de alcance**: cambiar el modelo de sesión o el contador (ya se reinicia al ser una sesión nueva por disparo).
