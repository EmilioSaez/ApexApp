# Proposal

## Why

El nivel anti-trampas `STRICT` solo aportaba lock task (exigía ser propietario del dispositivo, solo concedible por ADB) y la detección de entrada táctil sintética por regularidad. Se retira para simplificar la política a `OFF` y `STANDARD`.

## What Changes

- **BREAKING** `AntiCheatLevel` pasa a `OFF`/`STANDARD`. Las alarmas guardadas o compartidas con `STRICT` se leen como `STANDARD` (no como `OFF`) para no perder protección.
- Se elimina la opción «Estricto» del editor, de la pantalla de importación y sus cadenas (es/en), incluido el aviso de propietario ausente.
- Se elimina el lock task (`Protection.lockTask`, `LockTaskPolicy`) y su liberación.
- Se elimina la regla de regularidad sintética de `TouchPlausibility` y el motivo de sospecha `SYNTHETIC_INPUT` con su plumbing (`TouchGuard`, `LockdownController.syntheticInput`).
- `STANDARD`, `OFF`, el resto de motivos de sospecha y la plausibilidad de toques no sintética no cambian.

## Capabilities

### Modified Capabilities
- `anti-cheat-policy`, `alarm-lockdown`, `tamper-resistance`, `session-integrity`.

## Impact

- Código: `anticheat/`, `domain/AlarmConfig.kt`, `domain/challenge/touch/TouchPlausibility.kt`, `ui/anticheat`, `ui/challenge`, `ui/ring`, `di/AntiCheatModule`, strings y tests.
- Sin migración de esquema (el nivel se guarda como JSON).
- Fuera de alcance: la capacidad «propietario del dispositivo» y su pantalla de estado siguen como están.
