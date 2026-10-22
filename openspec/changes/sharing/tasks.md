# Tasks

## 1. Modelo de payload y codec

- [x] 1.1 Crear DTOs `SharedAlarm` (versión `v`, bloque `alarm`, bloques opcionales `challenge`, `sound`, `antiCheat`) y mappers contra `Alarm`, `ChallengeConfig`, `SoundConfig` y `AntiCheatPolicy` de `alarm-core`; verificar con test unitario de ida y vuelta que no se exportan id, `enabled`, `sessionId` ni URIs externas
- [x] 1.2 Implementar codec JSON → Deflate → Base64 URL-safe (enlace) y binario (archivo) con límites de entrada (8 KB) y de salida descomprimida (16 KB); verificar con tests unitarios de ida y vuelta y de rechazo por tamaño/zip-bomb
- [x] 1.3 Implementar la política de versiones (aceptar v1, ignorar claves opcionales desconocidas, rechazar versión mayor); verificar con tests unitarios de los tres escenarios de versión

## 2. Validación e importación

- [x] 2.1 Implementar `SharedAlarmValidator` puro (rangos de hora/minuto, días únicos, longitud de etiqueta, tipos conocidos, tipo de reto desconocido → reto por defecto con aviso); verificar con tests unitarios de casos válidos y malformados, fuera de rango y todo-o-nada
- [x] 2.1b Omitir el reto QR al importar (sin `params` al exportar) con aviso en la vista previa; verificar con test unitario
- [x] 2.2 Implementar `ImportPreview` y el caso de uso de importación que guarda con `enabled = false` vía `AlarmRepository` solo tras confirmación; verificar con test unitario (aceptar crea 1 alarma desactivada, cancelar no crea, importar dos veces crea dos) y que no se invoca el programador
- [x] 2.3 Mapear sonido: exportar solo `SoundSource.Bundled(key)` y resolver al importar con `BundledSounds.resolve(key)` (por defecto si es null/desconocida; contrato provisional de `alarm-sound`); `soundId == null` no exporta clave; verificar con test unitario incluyendo URI externa/tono del sistema omitidos

## 3. Exportar y compartir alarma

- [x] 3.1 Generar enlace `apexapp://alarm?d=…` y compartirlo con `ACTION_SEND`; verificar con test unitario del enlace y manualmente en emulador que aparece la share sheet
- [x] 3.2 Añadir ZXing core al catálogo para generar el QR y no añadir CameraX ni ML Kit (los aporta `challenges-qr-sensors`); verificar que el proyecto compila
- [x] 3.3 Generar y mostrar el QR de la alarma con tope de tamaño y fallback a archivo si no cabe; verificar con test unitario del tope y prueba de lectura del QR generado
- [ ] 3.4 Exportar archivo `.apexalarm` con `FileProvider` (`file_paths.xml` limitado a `cache/shared/`); verificar manualmente que otra app lo recibe y que no accede a otros archivos
- [x] 3.5 Añadir acción "Compartir" en `AlarmEditorScreen`/`HomeScreen` con selector enlace/QR/archivo; verificar con test de UI Compose

## 4. Recepción e importación en la UI

- [x] 4.1 Añadir `intent-filter` (deep link y archivo/MIME `.apexalarm`) en `MainActivity` y lectura acotada por `ContentResolver`; verificar con `adb shell am start -d "apexapp://alarm?d=…"` y apertura de archivo
- [x] 4.2 Añadir `ImportRoute` en `AppNavigation` y pantalla de vista previa (hora, días, etiqueta, reto, sonido, anti-trampa) con Aceptar/Cancelar y estados de error, siguiendo el skill `design-taste-compose`; verificar con tests de UI Compose
- [x] 4.3 Usar `BarcodeScanner` (paquete `scanner`, provisional) de `challenges-qr-sensors`, pidiendo CAMERA con su helper, para escanear un QR desde la app y llevar a `ImportRoute`; verificar escaneando el QR generado en 3.3 en emulador/dispositivo

## 5. Tarjeta de racha

- [x] 5.1 Definir `StreakStatsSource` como envoltorio de `WakeHistoryRepository.observeStreak()` (`Streak(current, best)`, commit f20f4ce de feature/wake-stats; documentar dependencia); verificar con test unitario con fake
- [x] 5.2 Implementar `StreakCardRenderer` (Bitmap 1080×1350, tema de la app, sin etiquetas ni horas); verificar con test instrumentado/Robolectric que genera un bitmap de las dimensiones esperadas
- [x] 5.3 Pantalla de vista previa de la tarjeta con estado "sin logros" y actualización reactiva; verificar con tests de UI Compose
- [ ] 5.4 Compartir la tarjeta con `FileProvider` + `ACTION_SEND` y `FLAG_GRANT_READ_URI_PERMISSION`, limpiando `cache/shared/` antes; verificar manualmente en emulador que la imagen llega a otra app

## 6. Cierre

- [x] 6.1 Ejecutar `./gradlew testDebugUnitTest` y `connectedDebugAndroidTest`; verificar que pasan sin regresiones en alarm-storage/alarm-scheduling
- [x] 6.2 Revisar `openspec validate sharing` y contrastar los esquemas de reto/sonido/anti-cheat con los changes propietarios; verificar que la validación pasa y los bloques están integrados o documentados como opcionales
