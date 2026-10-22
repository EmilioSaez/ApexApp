# Design

## Context

Ver proposal.md. Este change se mergea sexto, tras `design-system-stitch`, `alarm-core`, `alarm-reliability`, `challenge-engine` y `alarm-sound`, y se construye sobre lo que ellos fijan:

- `alarm-core`: Room v2 con `Alarm.antiCheat: AntiCheatPolicy` en una columna `antiCheatJson TEXT NOT NULL DEFAULT '{}'` (kotlinx.serialization, `ignoreUnknownKeys`, defaults del dominio; vacío = permisivo); `RingActivity` (`showWhenLocked`, `turnScreenOn`); `DismissGate`; el editor de alarma.
- `alarm-reliability`: dueño único del servicio en primer plano (`AlarmRingingService`), de la interfaz `AlarmRinger`, del permiso de pantalla completa, del tiempo máximo de sonado y de la reprogramación/recuperación tras reinicio. Su `ReliabilityChecker` ya informa de pantalla completa y notificaciones.
- `challenge-engine`: sesión de retos y `ChallengeEventSink`; sustituye `DismissGate` para decidir cuándo se puede descartar.
- `alarm-sound`: reproducción en `USAGE_ALARM`, rampa hasta el techo del stream; no toca el volumen del stream.
- `AlarmFiringHandler` genera el `sessionId` y lo pasa a `RingActivity`.

minSdk 29, APK directo (sin Google Play).

## Goals / Non-Goals

**Goals:**
- Lockdown con huella mínima sobre código ajeno: solo puntos de enganche aditivos.
- Niveles por alarma con degradación explícita y visible.
- Lógica en Kotlin puro (resolutor de niveles, política de snooze, detector de integridad) probable con tests JVM.

**Non-Goals:**
- Servicio, actividad o canal de notificación propios; esquema Room; puerta de descarte (`DismissGate`); sonido; verificación anti-falsificación de retos (`challenges-qr-sensors`).
- Impedir apagado por hardware, modo seguro, recovery, ADB; accesibilidad y root.

## Decisions

**1. Esquema de `AntiCheatPolicy` (confirmado con `alarm-core`).** Propiedades con default, sin migración:

```
AntiCheatPolicy(
  level: AntiCheatLevel = OFF,            // JSON "level": "OFF" | "STANDARD" | "STRICT"
  snooze: SnoozePolicy = SnoozePolicy(),  // JSON "snooze"
)
SnoozePolicy(allowed: Boolean = false, minutes: Int = 5, maxCount: Int = 3)
```
Claves JSON: `level`, `snooze.allowed`, `snooze.minutes`, `snooze.maxCount`. Política ausente = `OFF` sin snooze. El editor preselecciona `STANDARD` en alarmas nuevas (la ausencia sigue siendo `OFF` para las existentes). El tiempo máximo de sonado **no** está aquí: lo posee `alarm-reliability`.

**2. Enganche al ciclo de vida del sonado: `RingingLifecycleListener`.** Interfaz `onStarted(session)`, `onEnded(session, reason)` con `reason ∈ {RESOLVED, SNOOZED, TIMED_OUT}` y `RingingSession(sessionId, alarm)`. Se inyecta como `Set<RingingLifecycleListener>` (Hilt multibinding) y el servicio de `alarm-reliability` lo invoca; es la única edición a su código y es aditiva (set vacío por defecto). Alternativa: observar un `StateFlow` del servicio; descartada porque `onEnded` con motivo es necesario y evitaría perder el cierre si el proceso muere. Pendiente de acordar la forma con `alarm-reliability` (ver Open Questions).

**3. Lockdown sobre `RingActivity`.** `LockdownController` (implementa el listener) aplica, según el nivel efectivo: inmersivo sticky y `BackHandler` vacío en `RingActivity`; `VolumeLock`; recuperación del primer plano; lock task en `STRICT` con device owner. `RingActivity` consulta el controlador en `onCreate`/`onStop` (alternativa a una actividad propia, descartada para no duplicar la pantalla de `alarm-core`). El controlador reaplica el lockdown cuando `RingActivity` se recrea con una sesión viva, lo que cubre la muerte del proceso y el reinicio si `alarm-reliability` restaura el sonado.

**4. Recuperación del primer plano.** Con `SYSTEM_ALERT_WINDOW` concedido la app puede relanzar `RingActivity` desde segundo plano (exención de arranque); un `onStop` con sesión viva y nivel activo reintenta con un pequeño retardo mediante el `RingScreenLauncher` de `alarm-reliability`. Sin el permiso, la notificación de pantalla completa de `alarm-reliability` queda como vía de retorno. El overlay no se usa como pantalla de bloqueo propia (Android 12+ bloquea toques sobre overlays no confiables y no cubre el menú de apagado).

**5. Resolución del fin del sonado.** `DismissGate` y la resolución de retos son de `challenge-engine`/`alarm-core`; este change no sustituye `DismissGate`. Cuando el reto se completa, el `AlarmResolver` de `alarm-reliability` termina el sonado y llama a `onEnded(RESOLVED)`, que libera el lockdown. Contrato resumido: el lockdown se libera **solo** por `onEnded`; no hay otra salida.

**6. Contrato con `alarm-sound`.** Este change es el único que modifica `STREAM_ALARM`. `VolumeLock.engage(sessionId)` guarda el volumen previo, fija el máximo y registra un `ContentObserver` sobre `Settings.System`; `release()` restaura. `alarm-sound` reproduce hasta el techo del stream y no toca el volumen. Si el lockdown está activo, la rampa llega al máximo real; con `OFF` queda en el volumen del usuario. Las teclas de volumen se consumen en `RingActivity.onKeyDown`.

**7. Niveles y resolución.** `AntiCheatResolver.effective(level, capabilities)` es una función pura:
- `OFF` → sin medidas.
- `STANDARD` → inmersivo + Atrás bloqueado + volumen forzado + recuperación de primer plano (esta última solo con overlay).
- `STRICT` → `STANDARD` + lock task si hay device owner (sin él, degrada a `STANDARD` y avisa).
`CapabilityChecker` (interfaz con fake) expone overlay, device admin y device owner. La pantalla de estado muestra estas tres y reutiliza el estado de `ReliabilityChecker` para pantalla completa y notificaciones, sin duplicar sus permisos.

**8. Lock task con device owner.** `DevicePolicyManager.setLockTaskPackages`, `setLockTaskFeatures(NONE)` y `startLockTask()`/`stopLockTask()`; `stopLockTask` se ejecuta en `onEnded` con cualquier motivo y, defensivamente, en `onDestroy` de `RingActivity` y al arrancar la app si no hay sesión viva. Provisión: `adb shell dpm set-device-owner app.luxion.apexapp/.admin.ApexDeviceAdminReceiver` en un dispositivo sin cuentas. Alternativa: screen pinning de usuario; requiere confirmación manual y se sale con Atrás+Recientes, no sirve.

**9. Device admin sin owner.** `DeviceAdminReceiver` solo para impedir la desinstalación directa. Sin `force-lock` ni wipe.

**10. Snooze.** `SnoozeController` programa un disparo único con `AlarmClockGateway` (reserva de un request code derivado del `alarmId`, sin tocar la próxima ocurrencia de las repetitivas), con extras `alarmId`, `sessionId` y `snoozeCount`. `AlarmFiringHandler.onFire(alarmId, sessionId?)` reutiliza el `sessionId` recibido en lugar de generar uno, con un cambio aditivo (parámetro opcional). Al posponer: `onEnded(SNOOZED)`, se detiene el sonado y se emite el evento. `RingActivity` muestra el botón solo si `allowed` y `count < maxCount`. Con snooze, el lockdown se libera entre sonados (el usuario eligió posponer). Alternativa: mantener el servicio vivo en silencio hasta el re-disparo; descartada por consumo y por complicar la fiabilidad.

**11. Eventos hacia `wake-stats`.** `alarm-reliability` ya publica `WakeEventRecorder` (con binding no-op hasta que `wake-stats` aporte el real) y los tipos `SNOOZED` y `SUSPICIOUS`; se emiten directamente, sin puerto propio. Los motivos de la sospecha no se persisten hasta que `wake-stats` ofrezca un campo para ellos; el tracker los calcula y queda cubierto por tests. `adaptive-difficulty` los lee a través de `wake-stats`.

**12. Detección de sesión sospechosa.** `SessionIntegrityTracker` (Kotlin puro, reloj inyectado) acumula por `sessionId`: `REPEATED_EXIT` (≥ 3 salidas de la pantalla de sonar), `VOLUME_TAMPER` (≥ 3 reaplicaciones de volumen), `PROCESS_INTERRUPTED` (sesión viva restaurada tras muerte/reinicio) y `TIMED_OUT` (terminó por tiempo máximo). Cualquier motivo marca la sesión; un `STRICT` degradado sin motivos no se marca. Los umbrales son constantes con valor por defecto, ajustables sin tocar los specs. La interrupción por muerte del proceso o reinicio se detecta con una marca persistida de sesión viva (`ActiveSessionStore`) que `ApexApplication` revisa al arrancar.

## Risks / Trade-offs

- [Edición a `AlarmRingingService` y `AlarmFiringHandler` de `alarm-reliability`] → Solo puntos aditivos (set de listeners, `sessionId` opcional); acordar la forma antes de implementar.
- [Relanzar la actividad falla sin overlay] → Pedir el permiso en `STANDARD`, avisar y degradar a la notificación de pantalla completa.
- [Device owner exige provisión por ADB y un dispositivo sin cuentas] → Opcional y solo para `STRICT`; documentar procedimiento y salida por ADB.
- [Quedar atrapado por un fallo] → `stopLockTask` en todos los caminos, tiempo máximo de `alarm-reliability` y comando ADB de rescate documentado.
- [Falsos positivos de «sospechosa» (p. ej. el sistema mata el proceso)] → La marca solo excluye de la dificultad adaptativa; los umbrales son holgados y los motivos quedan registrados.
- [Choque con `alarm-sound` por el volumen] → Una sola fuente de verdad documentada: este change fija el volumen, `alarm-sound` solo reproduce.
- [Permisos delicados incompatibles con Play] → Aceptado: APK directo.

## Migration Plan

Sin migración de base de datos: la política vive en `antiCheatJson` de `alarm-core`. Las alarmas existentes no tienen política y se leen como `OFF`. Despliegue por APK; el rollback es instalar la versión anterior, que ignora las claves nuevas.

## Open Questions

- Forma exacta del enganche en el servicio de `alarm-reliability` (set de `RingingLifecycleListener` frente a un flujo de estado) y del parámetro `sessionId` opcional en `AlarmFiringHandler.onFire`; se acuerda con sus autores antes de implementar.
- Dónde guarda `wake-stats` los motivos de `SUSPICIOUS` (el tipo de evento ya está acordado).
- Tipo de servicio en primer plano en Android 14: lo decide `alarm-reliability`.
