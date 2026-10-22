# Proposal

## Why

`project-foundations` dejó la base: persistencia en Room, programación exacta con `setAlarmClock`, reprogramación tras reinicio y una lista de alarmas con interruptor. Pero el usuario aún no puede crear, editar ni borrar alarmas (el editor es un placeholder) y, al llegar la hora, solo ve una notificación: no hay pantalla de sonar. Además, otros changes en paralelo (challenge-engine, alarm-sound, anti-cheat) necesitan campos en el modelo de alarma; si cada uno migra Room por su cuenta habrá conflictos de esquema.

## What Changes

- Editor de alarmas funcional (crear y editar): hora, días de repetición, etiqueta y activo.
- Borrado de alarmas desde la lista (con deshacer) y apertura del editor al pulsar una alarma.
- Pantalla de sonar (`RingActivity`): se muestra sobre la pantalla de bloqueo al dispararse la alarma, con hora/etiqueta y acción para descartar. La notificación pasa a ser de pantalla completa.
- El modelo `Alarm` incorpora **campos de extensión con valores por defecto** (configuración de retos y dificultad, sonido/volumen/vibración/TTS, política anti-trampa). Este change solo los almacena y los expone; su comportamiento lo definen sus changes propietarios.
- Migración de Room v1 → v2 que añade esas columnas de una sola vez.
- Ya cubierto y **no se toca**: persistencia básica, `AlarmClockScheduler`, `BootReceiver`/`rescheduleAll`, permisos de alarmas exactas y notificaciones, lista con interruptor.

## Capabilities

### New Capabilities
- `alarm-management`: crear, editar, borrar y activar/desactivar alarmas desde la UI.
- `alarm-ring`: pantalla de sonar mostrada al dispararse una alarma y descarte de la misma.

### Modified Capabilities
- `alarm-storage`: la alarma guarda además campos de extensión (retos, sonido, anti-trampa) con valores por defecto y migración sin pérdida de datos.
- `alarm-scheduling`: el disparo presenta la pantalla de sonar (notificación de pantalla completa) en lugar de una simple notificación.

## Impact

- Código: `domain/Alarm.kt`, `data/local/AlarmEntity.kt`, `ApexDatabase` (v2 + `Migration`), `Mappers.kt`, `ui/editor/*`, `ui/home/*`, `ui/navigation/AppNavigation.kt`, `alarm/AndroidAlarmNotifier.kt`, nueva `ui/ring/*`, `AndroidManifest.xml` (`USE_FULL_SCREEN_INTENT`, `RingActivity`).
- Esquema Room exportado `2.json`; tests de migración.
- Fuera de alcance (otros changes): lógica de retos y dificultad (challenge-engine, challenges-qr-sensors, adaptive-difficulty), reproducción de audio/vibración/TTS (alarm-sound), bloqueo de descarte (anti-cheat), fiabilidad en Doze/OEM y servicio en primer plano (alarm-reliability), tema/diseño (design-system-stitch), estadísticas y compartir.
