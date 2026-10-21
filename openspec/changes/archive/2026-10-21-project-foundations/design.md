# Design

## Context

La app es la plantilla de Compose: un único módulo `:app`, paquete `app.luxion.apexapp`, `minSdk` 28, AGP 9.2.1, Kotlin 2.2.10, sin persistencia, DI ni navegación. Ver proposal.md - Why.

## Goals / Non-Goals

**Goals:**
- Una estructura de paquetes y dependencias sobre la que añadir features sin reabrir decisiones de arquitectura.
- Alarmas que suenan a su hora con la app cerrada y tras reiniciar.

**Non-Goals:**
- Multi-módulo Gradle (un solo módulo basta por ahora).
- UI final de alarmas, reproducción y selección de sonidos, acciones al despertar.

## Decisions

- **Capas en un módulo, por paquetes**: `ui` (pantallas + ViewModels), `domain` (modelos y casos de uso simples), `data` (Room, repositorios), `alarm` (scheduler y receivers), `di`. Alternativa: multi-módulo; descartada por sobrecoste actual.
- **Room + Flow** para persistencia: DAO devuelve `Flow<List<AlarmEntity>>`. Los días de repetición se guardan como máscara de bits. `AlarmEntity.soundId` es clave foránea nullable con `ON DELETE SET NULL` (cumple el spec de sonido eliminado). Alternativa: DataStore; no encaja con datos relacionales.
- **Hilt** para DI (con KSP). Alternativas: Koin (sin codegen, pero errores en runtime) y DI manual (más código al crecer).
- **Estado de UI** con `StateFlow` en ViewModels y `collectAsStateWithLifecycle`.
- **Navegación**: Navigation Compose con rutas tipadas (`@Serializable`), una sola `Activity`.
- **Programación**: `AlarmManager.setAlarmClock()` por alarma activa. Es exacta, resiste Doze y muestra el icono de alarma; requiere `SCHEDULE_EXACT_ALARM` (API 31+, comprobada con `canScheduleExactAlarms()`). Alternativa: `setExactAndAllowWhileIdle`, sin la prioridad de reloj de alarma.
- **Cálculo de próxima ocurrencia** como función pura en `domain`, con `java.time` y reloj inyectable, para testearla sin Android.
- **Disparo**: `AlarmReceiver` publica una notificación en un canal de alta importancia y reprograma (o desactiva si es puntual). `BootReceiver` (`BOOT_COMPLETED`) reprograma todas las activas. El sonido y las acciones al despertar quedan para cambios posteriores; aquí la notificación usa el sonido por defecto.
- **Permisos**: clase `AlarmPermissions` consulta el estado; la UI solicita `POST_NOTIFICATIONS` (API 33+) y lleva a `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
- **Tests**: JUnit para dominio y scheduler (con `AlarmManager` tras una interfaz); Room en memoria para DAOs/repositorios (instrumentados); un test Compose del arranque.

## Risks / Trade-offs

- [Compatibilidad Hilt/Room/KSP con AGP 9.2.1] → Verificar el build en la primera tarea de dependencias; si falla, fijar versiones compatibles o recurrir a DI manual.
- [`SCHEDULE_EXACT_ALARM` puede ser revocado por el usuario] → Comprobar al activar y al reprogramar, y degradar con aviso.
- [Fabricantes que matan receivers en segundo plano] → Fuera de alcance; anotar para un cambio posterior de fiabilidad.
- [Esquema de Room inestable en esta fase] → Exportar el esquema y evitar migraciones destructivas fuera de debug.
