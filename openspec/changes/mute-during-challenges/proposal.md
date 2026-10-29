# Proposal

## Why

Con retos activos, la alarma sigue sonando a pleno volumen mientras el usuario intenta resolverlos, lo que dificulta concentrarse. Se quiere una opción por alarma para silenciar el sonido mientras se resuelven los retos, sin que eso permita dormirse: si el usuario deja de interactuar, la alarma vuelve a sonar.

## What Changes

- Opción por alarma «Silenciar mientras resuelvo los retos» (desactivada por defecto) y un tiempo de inactividad configurable (por defecto 30 s).
- Al registrarse la primera interacción en la pantalla de retos (toques o progreso del reto) se silencia el sonido, la vibración y la voz de la alarma.
- Si pasa el tiempo de inactividad sin interacción, la alarma vuelve a sonar; con la siguiente interacción se vuelve a silenciar.
- Al superar todos los retos la alarma se detiene como hasta ahora; el silencio nunca la sustituye ni la prolonga (el tiempo máximo de sonado sigue contando).
- Máquina de estados de inactividad pura, con reloj inyectado, testeable sin Android.
- Campos nuevos en `Alarm`/`AlarmEntity` y migración Room explícita v5 → v6.
- Sección en el editor para activar la opción y elegir la inactividad.

## Capabilities

### New Capabilities
- `challenge-mute`: silencio del sonado durante los retos, con reactivación por inactividad.

### Modified Capabilities
- `alarm-storage`: dos columnas nuevas en `alarms` y migración v5 → v6.

## Impact

- Código: `domain/Alarm`, `data/local/AlarmEntity`, `ApexDatabase` (v6, `MIGRATION_5_6`), `data/Mappers`, `alarm/sound/AlarmSoundEngine` (`setMuted`), nuevo `alarm/mute/` (máquina, controlador), `ui/ring` (señal de interacción), `ui/editor` (sección).
- Datos: columnas `muteDuringChallenges` y `muteInactivitySeconds`; esquema `6.json`.
- Compatible con anti-trampas y bloqueo de volumen: no se toca el volumen del stream de alarma.
- Fuera de alcance: compartir la opción en alarmas compartidas (`sharing`); detectar movimiento de sensores como interacción.
