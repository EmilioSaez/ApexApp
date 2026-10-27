# Tasks

## 1. Dependencias y build

- [x] 1.1 Añadir a `libs.versions.toml` y a los `build.gradle.kts` KSP, Hilt, Room, Navigation Compose, lifecycle-viewmodel-compose, kotlinx-serialization y librerías de test; verificar con `./gradlew assembleDebug`
- [x] 1.2 Crear los paquetes `data`, `domain`, `ui`, `alarm`, `di` y la clase `Application` con `@HiltAndroidApp` registrada en el manifest; verificar que la app compila y arranca

## 2. Dominio y persistencia

- [x] 2.1 Definir modelos de dominio `Alarm` y `Sound` y la función pura de próxima ocurrencia; verificar con tests unitarios (alarma puntual, repetitiva, cambio de día, hora ya pasada)
- [x] 2.2 Implementar entidades, DAOs y base de datos Room con la clave foránea `soundId` `ON DELETE SET NULL` y esquema exportado; verificar con tests instrumentados en memoria (CRUD, flujo reactivo, borrado de sonido)
- [x] 2.3 Implementar `AlarmRepository` y `SoundRepository` con mapeo entidad/dominio y módulo Hilt; verificar con tests de repositorio

## 3. Programación de alarmas

- [x] 3.1 Implementar `AlarmScheduler` (interfaz + versión con `setAlarmClock`) para programar y cancelar; verificar con tests unitarios contra un `AlarmManager` falso
- [x] 3.2 Implementar `AlarmReceiver` con canal de notificación, reprogramación de repetitivas y desactivación de puntuales; declarar receiver y permisos en el manifest; verificar con test del flujo de disparo
- [x] 3.3 Implementar `BootReceiver` que reprograma las alarmas activas; verificar con test y simulando `BOOT_COMPLETED` con `adb`
- [x] 3.4 Implementar `AlarmPermissions` y los flujos de solicitud de `POST_NOTIFICATIONS` y alarmas exactas; verificar en emulador API 33+ con el permiso denegado y concedido

## 4. Shell de la app

- [x] 4.1 Configurar Navigation Compose con rutas tipadas y pantalla inicial (lista o estado vacío) con su ViewModel, sustituyendo `Greeting`; verificar con un test Compose de estado vacío y con alarmas
- [x] 4.2 Confirmar el tema claro/oscuro sobre las pantallas nuevas; verificar visualmente en ambos modos en el emulador

## 5. Integración

- [x] 5.1 Ejecutar `./gradlew test` y `./gradlew assembleDebug`, y una prueba manual en emulador: crear alarma vía repositorio de depuración, ver que suena y que sobrevive a un reinicio
- [x] 5.2 Documentar en `README.md` la estructura de paquetes y cómo ejecutar los tests
