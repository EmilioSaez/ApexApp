# Proposal

## Why

ApexApp existe para obligar a despertar mediante retos, pero una alarma que se puede abandonar con Home/Atrás, silenciar con las teclas de volumen o posponer sin límite hace los retos opcionales. Los changes `alarm-core` (pantalla de sonar y política `AntiCheatPolicy` en `Alarm`), `alarm-reliability` (servicio en primer plano y `AlarmRinger`) y `challenge-engine` (retos) dan la base; falta la capa que impide salir de ella. Android no permite bloquear del todo el apagado ni los ajustes sin ser propietario del dispositivo, así que hay que definir niveles, degradar con elegancia y documentar los límites.

## What Changes

- Modo kiosko sobre la pantalla de sonar de `alarm-core` (`RingActivity`): inmersivo sticky, Atrás bloqueado y recuperación del primer plano si el usuario pulsa Home o Recientes. No hay actividad ni servicio propios: el lockdown se engancha al ciclo de vida del servicio de `alarm-reliability`.
- Volumen de alarma forzado al máximo mientras suena y reaplicado si se baja (teclas consumidas y observador de volumen). `alarm-sound` reproduce; este change es el único que toca el volumen del stream.
- Resistencia al apagado, cierre de la app, desinstalación y ajustes por capacidades: sin privilegios (inmersivo, overlay para relanzar la pantalla, device admin anti-desinstalación) y con device owner (lock task real y menú de apagado suprimido, solo para `STRICT`).
- Política anti-trampas por alarma, guardada dentro de `AntiCheatPolicy` (JSON de `alarm-core`, **sin columna ni migración Room propias**): nivel `OFF`/`STANDARD`/`STRICT` y política de snooze. Selector en el editor y aviso de permisos que faltan.
- **Snooze** (posponer) como capacidad propia: configurable por alarma (permitido, minutos, máximo de veces), desactivado por defecto, reutilizando el `sessionId` de la sesión de despertar y emitiendo el evento a `wake-stats`.
- **Sesiones sospechosas**: se marcan con motivos (intentos de salida repetidos, volumen manipulado, muerte del proceso o reinicio durante el sonado, salida por tiempo máximo) y se emiten a `wake-stats` para que `adaptive-difficulty` las excluya del ajuste.
- Pantalla de estado de capacidades (overlay, device admin, device owner, límites) que reutiliza el estado de fiabilidad de `alarm-reliability` para pantalla completa y notificaciones.
- Contratos fijados: `RingingLifecycleListener` (extensión aditiva del servicio de `alarm-reliability`), `AntiCheatEventSink` (hacia `wake-stats`) y `VolumeLock` (frente a `alarm-sound`). Ver design.md.
- Fuera de alcance: verificación anti-falsificación de cada reto (`challenges-qr-sensors`), lógica de retos y puerta de descarte (`challenge-engine`/`DismissGate`), reproducción de sonido, servicio en primer plano, fiabilidad de programación y pantalla de sonar base (`alarm-reliability`/`alarm-core`), esquema Room (v2 lo fija `alarm-core`) y diseño visual (`design-system-stitch`).
- Descartado por riesgo: servicio de accesibilidad y root.

## Capabilities

### New Capabilities
- `alarm-lockdown`: kiosko sobre la pantalla de sonar: inmersivo, sin salida por Atrás/Home y recuperación del primer plano, atado al ciclo de vida del sonado.
- `alarm-volume-lock`: forzado y reaplicado del volumen de alarma mientras suena.
- `tamper-resistance`: medidas best-effort contra apagado, cierre de la app, desinstalación y ajustes, con detección de capacidades y degradación documentada.
- `anti-cheat-policy`: nivel anti-trampas por alarma dentro de `AntiCheatPolicy`, nivel efectivo según capacidades y pantalla de estado.
- `alarm-snooze`: posponer una alarma con límites por alarma, misma sesión de despertar y evento de snooze.
- `session-integrity`: detección y marcado de sesiones sospechosas y su emisión a `wake-stats`.

### Modified Capabilities
<!-- Ninguna: no se tocan specs existentes. El almacenamiento (`alarm-storage`) y el disparo/servicio (`alarm-scheduling`, `alarm-ringing-service`) los modifican alarm-core y alarm-reliability; este change solo se engancha por extensión. -->

## Impact

- Código: `RingActivity` y editor de alarma (de `alarm-core`), `AlarmRingingService` y `AlarmFiringHandler` (de `alarm-reliability`: solo puntos de enganche aditivos), `Alarm.antiCheat`/`AntiCheatPolicy` (contenido, no esquema), nueva pantalla de estado, `AndroidManifest.xml` (`SYSTEM_ALERT_WINDOW`, receiver de device admin; full-screen, FGS y wake lock ya los declara `alarm-reliability`).
- Orden de merge acordado: design-system-stitch, alarm-core, alarm-reliability, challenge-engine, alarm-sound, **anti-cheat**, wake-stats, adaptive-difficulty, challenges-qr-sensors, sharing. Este change depende de los cuatro anteriores y no depende de los siguientes; `wake-stats` y `adaptive-difficulty` se adaptan a los contratos de aquí.
- Permisos delicados: `SYSTEM_ALERT_WINDOW`, `BIND_DEVICE_ADMIN`, device owner vía `adb shell dpm set-device-owner` (aceptado para `STRICT`; no apto para Google Play). minSdk 29.
- Riesgos: el apagado por hardware, el modo seguro, recovery y ADB no se pueden bloquear; Android 12+ restringe toques sobre overlays; un lock task mal liberado deja el móvil atrapado (liberación en todos los caminos de salida).
