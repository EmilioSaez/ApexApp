## Context

Ver proposal.md - Why. Estado actual observado:

- `AndroidAlarmClockGateway` ya usa `AlarmManager.setAlarmClock`, que está exento de Doze y se muestra como alarma en el sistema. Se mantiene.
- `AlarmReceiver` -> `AlarmFiringHandler.onFire` -> `AlarmNotifier.notify` publica solo una notificación; no hay servicio, ni full-screen intent, ni audio.
- `BootReceiver` solo escucha `BOOT_COMPLETED`. El manifiesto solo declara `SCHEDULE_EXACT_ALARM`, `POST_NOTIFICATIONS` y `RECEIVE_BOOT_COMPLETED`.
- `AlarmPermissions` expone `PermissionState` (notificaciones, alarma exacta) e intent a ajustes de alarma exacta.
- `minSdk` = 28 hoy (se espera 29 por decisión del proyecto), `targetSdk` = 36. Hilt para DI.
- Otros changes en paralelo: alarm-core (CRUD y pantalla de sonar), alarm-sound (audio). Este change no los implementa pero define el punto de enganche.

## Goals / Non-Goals

**Goals:**
- Que una alarma activa suene a su hora aunque la app esté cerrada, el dispositivo en Doze o tras reinicio/cambio de hora.
- Detectar y explicar al usuario lo que podría impedirlo.

**Non-Goals:**
- Reproducción de audio real, pantalla de sonar y lógica de retos (otros changes).
- Garantizar el comportamiento en ROMs que maten la app de forma agresiva: solo mitigar con guías y prueba.
- Distribución en Play Store (políticas de `USE_EXACT_ALARM`, FGS) no se optimizan.

## Decisions

1. **Mantener `setAlarmClock` como mecanismo de programación.** Es el más fiable (Doze-exempt, sube la app a lista blanca temporal para arrancar FGS). Alternativas: `setExactAndAllowWhileIdle` (sujeto a cuotas), WorkManager (no exacto). Descartadas.

2. **El receptor delega en un servicio en primer plano.** `AlarmReceiver` valida el id y llama `ContextCompat.startForegroundService` hacia `AlarmRingingService`; la lógica de `AlarmFiringHandler` (reprogramar o desactivar una sola vez) se ejecuta en el servicio/receptor, protegida por un conjunto de alarmas sonando para ignorar duplicados. Un `BroadcastReceiver` solo da ~10 s y no mantiene el proceso. Alternativa: actividad lanzada directamente desde el receptor (bloqueada por restricciones de inicio de actividades en segundo plano).

3. **Tipo de FGS y arranque.** Decidido: `mediaPlayback` (API 34+ obliga a declarar tipo), coherente con `USAGE_ALARM`. Se arranca desde el receptor del `AlarmClockInfo`, que el sistema exime de la restricción de FGS en segundo plano. Alternativa `specialUse` descartada.

4. **Dueño único del servicio y contrato de enganche.** Solo existe `AlarmRingingService`; ningún otro change crea servicios. Contrato (interfaces Hilt, estables):
   - **Audio:** `AlarmRinger { start(session: RingingSession); stop() }`. `SystemToneRinger` (tono `TYPE_ALARM`, `USAGE_ALARM`) es el binding por defecto para que nunca sea silenciosa; alarm-sound lo sustituye. anti-cheat puede envolverlo (p. ej. volumen forzado) sin tocar el servicio.
   - **Pantalla:** `RingScreenLauncher` / constante de ruta que el `fullScreenIntent` abre; alarm-core aporta la actividad de sonar y recibe `alarmId` y `sessionId`.
   - **Resolución:** `AlarmResolver.resolve(sessionId: String, reason: EndReason = RESOLVED)`; detiene audio, notificación y servicio y notifica a los listeners. `EndReason { RESOLVED, SNOOZED, TIMED_OUT }`: `TIMED_OUT` lo emite el servicio al agotar el tiempo máximo; `SNOOZED` lo emite anti-cheat al llamar a `resolve` (el snooze en sí es suyo y no se define aquí).
   - `RingingSession(sessionId: String, alarm: Alarm)`: el `sessionId` lo crea `AlarmFiringHandler` o se reutiliza si llega en `onFire(alarmId, sessionId: String? = null)` y en el extra del `AlarmReceiver`, de modo que el re-disparo de un snooze mantiene la sesión. La alarma de prueba genera su propio `sessionId`.
   - **Ciclo de vida:** `RingingLifecycleListener { onStarted(session: RingingSession); onEnded(session: RingingSession, reason: EndReason) }`, inyectado como `Set<RingingLifecycleListener>` (multibinding Hilt, vacío por defecto). El servicio llama `onStarted` al empezar el sonado y `onEnded` al terminar, una vez por sesión; un fallo de un listener no interrumpe el sonado.

5. **Notificación de alarma.** Canal `alarms` con `IMPORTANCE_HIGH`, categoría `CATEGORY_ALARM`, `fullScreenIntent` hacia `MainActivity` (provisionalmente) y luego la pantalla de sonar. Sin `USE_FULL_SCREEN_INTENT` concedido (API 34+, `NotificationManager.canUseFullScreenIntent()`), heads-up. Se reemplaza el uso actual de `AlarmNotifier` por el servicio; la interfaz `AlarmNotifier` se conserva para no romper tests y consumidores.

6. **Permisos.** Manifiesto: `USE_EXACT_ALARM` (API 33+), `SCHEDULE_EXACT_ALARM` con `android:maxSdkVersion="32"`, `USE_FULL_SCREEN_INTENT`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. `USE_EXACT_ALARM` es una concesión automática y no revocable; en API 31-32 sigue siendo `SCHEDULE_EXACT_ALARM` revocable y se escucha `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` está restringido en Play, pero la distribución es APK directo.

7. **Reprogramación.** Un único `RescheduleReceiver` (o ampliar `BootReceiver`) con acciones `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED` y el cambio de estado de alarma exacta, todas llamando a `AlarmController.rescheduleAll()`. Alarmas perdidas: `rescheduleAll` consulta `NextOccurrence` y, si la última ocurrencia pasó dentro de la ventana (por defecto 10 min) y no se registró disparo, lanza el sonado inmediato; si no, registra como perdida. El estado de último disparo se guarda en una **tabla propia** (`alarm_fire_state`: `alarmId`, `scheduledAt`, `lastFiredAt`) con migración Room aditiva v2 (alarm-core) -> v3, sin tocar `AlarmEntity`. Las alarmas perdidas se emiten a `WakeEventRecorder` (contrato de wake-stats-b7, paquete `app.luxion.apexapp.domain`): `suspend fun record(sessionId: String, type: WakeEventType, alarmId: Long? = null, timestamp: Long = now, scheduledAt: Long? = null, challengeType: String? = null, difficulty: Int? = null)`, con `WakeEventType { RANG, SNOOZED, CHALLENGE_FAILED, COMPLETED, MISSED, SUSPICIOUS }`. Una alarma perdida llama `record(sessionId = UUID nuevo, type = MISSED, alarmId = id, scheduledAt = hora que debió sonar)` con `challengeType` y `difficulty` nulos; no se usa `CHALLENGE_FAILED` para no contaminar las métricas de adaptive-difficulty. Como este change se fusiona antes, se declara esa misma interfaz y enum copiados tal cual, con una clase no-op (`NoOpWakeEventRecorder`) enlazada por Hilt que wake-stats sustituye sin cambios.

8. **Estado de fiabilidad.** `ReliabilityChecker` produce `ReliabilityState` (lista de `ReliabilityIssue` con severidad crítica/recomendada), ampliando `AlarmPermissions`/`PermissionState` en vez de duplicarlos. Se evalúa en `onResume` de Inicio y al activar alarmas. La UI es una pantalla `ReliabilitySetup` en el grafo de navegación y un banner en `HomeScreen`.

9. **Guías por fabricante.** Tabla estática `Manufacturer -> lista de intents de ajustes conocidos + texto de pasos`, probando cada intent con `resolveActivity` y cayendo a `ACTION_APPLICATION_DETAILS_SETTINGS`. La confirmación "ya lo hice" se guarda en preferencias (DataStore o SharedPreferences, lo que ya use el proyecto). Los intents de fabricante cambian entre ROMs; no se prometen.

10. **Alarma de prueba.** Reutiliza `AlarmClockGateway.setAlarmClock` con un id reservado (negativo/constante) a ~10 s para recorrer el camino real, sin persistir en Room.

## Risks / Trade-offs

- [ROMs que matan la app o bloquean autoarranque] -> guías por fabricante, alarma de prueba y exclusión de batería; no es verificable por API.
- [Ventana de tolerancia mal elegida: alarma sonando horas tarde o perdida silenciosa] -> ventana corta y configurable; registro de perdidas visible al usuario.
- [Conflictos con alarm-core sobre Room y `AlarmNotifier`] -> tabla propia en v3 tras rebasar sobre alarm-core; interfaces estables.
- [FGS tipo `mediaPlayback` sin audio activo puede ser cuestionado] -> el servicio siempre inicia al menos el tono por defecto.
- [Permiso de pantalla completa revocable por el usuario en API 34+] -> degradación a heads-up y aviso recomendado.
- [Llamadas a startForegroundService fuera de la exención lanzan `ForegroundServiceStartNotAllowedException`] -> capturar y caer a notificación de alta prioridad.

## Migration Plan

- Aditivo: nuevos permisos, servicio y receivers; migración Room v2->v3 aditiva (tabla nueva). Rebasar sobre alarm-core antes de implementar.
- Usuarios con alarmas ya programadas: `MY_PACKAGE_REPLACED` las reprograma al instalar el nuevo APK.
- Rollback: revertir el change; la programación con `setAlarmClock` previa sigue funcionando.

## Open Questions

- Tiempo máximo de sonado y ventana de tolerancia: valores por defecto propuestos 10 min y 10 min; ajustables sin cambiar specs.
- El número de versión de Room (v3) es provisional: se fija al rebasar según el orden de merge.

## Decisiones tomadas al implementar

- `LOCKED_BOOT_COMPLETED` no se escucha: Room vive en almacenamiento cifrado por credenciales, inaccesible antes del primer desbloqueo; `BOOT_COMPLETED` llega ya desbloqueado.
- `alarm_fire_state` guarda `scheduledAt` además de `lastFiredAt`: una alarma está perdida si `scheduledAt <= ahora` y `lastFiredAt < scheduledAt`. Sin esa hora programada no se distinguiría una alarma recién creada de una perdida.
- Una alarma puntual perdida fuera de ventana se desactiva (reprogramarla para el día siguiente la haría sonar sin que el usuario lo espere); una repetitiva se reprograma.
- `AlarmFiringHandler` es quien crea la sesión, inicia el servicio y aplica reprogramar/desactivar; con `sessionId` recibido (re-disparo de snooze) no reprograma ni desactiva. Si el sistema impide iniciar el servicio (p. ej. `BOOT_COMPLETED` con `mediaPlayback` en Android 15) cae a `AlarmNotifier` (canal `alarms`, con sonido).
- `AlarmScheduler.schedule/cancel` pasan a `suspend` para registrar la hora programada.
- Con varias alarmas sonando a la vez el servicio es único y la notificación en primer plano es la de la última; el audio sigue a la última sesión activa.
- `USE_EXACT_ALARM` hace que `canScheduleExactAlarms()` sea siempre verdadero desde API 33, por lo que no se requiere código adicional; `exactAlarmSettingsIntent()` devuelve `null` desde API 33.
