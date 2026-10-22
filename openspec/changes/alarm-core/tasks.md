# Tasks

## 1. Modelo y persistencia (Room v2)

- [x] 1.0 Acordar con alarm-sound y anti-cheat los campos de `SoundConfig` y `AntiCheatPolicy`, y comprobar `minSdk = 29` en `app/build.gradle.kts` (subirlo si falta); verificar que el build compila
- [x] 1.1 Añadir `ChallengeConfig`, `SoundConfig` y `AntiCheatPolicy` (con defaults) a `domain/` y los campos correspondientes en `Alarm`; verificar con test unitario de defaults
- [x] 1.2 Ampliar `AlarmEntity` (columnas `challengesJson`, `soundJson` y `antiCheatJson`) y `Mappers.kt` con serialización tolerante a claves desconocidas; verificar con test de ida y vuelta dominio↔entidad
- [x] 1.3 Subir `ApexDatabase` a v2, escribir `MIGRATION_1_2` (tres columnas `TEXT NOT NULL DEFAULT '{}'`), registrarla en `DataModule` y exportar `2.json`; verificar con test instrumentado de migración (`MigrationTestHelper`) que las alarmas de v1 se conservan con defaults
- [x] 1.4 Actualizar `RepositoryTest`/`ApexDatabaseTest` para los campos nuevos y verificar que pasan

## 2. Editor de alarmas

- [x] 2.1 Crear `AlarmEditorViewModel` (carga por `alarmId`, guardado vía `AlarmController`, estado de permisos) y verificar con test unitario de crear, editar y alarma inactiva
- [x] 2.2 Implementar `AlarmEditorScreen` con `TimePicker`, selector de días y etiqueta, y `AlarmEditorRoute(alarmId)`; verificar con test Compose de guardar y de salir sin guardar
- [x] 2.3 Mostrar al guardar el tiempo restante hasta la próxima ocurrencia usando `nextOccurrence`; verificar con test unitario del formateo
- [x] 2.4 Mover los textos nuevos a `strings.xml`; verificar que no quedan cadenas fijas en las pantallas nuevas

## 3. Lista: editar y borrar

- [x] 3.1 Abrir el editor al pulsar una alarma y añadir `delete`/`undoDelete` a `HomeViewModel` (cancela y reinserta conservando `id`); verificar con `HomeViewModelTest`
- [x] 3.2 Añadir la acción de borrar con `Snackbar` de deshacer en `HomeScreen`; verificar con `HomeContentTest` (borrar y deshacer)

## 4. Pantalla de sonar

- [x] 4.0 Generar un `sessionId` en `AlarmFiringHandler` y pasarlo a `RingActivity`/`RingViewModel`; verificar con `AlarmFiringHandlerTest` que cada disparo lleva un id distinto y llega a la pantalla
- [x] 4.1 Definir `DismissGate` (por defecto permite descartar) y `RingViewModel` (carga la alarma, descarta y retira la notificación); verificar con test unitario
- [x] 4.2 Crear `RingActivity` + pantalla Compose (hora, etiqueta, descartar solo si la puerta lo permite) y declararla en el manifest con `showWhenLocked`/`turnScreenOn`; verificar con test Compose y arranque manual en emulador
- [x] 4.3 Cambiar `AndroidAlarmNotifier` a notificación con `setFullScreenIntent` y `contentIntent` hacia `RingActivity`, y añadir `USE_FULL_SCREEN_INTENT` al manifest; verificar en emulador con dispositivo bloqueado y desbloqueado
- [x] 4.4 Confirmar que `AlarmFiringHandlerTest` cubre alarma puntual, repetitiva y eliminada/inactiva, y que descartar no cancela la siguiente ocurrencia

## 5. Integración

- [ ] 5.1 Flujo completo en emulador (`shogun emulator start`): crear alarma, esperar disparo, ver pantalla de sonar, descartar, reiniciar y comprobar reprogramación; verificar `./gradlew test connectedAndroidTest` en verde
