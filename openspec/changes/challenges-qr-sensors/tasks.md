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
- [ ] 3.3 Implementar la UI de configuración (objetivo con límites) y de ejecución de cada reto, siguiendo `design-taste-compose`; verificar con tests Compose de rango y de ajuste a límites
- [ ] 3.4 Pedir `ACTIVITY_RECOGNITION` al configurar el reto de pasos y ocultar el reto si no hay sensor; verificar manualmente en emulador sin y con permiso

## 4. Reto QR

- [x] 4.1 Implementar `FrameLivenessChecker` (coherencia temporal, varianza de fondo, parpadeo de pantalla) como función pura; verificar con tests sobre secuencias de fotogramas sintéticas estáticas y dinámicas
- [x] 4.2 Implementar el lector CameraX + ML Kit como componente compartido (flujo de lecturas por fotograma + previsualización), en el paquete `app.luxion.apexapp.scanner` con `BarcodeScanner(onScanned, modifier)` según design.md; verificar leyendo un QR y un código de barras en emulador/dispositivo
- [x] 4.3 Implementar registro del código (hash SHA-256 + formato serializado en `ChallengeSpec.params`, sin tocar Room; sustitución, validación al guardar) y comparación; verificar con tests unitarios de igualdad, distinto y sustitución
- [ ] 4.4 Implementar el reto QR contra la interfaz del motor con verificación anti foto-de-foto y reserva sin cámara o sin permiso; verificar rechazando una foto en pantalla y aceptando el objeto real
- [ ] 4.5 Implementar UI de registro y escaneo con gestión de permiso de cámara (pendiente, denegado permanente → ajustes) y aviso de colocación del código; verificar con tests Compose y a mano

## 5. Integración

- [ ] 5.1 Registrar los cuatro tipos de reto en el módulo Hilt del motor y exponerlos en la configuración de alarma; verificar que aparecen y se guardan por alarma
  - Hecho: registro Hilt de `Challenge`, `ChallengeRenderer` y `ChallengeParamsEditor` por tipo (QR bloquea guardar sin código; pasos exige contador de pasos). Falta verificar en dispositivo que aparecen en la sección «Retos» y se guardan por alarma.
- [ ] 5.2 Prueba de extremo a extremo en emulador/dispositivo: alarma con cada reto, disparo, superación y casos de trampa; verificar con `./gradlew test connectedDebugAndroidTest` y checklist manual
- [ ] 5.3 Calibrar umbrales (sacudidas, ventana de escaneo, límites de pasos) en dispositivo real y fijarlos en constantes únicas; verificar que los tests siguen en verde
