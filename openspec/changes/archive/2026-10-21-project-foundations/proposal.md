# Proposal

## Why

ApexApp es todavía la plantilla vacía de Compose (un `Greeting` en `MainActivity`). Antes de construir las alarmas, los sonidos y las acciones al despertar hace falta una base común: arquitectura, persistencia, programación de alarmas y navegación. Fijarla ahora evita que cada feature posterior reinvente estas decisiones.

## What Changes

- Estructura en capas (`ui`, `domain`, `data`) con ViewModels y estado expuesto como `StateFlow`.
- Navegación Compose con una pantalla inicial y la tematización existente como punto de partida.
- Persistencia local con Room: entidades y repositorios para **Alarma** y **Sonido**.
- Inyección de dependencias con Hilt.
- Programación de alarmas exactas con `AlarmManager`: scheduler, `BroadcastReceiver` de disparo, receiver de arranque que reprograma y gestión de permisos (`SCHEDULE_EXACT_ALARM`, `POST_NOTIFICATIONS`).
- Estructura de tests: unitarios (dominio, repositorios con Room en memoria) e instrumentados.
- Catálogo de versiones (`libs.versions.toml`) ampliado con las nuevas dependencias.

No incluye: UI final de alarmas, selección/reproducción de sonidos ni acciones al despertar (cambios posteriores).

## Capabilities

### New Capabilities
- `alarm-storage`: modelo y persistencia local de alarmas y sonidos (crear, leer, actualizar, borrar, observar).
- `alarm-scheduling`: programación y cancelación de alarmas exactas, disparo a su hora y reprogramación tras reinicio, incluido el manejo de permisos necesarios.
- `app-shell`: contenedor de la app: navegación entre pantallas, tema y punto de entrada.

### Modified Capabilities
<!-- Ninguna: openspec/specs/ está vacío. -->

## Impact

- Código: `app/src/main/java/app/luxion/apexapp/` (nuevos paquetes `data`, `domain`, `ui`, `di`, `alarm`), `MainActivity.kt`, `AndroidManifest.xml` (permisos, receivers, `Application`).
- Build: `gradle/libs.versions.toml`, `build.gradle.kts` y `app/build.gradle.kts` (plugins KSP y Hilt, Room, Navigation Compose, lifecycle-viewmodel-compose, test).
- Sin impacto en APIs externas. `minSdk` 28 se mantiene.
