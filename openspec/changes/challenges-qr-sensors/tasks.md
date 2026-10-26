# Tasks

## 1. Setup y dependencia del motor

- [x] 1.1 Confirmar que `challenge-engine` y `alarm-core-db` están fusionados (`ChallengeSpec.params`, `availability()`, `ChallengeFellBack`); reconciliar los nombres de este change con los reales y verificar que el proyecto compila contra ellos
- [x] 1.2 `minSdk` ya es 29 (lo subió alarm-core); añadir CameraX y ML Kit Barcode Scanning al catálogo de versiones y a `app/build.gradle.kts`; verificar `./gradlew assembleDebug`
- [x] 1.3 Declarar permisos `CAMERA` y `ACTIVITY_RECOGNITION` y `uses-feature` (`required="false"`) en el manifiesto; verificar con `./gradlew assembleDebug` y revisando el manifiesto fusionado

## 2. Verificadores puros de sensores

- [x] 2.1 Implementar `ShakeDetector` (pico/contrapico, refractario, reinicio por inactividad); verificar con tests JVM sobre señales sintéticas y ruidosas
- [x] 2.2 Implementar `StepVerifier` (línea base, validación cruzada con energía del acelerómetro); verificar con tests de pasos previos, pasos sin movimiento y caminar real grabado
- [x] 2.3 Implementar `StandUpDetector` (postura previa → marcha sostenida); verificar con tests de levantarse y de cambio de orientación sin desplazamiento
- [x] 2.4 Implementar `SensorPlausibilityChecker` (varianza mínima, periodicidad perfecta, timestamps, datos ausentes); verificar con tests de señales sintéticas perfectas rechazadas y reales aceptadas

## 3. Retos de sensores

- [x] 3.1 Crear adaptadores de `SensorManager` que emitan muestras con marca de tiempo y manejen sensor ausente; verificar con test instrumentado o fake en JVM
- [x] 3.2 Implementar los retos agitar, pasos y levantarse contra la interfaz del motor, con progreso y reserva por sensor sin datos; verificar con tests de la lógica del reto
- [x] 3.3 Implementar la UI de configuración (objetivo informativo derivado de la dificultad; ya no se elige) y de ejecución de cada reto, siguiendo `design-taste-compose`; verificar con tests de la tabla de dificultad y de su aplicación a cada reto
- [x] 3.4 Pedir `ACTIVITY_RECOGNITION` al configurar el reto de pasos y ocultar el reto si no hay sensor; verificar manualmente en emulador sin y con permiso
  - Verificado en emulador (sin contador de pasos): al elegir «Steps» aparece «This device has no step counter.» y Save queda deshabilitado. Verificado en móvil real (Pixel 7 Pro, confirmado por el usuario): con contador de pasos pide ACTIVITY_RECOGNITION, se concede y los pasos reales cuentan.

## 4. Reto QR

- [x] 4.1 Implementar `FrameLivenessChecker` (coherencia temporal, varianza de fondo, parpadeo de pantalla) como función pura; verificar con tests sobre secuencias de fotogramas sintéticas estáticas y dinámicas
- [x] 4.2 Implementar el lector CameraX + ML Kit como componente compartido (flujo de lecturas por fotograma + previsualización), en el paquete `app.luxion.apexapp.scanner` con `BarcodeScanner(onScanned, modifier)` según design.md; verificar leyendo un QR y un código de barras en emulador/dispositivo
- [x] 4.3 Implementar registro del código (hash SHA-256 + formato serializado en `ChallengeSpec.params`, sin tocar Room; sustitución, validación al guardar) y comparación; verificar con tests unitarios de igualdad, distinto y sustitución
- [x] 4.4 Implementar el reto QR contra la interfaz del motor con verificación anti foto-de-foto y reserva sin cámara o sin permiso; verificar rechazando una foto en pantalla y aceptando el objeto real
  - Verificado en emulador: vista de cámara en vivo y recorte correcto. Verificado en móvil real (Pixel 7 Pro, confirmado por el usuario): escaneo del QR.
- [x] 4.5 Implementar UI de registro y escaneo con gestión de permiso de cámara (pendiente, denegado permanente → ajustes) y aviso de colocación del código; verificar con tests Compose y a mano
  - Verificado en emulador: permiso de cámara pendiente (explicación + «Grant»), denegado una vez (vuelve a ofrecer), denegado permanente (aviso + «Open settings», abre los ajustes de la app), concedido desde ajustes y vuelta con Atrás (pasa solo a cámara; corregido en este change), aviso de colocación, y Save deshabilitado con QR sin código. Verificado en móvil real (Pixel 7 Pro, confirmado por el usuario): escanear y registrar un código real.

## 5. Integración

- [ ] 5.1 Registrar los cuatro tipos de reto en el módulo Hilt del motor y exponerlos en la configuración de alarma; verificar que aparecen y se guardan por alarma
  - Hecho: registro Hilt de `Challenge`, `ChallengeRenderer` y `ChallengeParamsEditor` por tipo (QR bloquea guardar sin código; pasos exige contador de pasos). Falta verificar en dispositivo que aparecen en la sección «Retos» y se guardan por alarma.
  - Verificado en emulador: los cuatro tipos aparecen en la sección «Challenges»; Shake con objetivo 40 se guarda y se recupera al reabrir la alarma. Sin verificar: guardar y recuperar un código QR (requiere escanear) y los demás tipos configurados.
- [ ] 5.2 Prueba de extremo a extremo en emulador/dispositivo: alarma con cada reto, disparo, superación y casos de trampa; verificar con `./gradlew test connectedDebugAndroidTest` y checklist manual
  - Verificado en la pantalla de pruebas de retos (no con alarma real disparada): Shake se supera con sacudidas simuladas (`adb emu sensor`). Sin verificar: disparo real de la alarma con cada reto, QR, Pasos y Levantarse.
- [ ] 5.3 Calibrar umbrales (sacudidas, ventana de escaneo, límites de pasos) en dispositivo real y fijarlos en constantes únicas; verificar que los tests siguen en verde

## Manual para el usuario

Pruebas que exigen un móvil físico (el emulador no tiene contador de pasos ni permite escanear un QR real sin tocar recursos compartidos del SDK):

- **Calibrar umbrales (5.3):** sacudidas (`ShakeDetector.DEFAULT_THRESHOLD` = 12 m/s², refractario 200 ms, inactividad 2 s), ventana de escaneo (`FrameLivenessChecker`: 1,5 s y 15 fotogramas) y la tabla de objetivos por nivel (`MotionTargets.BY_LEVEL`, dentro de los límites de `MotionKind`: sacudidas 10–100, pasos 10–200, segundos 5–60). Dejar los valores ajustados en esas constantes y comprobar que los tests siguen en verde.
- **Reto QR:** registrar un código real (QR y de barras), escanear el objeto real y comprobar que se acepta; apuntar a una foto impresa y a otra pantalla y comprobar que se rechaza («Parece una foto» / «Parece una pantalla»).
- **Reto de pasos:** conceder «actividad física» al configurarlo y comprobar que solo cuentan pasos reales caminando, no sacudir el móvil.
- **Reto de levantarse:** tumbado y quieto durante 2 s, incorporarse y caminar el tiempo configurado; comprobar que girar el móvil sin moverse no lo supera.
- **Alarma real de extremo a extremo:** con cada reto, dejar sonar la alarma, superar el reto y comprobar que se apaga; con la cámara o el permiso revocados, comprobar que el motor aplica la reserva.
- **Limitación conocida del detector de sensores falsos:** solo descarta señales con amplitud y periodo casi idénticos (variación < 2 %). Una señal inyectada con un poco de jitter (como un script de `adb emu sensor`) lo supera. Valorar endurecer `SensorPlausibility.MIN_VARIATION` al calibrar con sacudidas humanas reales.
- **Revisión tras la prueba en móvil real (levantarse y pasos fallaban):** `StepVerifier` limitaba cada lectura del contador por el hueco con la anterior y descartaba pasos agrupados; `StandUpDetector` exigía marcha ininterrumpida y una parada de 1 s reiniciaba todo; `MotionChallengeContent` seguía recogiendo con la app en segundo plano y el vigilante de 10 s sustituía el reto por la reserva. Corregido y cubierto con tests; falta confirmar en el Pixel (caminar con el móvil en la mano y con la pantalla apagándose).
