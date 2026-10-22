# Proposal

## Why

ApexApp obliga a despertar mediante retos, así que una alarma que no suena es un fallo crítico. Hoy el disparo solo publica una notificación desde un `BroadcastReceiver` (que el sistema puede matar en segundos), solo se reprograma tras `BOOT_COMPLETED` y nada guía al usuario con Doze, ahorro de batería ni optimizaciones de fabricante. La app se distribuye por APK directo a amigos, así que no hay Play Store que fije permisos ni políticas.

## What Changes

- Al dispararse una alarma se arranca un **servicio en primer plano** con notificación persistente y `fullScreenIntent`, que mantiene el proceso vivo y es el anfitrión del sonado. Este change es el **dueño único** del servicio (tipo `mediaPlayback`) y de la interfaz `AlarmRinger`; alarm-sound, anti-cheat y la pantalla de sonar (alarm-core) se enganchan mediante un contrato explícito (audio, pantalla, resolución) y no crean servicios propios.
- Permisos de alarma exacta: declarar `USE_EXACT_ALARM` (API 33+) y `SCHEDULE_EXACT_ALARM` (hasta API 32 con `maxSdkVersion`), y gestionar `USE_FULL_SCREEN_INTENT` (API 34+) y `POST_NOTIFICATIONS`.
- Reprogramación ampliada: además de reinicio, tras `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED` y al concederse el permiso de alarma exacta; recuperación de alarmas perdidas mientras el dispositivo estuvo apagado (dentro de una ventana de tolerancia), registrando las perdidas para wake-stats mediante `WakeEventRecorder`.
- Pantalla de **configuración de fiabilidad** con comprobación de estado y avisos guiados: permisos, exclusión de optimización de batería (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), canal de notificación no silenciado y guías por fabricante (Xiaomi, Huawei, Oppo/Realme, Vivo, Samsung, OnePlus).
- Aviso persistente en Inicio si falta algo crítico; comprobación al abrir la app y al activar una alarma; alarma de prueba.
- **Fuera de alcance**: CRUD y pantalla de sonar (alarm-core), selección y reproducción de sonido (alarm-sound; solo implementa `AlarmRinger`), snooze y modo estricto (anti-cheat), retos (challenge-engine, challenges-qr-sensors), anti-trampas (anti-cheat), diseño visual (design-system-stitch), estadísticas y compartir.

## Capabilities

### New Capabilities
- `alarm-ringing-service`: ciclo de vida del servicio en primer plano que mantiene viva la alarma al sonar, su notificación de pantalla completa y el contrato con el que otros módulos (sonido, pantalla de sonar, retos) se acoplan.
- `alarm-reliability-setup`: comprobación del estado de fiabilidad del dispositivo y guía al usuario para conceder permisos, excluir la app del ahorro de batería y ajustar optimizaciones de fabricante.

### Modified Capabilities
- `alarm-scheduling`: el disparo pasa a iniciar el servicio de sonado; la reprogramación cubre cambio de hora/zona, actualización de la app y alarmas perdidas; los permisos incluyen `USE_EXACT_ALARM` y se vuelven a comprobar tras cambios.

## Impact

- Código: `AndroidManifest.xml` (permisos, servicio, receivers), paquete `alarm/` (`AlarmReceiver`, `BootReceiver`, `AndroidAlarmNotifier`, `AlarmFiringHandler`, `AlarmPermissions`, nuevo `AlarmRingingService`), nueva UI de configuración y aviso en `HomeScreen`, navegación.
- Dependencias: ninguna nueva prevista; preferencias locales solo para recordar la confirmación de guías de fabricante.
- Coordinación: `AlarmFiringHandler` y `AlarmNotifier` son puntos de contacto con alarm-core y alarm-sound; se mantienen interfaces estables.
- Distribución: APK directo; `USE_EXACT_ALARM` está restringido por política de Play a apps de alarma, aceptable aquí.
- `minSdk` objetivo 29 (hoy el proyecto declara 28; el cambio de `minSdk` no es de este change, lo sube alarm-core, tarea 1.0; se asume aplicado). Orden de merge: design-system-stitch, alarm-core, **alarm-reliability**, challenge-engine, alarm-sound, anti-cheat, wake-stats...; este change se rebasa sobre alarm-core (Room v2).
