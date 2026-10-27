# Apex

App Android de alarmas (Kotlin, Jetpack Compose, Room, Hilt).

## Estructura de paquetes

Un solo módulo `:app`, paquete `app.luxion.apexapp`, organizado en capas:

| Paquete   | Contenido |
|-----------|-----------|
| `domain`  | Modelos (`Alarm`, `Sound`), interfaces de repositorio y `nextOccurrence` (función pura, sin Android). |
| `data`    | Room (`data.local`: entidades, DAOs, `ApexDatabase`), repositorios y mapeo entidad/dominio. |
| `alarm`   | Programación con `AlarmManager.setAlarmClock` (`AlarmScheduler`), `AlarmController`, receivers de disparo y de arranque, notificaciones y permisos. |
| `ui`      | Pantallas Compose, ViewModels (`StateFlow`), navegación tipada y tema. |
| `di`      | Módulos Hilt. |

El esquema de Room se exporta a `app/schemas/`. En `debug` hay un receiver para crear alarmas desde `adb`:

```
adb shell am broadcast -n app.luxion.apexapp/.debug.DebugAlarmReceiver --ei minutesFromNow 2 --es label Prueba
```

## Historial de despertar (wake-stats)

Cada vez que suena una alarma se abre una *sesión* (`sessionId`, UUID creado en `AlarmFiringHandler`) y los eventos se registran en `wake_events` (append-only; si se borra la alarma, `alarmId` pasa a nulo). Contrato en `domain`:

- `WakeEventRecorder.record(sessionId, type, alarmId?, timestamp, scheduledAt?, challengeType?, difficulty?)` (suspend). `WakeEventType`: `RANG`, `SNOOZED`, `CHALLENGE_FAILED`, `COMPLETED`, `MISSED` (alarma perdida, sin fallo de reto), `SUSPICIOUS`. `RANG`, `COMPLETED` y `MISSED` son idempotentes por sesión.
- `WakeHistoryRepository`: `observeSessions` / `sessions(from, to, challengeType?)`, `recentSessions(alarmId, limit, excludeSuspicious)` (más reciente primero) y `observeStreak()`.
- `WakeSession`: `snoozeCount`, `failCount`, `timeToWake` (desde que suena hasta `COMPLETED`, incluye snoozes), `completedAt`, `suspicious` y `status` (`PENDING`, `COMPLETED`, `NOT_COMPLETED`; sin completar tras 24 h = no completada).
- Reglas: puntual = completada ≤ 10 min tras la hora programada; un día cuenta para la racha si todas sus sesiones resueltas son puntuales y los días sin alarma no suman ni rompen.

## Tests

```
./gradlew test                      # unitarios (dominio, scheduler, receivers, ViewModels)
./gradlew connectedDebugAndroidTest # instrumentados: Room en memoria y Compose (requiere emulador/dispositivo)
./gradlew assembleDebug             # compilar
```
