## Context

El disparo (`AlarmReceiver` → `AlarmFiringHandler.onFire`) crea una sesión, arranca `AlarmRingingService` y reprograma o desactiva la alarma. El fin de una sesión se notifica con `RingingLifecycleListener.onEnded(session, reason)`. El snooze ya reprograma con `setAlarmClock`. Los permisos de notificaciones y alarmas exactas ya se piden y vigilan (`AlarmPermissions`, `ReliabilityChecker`).

## Goals / Non-Goals

**Goals:** comprobar que el usuario sigue despierto tras apagar la alarma; fiable ante Doze y reinicio; lógica pura testeable.
**Non-Goals:** plazos configurables, verificar la alarma de prueba, retos nuevos.

## Decisions

1. **Plazos fijos**: verificación a los **5 min** de resolver y **2 min** para confirmar (`AwakeVerificationPolicy`). 2 min da margen para ver la notificación sin dejar tiempo de volver a dormirse. Constantes en un único sitio.
2. **Repetición: sí**. Cada resolución de una alarma con la opción activa programa otra verificación; el bucle acaba solo al confirmar. Es lo coherente con el objetivo, y la confirmación siempre es posible porque el nuevo sonado vuelve a exigir los retos.
3. **`setAlarmClock` para ambas fases** (aviso y plazo), con `PendingIntent` propio hacia `AwakeVerificationReceiver` (acción + data URI `apexapp://verify/<alarmId>/<fase>`). `setExactAndAllowWhileIdle` descartado: en Doze se limita a ~1 disparo cada 9 min, incompatible con un plazo de 2 min. Coste: el sistema puede mostrar la verificación como «próxima alarma» durante esos minutos.
4. **Estado persistido** (`AwakeVerificationStore`, `SharedPreferences` como el contador de snooze): por alarma, `sessionId` de origen, fase (`PROMPT` | `CONFIRM`) y `dueAt`. Es la fuente de verdad: los receivers comprueban que la verificación sigue pendiente (idempotencia) y tras reinicio se reprograma desde él. Una verificación vencida hace más de 15 min al restaurar (móvil apagado mucho tiempo) se descarta; si no, se programa para ya.
5. **Máquina de estados en `AwakeVerifier`** (puro, con puertos `AwakeVerificationGateway`, `AwakeVerificationNotifier`, `AwakeVerificationStore`, `RingAgain`):
   - `onResolved(session)` → si `alarm.awakeVerification` y no es la prueba: guarda `PROMPT` y programa a +5 min.
   - `onPrompt(alarmId)` → si la alarma sigue existiendo con la opción: muestra la notificación, guarda `CONFIRM` y programa el plazo a +2 min. Si no, descarta. Si las notificaciones están bloqueadas no se puede confirmar, así que se **omite** la verificación en vez de forzar un sonado imposible de evitar (el aviso de fiabilidad existente ya señala las notificaciones bloqueadas).
   - `onConfirm(alarmId)` → borra estado, cancela plazo y notificación, registra `VERIFIED`.
   - `onDeadline(alarmId)` → si sigue en `CONFIRM`: cancela la notificación, registra `VERIFICATION_FAILED` y vuelve a sonar.
   - `cancel(alarmId)` → borra estado, plazo y notificación (borrado, desactivación explícita, opción quitada o nuevo disparo regular de la misma alarma).
6. **Volver a sonar = `AlarmFiringHandler.ringAgain(alarmId)`**: sesión nueva con evento «sonó» y el mismo arranque que un disparo normal (servicio, respaldo a notificación), pero **sin** reprogramar ni desactivar la alarma y sin exigir `enabled` (una alarma puntual ya se desactivó al sonar). Los retos son todos los de la alarma porque la sesión nueva los construye igual que cualquier disparo. Si esa alarma ya está sonando, no hace nada.
7. **«Desactivada» y alarmas puntuales**: una alarma puntual queda `enabled=false` al sonar, así que `enabled` no sirve como criterio de cancelación. Se cancela en `AlarmController.setEnabled(false)` (acción explícita), `delete` y `save` sin la opción; al vencer cada fase se vuelve a comprobar que la alarma existe y conserva la opción.
8. **Log**: `VERIFIED` y `VERIFICATION_FAILED` se añaden a la **sesión original** (la resuelta), no a una sesión nueva, para no crear sesiones fantasma. `groupSessions` los ignora, así que no alteran estados ni estadísticas; el nuevo sonado es una sesión normal con su `RANG`/`COMPLETED`. El tipo se guarda como texto, sin cambio de esquema.
9. **Datos**: `alarms.awakeVerificationEnabled INTEGER NOT NULL DEFAULT 0`, campo `Alarm.awakeVerification: Boolean = false`. Columna propia (no JSON) por ser un flag de primer nivel. Versión de BD **8**, `MIGRATION_7_8`; conflicto de numeración en el proposal.
10. **UI**: `AwakeVerificationEditorSection` (`EditorSection`, sin tocar el layout del editor): interruptor con texto explicativo (5 min / 2 min) y aviso si las notificaciones están bloqueadas. Notificación en canal propio `awake_verification` (importancia alta) con la acción «Estoy despierto».

## Risks / Trade-offs

- Un disparo regular de la misma alarma con una verificación pendiente la cancela (`AlarmReceiver`); la recuperación de una alarma perdida (`rescheduleAll`) no lo hace y, en el peor caso, la verificación vieja se resuelve sola al vencer.
- Numeración de esquema pendiente de renumerar al fusionar.
