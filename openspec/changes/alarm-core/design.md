# Design

## Context

Tras `project-foundations` existen `AlarmController` (guardar/borrar/activar + programar), `AlarmClockScheduler` (`setAlarmClock`), `AlarmFiringHandler` → `AlarmNotifier` (notificación simple que abre `MainActivity`), `BootReceiver` y `HomeScreen` con lista e interruptor. `AlarmEditorScreen` es un placeholder y `AlarmEntity` está en Room v1 (`exportSchema = true`). Hay otros 9 changes en paralelo; tres de ellos (challenge-engine, alarm-sound, anti-cheat) necesitan guardar configuración por alarma. Ver proposal.md para la motivación.

## Goals / Non-Goals

**Goals:**
- Un único salto de esquema (v1 → v2) que reserve todos los campos de extensión, para que los changes paralelos no generen migraciones en conflicto.
- Editor y borrado reutilizando `AlarmController`, sin duplicar lógica de programación.
- Pantalla de sonar mínima con un punto de extensión de descarte que challenge-engine/anti-cheat sustituyen.

**Non-Goals:**
- Semántica de retos, dificultad, audio, vibración, TTS o anti-trampa: solo se almacenan y se exponen.
- Servicio en primer plano, Doze/OEM, reintentos de fiabilidad (alarm-reliability).
- Snooze/posponer: decidido que va en anti-cheat.
- Foreground service y `AlarmRinger`: los posee alarm-reliability; alarm-sound y anti-cheat se enganchan a él.
- Selector de tono en el editor (alarm-sound) y rediseño visual (design-system-stitch).

## Decisions

1. **Campos de extensión como value objects del dominio con defaults.** `Alarm` gana `challenges: ChallengeConfig` (`difficulty: Int` y lista de `ChallengeSpec(type: String, params: String)`, con `params` JSON opaco por tipo que define challenge-engine), `soundConfig: SoundConfig` (`volume: Int = 100` como techo de ganancia del reproductor, `vibrate: Boolean = true`, `ttsEnabled: Boolean`, `ttsText: String?`, `rampSeconds: Int = 30`, `vibrationPattern: String = "steady"`, `speakTime: Boolean = false`, `speakRepeatSeconds: Int = 0`, todos con defaults) y `antiCheat: AntiCheatPolicy` (confirmado por anti-cheat: `level` "OFF"|"STANDARD"|"STRICT" = OFF, y `snooze` {`allowed`=false, `minutes`=5, `maxCount`=3}; ausente = OFF y sin snooze; el tiempo máximo de sesión es de alarm-reliability). Los ids de reto son `String` para que challenge-engine defina el catálogo sin tocar el esquema. La clave estable de sonido no va en `SoundConfig` ni en `alarms`: vive en el sonido (`SoundSource`/`SoundEntity.sourceType`, de alarm-sound); la FK `soundId` existente sigue referenciando la tabla de sonidos. *Alternativa:* añadir campos sueltos en cada change → rechazada por conflictos de migración.
2. **Persistencia: tres columnas JSON `NOT NULL` con default.** `challengesJson` (`{}` ), `soundJson` (`{}`) y `antiCheatJson` (`{}`), serializados con kotlinx.serialization (`ignoreUnknownKeys`, `encodeDefaults`, defaults del dominio para claves ausentes). Así alarm-sound, anti-cheat y challenge-engine añaden campos sin migrar. Contrapartida: no se consulta por esos campos en SQL (no hace falta). *Alternativa:* columnas escalares por campo → obligaría a migrar con cada campo nuevo.
3. **Migración manual `MIGRATION_1_2`** con `ALTER TABLE ... ADD COLUMN ... DEFAULT`, exportando `2.json` y probada con `MigrationTestHelper`. Nada de `fallbackToDestructiveMigration`.
4. **Pantalla de sonar = `RingActivity` propia** (`showWhenLocked`, `turnScreenOn`, `launchMode=singleTop`, fuera del NavHost principal) lanzada con `setFullScreenIntent` desde `AndroidAlarmNotifier`. Sin permiso `USE_FULL_SCREEN_INTENT` concedido (revocable en API 34+) queda la notificación heads-up cuyo `contentIntent` abre la misma actividad. *Alternativa:* lanzar la actividad directamente desde el receiver → bloqueada por las restricciones de inicio desde segundo plano.
5. **Punto de extensión de descarte:** interfaz `DismissGate` (`canDismiss(alarm): Boolean`, por defecto `true`) inyectada en `RingViewModel`. challenge-engine y anti-cheat la sustituyen por su implementación vía Hilt; la pantalla de sonar muestra el botón solo si la puerta lo permite.
6. **`AlarmFiringHandler` no cambia de contrato:** sigue notificando y reprogramando/desactivando; solo cambia el `AlarmNotifier`. Descartar retira la notificación y cierra la actividad (el estado ya se actualizó al disparar).
7. **Editor:** `AlarmEditorRoute(alarmId: Long? = null)` con `AlarmEditorViewModel` (carga por id, guarda vía `AlarmController`, traduce `ScheduleResult.PermissionMissing` al aviso existente). `TimePicker` y chips de días de M3; el editor deja un hueco (slot) para que anti-cheat añada el selector de nivel y los controles de snooze. Borrado en la lista con `Snackbar` y deshacer que reinserta la alarma conservando su `id`.
8. **`sessionId` por disparo:** `AlarmFiringHandler` genera un `sessionId` en cada disparo y lo pasa a `RingActivity` (extra del `PendingIntent`/full-screen intent) y a `RingViewModel`, para que challenge-engine, wake-stats y anti-cheat lo usen sin crearlo ellos.
9. **Propiedad de columnas y coordinación:** alarm-core es el único dueño de Room v2 y de los campos de `ChallengeConfig`, `SoundConfig` y `AntiCheatPolicy`. alarm-sound y anti-cheat añaden sus campos a estas clases (no columnas propias duplicadas) y los changes posteriores rebasan y usan v3+ en orden de merge, o solo tablas nuevas. Orden de merge: design-system-stitch, alarm-core, alarm-reliability, challenge-engine, alarm-sound, anti-cheat, ...
10. **minSdk 29:** el proyecto sube a minSdk 29 (hoy 28). Si design-system-stitch, que se mergea antes, no lo ha hecho al rebasar, se hace aquí.
11. **Ya cubierto, sin tocar:** reprogramación tras reinicio (`BootReceiver` + `rescheduleAll`), permisos y banners de Home.

## Risks / Trade-offs

- [Reservar campos que quizá no se usen como se espera] → solo escalares simples y JSON tolerante; los changes propietarios pueden añadir claves sin migrar.
- [Conflicto de versión de Room con otros changes que también migren] → este change fija v2 y debe mergearse primero; el resto rebasa sobre ella.
- [Full-screen intent denegado en API 34+] → degradación a heads-up; la fiabilidad avanzada es de alarm-reliability.
- [Deshacer reinserta con el mismo `id`] → `save` con `id != 0` actualiza/inserta según Room; verificar con test (ver tareas 3.1).
- [Sin sonido hasta alarm-sound] → la alarma solo muestra pantalla/notificación; estado intermedio aceptado.

## Migration Plan

Subir `ApexDatabase` a v2 con `MIGRATION_1_2` y esquema exportado. Rollback: revertir el commit; una base v2 no baja a v1, aceptable al no haber release publicada.

## Open Questions

- Confirmación de alarm-sound y anti-cheat sobre los campos de decisión 1; cualquier campo extra se añade como clave JSON sin cambiar el esquema.
