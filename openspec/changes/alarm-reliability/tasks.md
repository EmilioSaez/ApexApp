## 1. Manifiesto y permisos

- [x] 1.1 Declarar `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` con `maxSdkVersion="32"`, `USE_FULL_SCREEN_INTENT`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK` y `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` en `AndroidManifest.xml`
- [x] 1.2 Registrar `AlarmRingingService` (tipo `mediaPlayback`, no exportado) y ampliar los intent-filters del receiver de reprogramación
- [x] 1.3 Actualizar `AndroidAlarmPermissions`/`AndroidAlarmClockGateway` para que `canScheduleExact` considere `USE_EXACT_ALARM` (API 33+)

## 2. Servicio de sonado

- [x] 2.1 Crear `AlarmRingingService` con notificación en primer plano, canal `alarms` de importancia alta y `fullScreenIntent` (heads-up si `canUseFullScreenIntent()` es falso en API 34+)
- [x] 2.2 Definir el contrato de enganche: `AlarmRinger`, `RingScreenLauncher`, `AlarmResolver.resolve(sessionId, reason: EndReason)`, `EndReason {RESOLVED, SNOOZED, TIMED_OUT}` y `RingingSession(sessionId, alarm)`, con `SystemToneRinger` por defecto y bindings Hilt
- [x] 2.2c Añadir `RingingLifecycleListener {onStarted, onEnded}` como `Set` multibinding Hilt (vacío por defecto) y `sessionId: String? = null` en `AlarmFiringHandler.onFire` y en el extra de `AlarmReceiver`
- [x] 2.2b Documentar el contrato para alarm-sound, anti-cheat y alarm-core (sin servicios propios)
- [x] 2.3 Evitar sonados duplicados de una misma alarma y aplicar la lógica de `AlarmFiringHandler` una sola vez por disparo
- [x] 2.4 Implementar el tiempo máximo de sonado y la acción de resolver que detiene audio, notificación y servicio
- [x] 2.5 Cambiar `AlarmReceiver` para iniciar el servicio, capturando `ForegroundServiceStartNotAllowedException` con respaldo a notificación de alta prioridad
- [x] 2.6 Tests unitarios de `AlarmFiringHandler`/servicio para disparo puntual, repetitivo y duplicado

## 3. Reprogramación

- [x] 3.1 Ampliar `BootReceiver` (o crear `RescheduleReceiver`) para `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED` y `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`
- [x] 3.1b Rebasar sobre alarm-core (Room v2) antes de tocar la base de datos
- [x] 3.2 Añadir tabla propia `alarm_fire_state` (`alarmId`, `scheduledAt`, `lastFiredAt`; migración Room v2->v3 aditiva) sin modificar `AlarmEntity`
- [x] 3.3 Implementar la detección de alarmas perdidas en `AlarmController.rescheduleAll` con ventana de tolerancia, emitiendo las perdidas como `record(type = MISSED, scheduledAt = ...)` a `WakeEventRecorder`
- [x] 3.3b Declarar `WakeEventType` y `WakeEventRecorder` en `app.luxion.apexapp.domain` con la firma acordada con wake-stats-b7 y `NoOpWakeEventRecorder` enlazado por Hilt
- [x] 3.4 Tests unitarios de `rescheduleAll` para cambio de hora/zona y alarmas dentro y fuera de ventana

## 4. Estado de fiabilidad

- [x] 4.1 Crear `ReliabilityChecker` y `ReliabilityState` con comprobaciones críticas y recomendadas (alarma exacta, notificaciones, canal no silenciado, pantalla completa, exclusión de batería)
- [x] 4.2 Comprobar el estado al abrir la app, en `onResume` y al activar una alarma, avisando de lo que falte sin bloquear
- [x] 4.3 Tests unitarios del cálculo de estado por versión de API

## 5. UI de configuración y avisos

- [x] 5.1 Crear la pantalla de configuración de fiabilidad con una fila por comprobación, explicación y acción que abre el ajuste del sistema
- [x] 5.2 Añadir la ruta al grafo de navegación y el aviso persistente en `HomeScreen`
- [x] 5.3 Refrescar el estado al volver a la app tras conceder un permiso
- [x] 5.4 Seguir `design-taste-compose` y el tema existente; coordinar con design-system-stitch

## 6. Guías por fabricante y prueba

- [x] 6.1 Crear la tabla de fabricantes (Xiaomi, Huawei, Oppo/Realme, Vivo, Samsung, OnePlus) con intents conocidos y textos de pasos, con respaldo a los ajustes de la aplicación
- [x] 6.2 Guardar la confirmación "ya lo hice" en preferencias y dejar de avisar
- [x] 6.3 Implementar la alarma de prueba por el camino real de disparo a pocos segundos
- [x] 6.4 Cadenas de texto en `strings.xml`

## 7. Verificación

- [x] 7.1 Ejecutar los tests unitarios e instrumentados existentes y los nuevos
- [x] 7.2 Probar manualmente en emulador (`shogun emulator start`): pantalla bloqueada, app cerrada, `adb shell dumpsys deviceidle force-idle`, reinicio y cambio de hora/zona
- [ ] 7.3 Probar en al menos un dispositivo físico con optimización de fabricante agresiva (pendiente manual para el usuario: requiere un móvil real con ROM agresiva)

<!-- Verificación 7.1/7.2 (emulador API 36): 27/27 tests instrumentados y 68 unitarios en verde; alarma con app cerrada, pantalla apagada: FGS mediaPlayback en primer plano, RingActivity en foco, audio USAGE_ALARM, descartar detiene audio/servicio/notificación; cambio de zona reprograma a la misma hora local; el reinicio reprograma. Doze profundo (force-idle) no se alcanzó en el emulador (se queda en INACTIVE por estar cargando) pero la alarma se concedió como setAlarmClock con exactAllowReason=policy_permission. Fallo encontrado y corregido: RingingRegistry no era @Singleton. -->
