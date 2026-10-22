# Proposal

## Why

ApexApp se distribuye por APK directo entre amigos y el reto de despertar funciona mejor en grupo. Hoy cada usuario configura sus alarmas a mano y no hay forma de enseñar los logros. Compartir una alarma (con su reto, sonido y política anti-trampa) y presumir de racha por chat/redes facilita la adopción entre amigos sin necesitar backend.

## What Changes

- **Compartir configuración de alarma** con un amigo por tres canales: enlace/deep link, código QR y archivo (`.apexalarm`), todos con el mismo payload.
- **Formato de payload versionado** (`schemaVersion`) que cubre la alarma base (hora, días, etiqueta), el reto (challenge-engine), el sonido (alarm-sound) y la política anti-trampa (anti-cheat). Los campos de otros changes son opcionales y se ignoran de forma segura si la versión local no los entiende.
- **Importación segura**: el payload nunca se confía; se valida (tamaño máximo, versión, rangos, longitudes, listas blancas de tipos de reto/sonido), se muestra una **vista previa** y solo se crea la alarma tras aceptar. La alarma importada entra **desactivada** y nunca se programa sin confirmación.
- **Tarjeta de racha**: imagen generada (Bitmap/Canvas) con los datos de racha de `wake-stats`, compartible vía share sheet y `FileProvider`.
- Nuevo `intent-filter` (esquema/host propio y `ACTION_VIEW` de archivo) en `MainActivity`, `FileProvider` y entradas de navegación (compartir desde el editor/lista, pantalla de importación, pantalla de tarjeta).

No hay cambios **BREAKING**.

## Dependencias y límites

- **Orden de merge**: sharing es el último (10º), tras todos los demás changes paralelos; se apoya en el modelo `Alarm` de `alarm-core` (con `ChallengeConfig`, `SoundConfig`, `AntiCheatPolicy`).
- **Depende de `wake-stats`** (change paralelo): consume `WakeHistoryRepository.observeStreak(): Flow<Streak(current, best)>` (racha actual y mejor racha, por días; ya en `feature/wake-stats`). Si su contrato cambia, solo se ve afectada la capability `streak-card`.
- Los esquemas de reto, sonido y anti-cheat los define cada change propietario (`challenge-engine`, `alarm-sound`, `anti-cheat`); aquí solo se define el sobre de serialización y la validación, no las reglas de esos dominios. Hasta que existan, esos bloques se tratan como opcionales.
- Fuera de alcance: backend o enlaces cortos en servidor, sincronización de alarmas, cuentas, compartir sonidos personalizados (los audios externos no viajan en el payload; solo sonidos incluidos en la app), rediseño visual (lo cubre `design-system-stitch`).

## Capabilities

### New Capabilities
- `alarm-sharing`: exportar la configuración de una alarma como enlace, QR o archivo, y recibirla mediante importación segura con validación del payload, esquema versionado y vista previa antes de aceptar.
- `streak-card`: generar una imagen de tarjeta de racha a partir de las estadísticas y compartirla por la share sheet del sistema mediante FileProvider.

### Modified Capabilities
<!-- Ninguna: la importación crea alarmas mediante la API existente de alarm-storage y la programación sigue las reglas de alarm-scheduling sin cambiar sus requisitos. -->

## Impact

- **Código**: nuevo paquete `sharing/` (codec, validador, modelos de payload, generador de QR, generador de tarjeta), pantallas Compose de importación y tarjeta, acciones de compartir en `AlarmEditorScreen`/`HomeScreen`, rutas en `AppNavigation`, manejo de intents en `MainActivity`.
- **Manifest/recursos**: `intent-filter` VIEW (deep link y archivo), `FileProvider` + `res/xml/file_paths.xml`.
- **Dependencias**: ZXing core para generar QR; el escaneo reutiliza ML Kit Barcode Scanning + CameraX de `challenges-qr-sensors`; serialización JSON (kotlinx.serialization si ya está en el proyecto).
- **Datos**: sin migración de Room; importar usa `AlarmRepository`.
- **Seguridad**: el payload es entrada no confiable de terceros; ver design.
