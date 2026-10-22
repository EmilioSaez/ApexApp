# Design

## Context

Ver proposal.md para la motivación. Estado actual: `Alarm` (hora, minuto, `repeatDays`, `enabled`, `label`, `soundId`) y `Sound` (`Resource(resId)` o `Uri`) en `domain/`; persistencia Room vía `AlarmRepository`; navegación Compose con rutas `@Serializable` (`HomeRoute`, `AlarmEditorRoute`); `kotlinx-serialization-json` ya está en el catálogo de versiones; `MainActivity` solo tiene el intent-filter LAUNCHER. No hay backend, solo APK directo.

Los dominios de reto, sonido y anti-cheat se definen en changes paralelos y aún no existen en el código; `wake-stats` también.

## Goals / Non-Goals

**Goals:**
- Un único payload canónico, serializado igual para enlace, QR y archivo.
- Importación segura: entrada no confiable, todo o nada, con confirmación del usuario.
- Evolución del esquema sin romper versiones antiguas de la app.
- Tarjeta de racha reproducible y testeable sin UI.

**Non-Goals:**
- Firma criptográfica o autenticidad del emisor (el payload no da ventajas: solo crea una alarma desactivada tras confirmación).
- Transferir audios personalizados.
- App Links verificados (requiere dominio y `assetlinks.json`).

## Decisions

**D1. Payload JSON con `kotlinx.serialization`, sobre `SharedAlarmV1`.**
Estructura: `{ "v": 1, "alarm": {h, m, days[], label?}, "challenge"?: {...}, "sound"?: {...}, "antiCheat"?: {...} }`. Cada bloque opcional lo define su change propietario; sharing solo posee el sobre y los modelos `…Dto` desacoplados del dominio (un mapper por dirección), de modo que cambiar el dominio no cambia el formato en disco. `ignoreUnknownKeys = true` para campos nuevos dentro de la misma versión mayor; versión mayor desconocida se rechaza. *Alternativas:* Protobuf (menor tamaño pero no legible/depurable, dependencia extra); Parcelable/Serializable de Java (inseguro: deserialización arbitraria).

**D2. Codificación para transporte: JSON → Deflate → Base64 URL-safe sin padding.**
- Enlace: `apexapp://alarm?d=<payload>` (esquema propio) en vez de https. Sin dominio no hay App Links; el esquema propio funciona entre amigos con APK. Limitación: algunos chats no linkifican esquemas propios → se acompaña en el texto compartido de una línea explicativa y se ofrece archivo/QR como alternativa.
- QR: el mismo enlace; el tamaño del payload (alarma + bloques) debe caber en un QR de nivel de corrección M (objetivo < ~1000 bytes codificados); si lo excede se omite el QR y se sugiere archivo.
- Archivo: `.apexalarm` con el JSON comprimido (misma codificación binaria, sin Base64) y MIME propio `application/vnd.apexapp.alarm`.
*Alternativa:* solo enlace https a una web estática de redirección; descartada por requerir infraestructura.

**D3. Pipeline de importación en capas, puro y testeable.**
`bytes/String → SharedAlarmDecoder (límite de tamaño ANTES de descomprimir, límite de salida descomprimida contra zip-bomb) → parse → SharedAlarmValidator → ImportPreview → (confirmación) → AlarmRepository.save`. El validador devuelve `Valid(preview) | Invalid(reason)` y es una función pura. Límites iniciales: entrada ≤ 8 KB, descomprimido ≤ 16 KB, etiqueta ≤ 60 caracteres, días ⊆ 7 valores únicos. Los bloques de reto/sonido/anti-cheat se validan con funciones que aporta cada change; hasta entonces se tratan como opcionales y se ignoran. Un tipo de reto desconocido se sustituye por el reto por defecto y se avisa en la vista previa (nunca se resuelve por reflexión ni nombre de clase).

**D4. La alarma importada entra desactivada y sin programar.** Se persiste con `enabled = false` sobre el modelo `Alarm` de `alarm-core` (que fija Room v2 e incorpora `ChallengeConfig`, `SoundConfig` y `AntiCheatPolicy`); `alarm-scheduling` no programa nada. Evita que un tercero active una alarma con efectos (p. ej. sonido a volumen alto) sin que el usuario lo haga explícitamente. El sonido viaja solo si es `SoundSource.Bundled(key)` de `alarm-sound` (clave estable, p. ej. "default"; contrato provisional hasta su apply, 5º en el orden de merge); la clave NO está en `SoundConfig` (volume, vibrate, ttsEnabled, ttsText, rampSeconds, vibrationPattern, speakTime, speakRepeatSeconds; serializable, viaja en el bloque `sound` junto a la clave) sino en el sonido. Al importar se resuelve con `BundledSounds.resolve(key)`, que devuelve el sonido por defecto si la clave es null o desconocida. Las fuentes no portables (URI externa SAF/tono del sistema) se omiten al exportar y nunca se importan.

**D4b. Ajustes tras revisar develop (e50cb8f, Room v5).** (a) `Alarm` ya trae `challenges: ChallengeConfig`, `soundConfig: SoundConfig` y `antiCheat: AntiCheatPolicy`, todos `@Serializable`; los DTOs los reutilizan sin duplicar campos y sin tocar Room (no hay migración 5→6). (b) El sonido: `soundId == null` ya significa sonido por defecto; solo se exporta la clave si el `Sound` referenciado es `SoundSource.Bundled`; al importar, la clave se resuelve con `BundledSounds.resolve` y, si no es la de por defecto, se reutiliza o crea la fila `Sound` bundled para fijar `soundId`. (c) El reto QR guarda en `params` el hash SHA-256 de un código físico que el receptor no tiene: se exporta sin params y al importar el reto QR se omite con aviso en la vista previa («registra tu propio código al editar»); si la cadena queda vacía se usa la configuración de retos por defecto. El resto de `params` se valida como JSON de tamaño acotado. (d) Los tipos de reto se validan contra los registrados (`ChallengeRegistry`), no contra una lista propia.

**D5. Entrada por intents y escáner QR.** `MainActivity` añade `intent-filter` VIEW/BROWSABLE para `apexapp://alarm` y VIEW para el MIME/extensión `.apexalarm` (`content://`). Los datos se leen con `ContentResolver` con tope de bytes; el intent se traduce a una ruta de navegación `ImportRoute(payload)` en `AppNavigation`, sin ejecutar lógica en el receptor. Escáner de QR **decidido**: ML Kit Barcode Scanning + CameraX, reutilizando el componente de escaneo de `challenges-qr-sensors` (sin duplicar dependencias ni permiso de cámara; se coordina con esa sesión y, al ser sharing el último en el orden de merge, el componente ya estará disponible). Contrato confirmado por esa sesión (nombres provisionales hasta su apply): paquete `app.luxion.apexapp.scanner`, `@Composable fun BarcodeScanner(onScanned: (ScanResult) -> Unit, modifier: Modifier = Modifier)` con `ScanResult(value: String, format: Int)`; gestiona cámara, previsualización y ciclo de vida, y el llamante (sharing) pide el permiso CAMERA con el helper del mismo paquete. CameraX y ML Kit los añade `challenges-qr-sensors` (merge 9º, antes que sharing). Para generar el QR se usa ZXing core (puro Java, sin servicios de Google).

**D6. Tarjeta de racha: render con `Canvas`/`Bitmap` en un componente puro.**
`StreakCardRenderer(stats, theme) → Bitmap` de tamaño fijo (1080×1350), independiente de la pantalla para ser testeable con Robolectric/instrumentada. Una pantalla Compose muestra la vista previa con los mismos datos. Se guarda en `cacheDir/shared/` y se expone con `FileProvider` (`res/xml/file_paths.xml` limitado a ese subdirectorio) con `FLAG_GRANT_READ_URI_PERMISSION` vía `Intent.createChooser(ACTION_SEND)`. Se limpia el directorio antes de generar. *Alternativa:* `MediaStore` (requiere guardar en galería, permisos/ruido innecesarios).

**D7. Dependencia con `wake-stats`.** La tarjeta consume directamente `observeStreak()` de `wake-stats` (`Flow<Streak(current, best)>` de `WakeHistoryRepository`, racha por días; el total de despertares no forma parte del contrato, así que la tarjeta no lo muestra). Se mantiene una interfaz fina `StreakStatsSource` en sharing que envuelve esa llamada para poder usar un fake en tests y aislar cambios de contrato.

**D8. Orden de merge y alcance de campos.** Sharing se integra el último (tras design-system-stitch, alarm-core, alarm-reliability, challenge-engine, alarm-sound, anti-cheat, wake-stats, adaptive-difficulty y challenges-qr-sensors), por lo que los DTOs de reto, sonido y anti-cheat se mapean contra `ChallengeConfig`, `SoundConfig` y `AntiCheatPolicy` de alarm-core. El snooze pertenece a anti-cheat y viaja dentro de `AntiCheatPolicy`. Los identificadores de sesión (`sessionId`) y el estado de racha nunca se exportan.

## Risks / Trade-offs

- [Payload malicioso (zip-bomb, JSON enorme, valores raros)] → límites de tamaño pre y post descompresión, validación total, todo-o-nada, alarma desactivada, confirmación obligatoria.
- [Esquema propio no linkificado en algunos chats] → texto explicativo, QR y archivo como alternativas; App Links como mejora futura.
- [QR demasiado denso con muchos bloques] → payload comprimido, tope de tamaño para QR y fallback a archivo.
- [Acoplamiento a esquemas de changes paralelos aún no definidos] → bloques opcionales con `ignoreUnknownKeys`, DTOs desacoplados, integración incremental por change.
- [Cambios de `resId` rompen compatibilidad de sonidos] → claves estables, no ids de recurso.
- [Fuga de datos en la tarjeta] → solo racha actual y mejor racha; sin etiquetas ni horas.

## Migration Plan

Sin migraciones de Room. Cambio aditivo; rollback = revertir el change. La versión de esquema inicial es 1; futuros cambios incompatibles incrementan la versión mayor.

## Open Questions

- Nombre exacto del esquema/host del deep link y extensión de archivo.
- Confirmar con `challenge-engine`/`anti-cheat` los campos serializables de `ChallengeConfig` y `AntiCheatPolicy`; hasta entonces son opcionales.
