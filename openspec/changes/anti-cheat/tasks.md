# Tasks

## 1. Política y niveles

- [x] 1.1 Añadir `AntiCheatLevel`, `SnoozePolicy` y las propiedades `level`/`snooze` (con defaults) a `AntiCheatPolicy` de `alarm-core`; verificar con tests de serialización (JSON vacío, claves ausentes, claves desconocidas) y que no hay cambios de esquema Room.
- [x] 1.2 Implementar `AntiCheatResolver.effective(level, capabilities)` como función pura; verificar con tests de todos los niveles y combinaciones de capacidades.
- [x] 1.3 Implementar `CapabilityChecker` (overlay, device admin, device owner) tras una interfaz con fake; verificar con tests unitarios sobre el fake.

## 2. Puertos y contratos

- [ ] 2.1 Definir `RingingLifecycleListener`/`RingingSession`/`EndReason` y engancharlos en `AlarmRingingService` como `Set` de listeners (vacío por defecto); verificar con un test del servicio de que se invocan `onStarted` y `onEnded` con el motivo correcto.
- [x] 2.2 Añadir el parámetro opcional `sessionId` a `AlarmFiringHandler.onFire` y a `AlarmReceiver`; verificar con un test de que se genera uno nuevo si falta y se reutiliza si llega.
- [x] 2.3 Emitir `SNOOZED` y `SUSPICIOUS` directamente con el `WakeEventRecorder` que aporta `alarm-reliability` (binding no-op hasta `wake-stats`); verificar con tests que usan un recorder falso.

## 3. Lockdown y volumen

- [x] 3.1 Implementar `LockdownController` (listener) que aplica las medidas por nivel efectivo y las libera en `onEnded`; verificar con tests unitarios sobre fakes de cada medida.
- [ ] 3.2 Aplicar en `RingActivity` el modo inmersivo sticky, el bloqueo de Atrás y la consulta al controlador; verificar en emulador con la pantalla bloqueada y desbloqueada.
- [ ] 3.3 Implementar la recuperación del primer plano tras Home/Recientes (relanzado con overlay; sin él, solo la notificación de pantalla completa); verificar manualmente con y sin el permiso.
- [ ] 3.4 Implementar `VolumeLock` (guardar, máximo, `ContentObserver`, restaurar) tras una interfaz de audio con fake y consumir las teclas de volumen en `RingActivity`; verificar con tests unitarios y manualmente que el volumen no baja y se restaura.
- [ ] 3.5 Verificar la integración con `alarm-sound`: la rampa llega al máximo con lockdown y respeta el volumen del usuario con `OFF`; probar en emulador.
- [ ] 3.6 Reaplicar el lockdown cuando `RingActivity` se recrea con sesión viva; verificar matando el proceso con `adb shell am force-stop` durante el sonado.

## 4. Resistencia a manipulación

- [ ] 4.1 Declarar `SYSTEM_ALERT_WINDOW` y el receiver de device admin con su XML de políticas en el manifiesto; verificar que la app compila e instala.
- [ ] 4.2 Implementar `ApexDeviceAdminReceiver` y el flujo de activación para impedir la desinstalación; verificar manualmente que desinstalar exige desactivar antes.
- [ ] 4.3 Implementar el lock task para `STRICT` con device owner (`setLockTaskPackages`, `setLockTaskFeatures`, `startLockTask`/`stopLockTask` en `onEnded`, `onDestroy` y arranque sin sesión); verificar en emulador provisionado con `dpm set-device-owner` y con liberación por fin, snooze y tiempo máximo.
- [ ] 4.4 Documentar en `docs/anti-cheat.md` la provisión del device owner, la salida por ADB, los permisos y los límites no superables; verificar que los comandos documentados funcionan tal como se escriben.

## 5. Editor y pantalla de estado

- [ ] 5.1 Añadir al editor de alarma el selector de nivel (preselecciona `STANDARD` en nuevas) y el aviso de capacidades que faltan al guardar; verificar con tests de UI.
- [ ] 5.2 Crear la pantalla de estado de capacidades (overlay, device admin, device owner, límites de volumen y de plataforma) reutilizando `ReliabilityChecker` para pantalla completa y notificaciones; verificar con un test de UI y manualmente que cada botón abre el ajuste correcto.

## 6. Snooze

- [ ] 6.1 Añadir los controles de snooze al editor (permitido, minutos, máximo); verificar con tests de UI y de persistencia.
- [x] 6.2 Implementar `SnoozeController` (re-disparo único con el mismo `sessionId` y contador, sin tocar la siguiente ocurrencia repetitiva) y el botón de posponer en `RingActivity` según política y contador; verificar con tests unitarios con reloj y gateway falsos y manualmente en emulador.
- [x] 6.3 Emitir `Snoozed` por `AntiCheatEventSink` y `onEnded(SNOOZED)`; verificar con un test del sink con un fake.

## 7. Integridad de sesión

- [x] 7.1 Implementar `SessionIntegrityTracker` (salidas, volumen, interrupción de proceso, tiempo máximo) (la interrupción por muerte del proceso se detecta con `ActiveSessionStore` en `LockdownController`); verificar con tests unitarios con reloj falso, incluido el STRICT degradado sin marca.
- [x] 7.2 Conectar el tracker a `LockdownController`, `VolumeLock` y la restauración de sesión y emitir `SessionFlagged` al terminar; verificar con un test de integración con un fake del sink.

## 8. Integración

- [ ] 8.1 Probar en emulador el flujo completo por nivel (`OFF`, `STANDARD`, `STRICT` con y sin device owner) con Home, Atrás, volumen, matar el proceso, snooze y tiempo máximo; anotar el resultado en `docs/anti-cheat.md`.
- [ ] 8.2 Ejecutar `./gradlew test` y `./gradlew connectedAndroidTest` y verificar que pasan.
