# Design

## Context

La app es Kotlin + Compose/M3 con Hilt, Room y navegación tipada. `minSdk` está en 28 en `app/build.gradle.kts` aunque el producto apunta a API 29+. Hoy no hay cámara, sensores ni retos: la alarma solo notifica (`alarm/AlarmFiringHandler`). Motivación y alcance en `proposal.md`; requisitos en `specs/`.

**Dependencia de `challenge-engine`** (merge 4 antes que este, 9; `alarm-core-db` fija Room v2 antes). Su diseño define: `Challenge` con `type: ChallengeType` (id `String` estable), `generate(level: Int, params: String, random: Random): ChallengeInstance` y `availability(): Availability` (`Available`/`Unavailable(reason)`); `ChallengeInstance.check(answer: ChallengeAnswer)`; `ChallengeAnswer` sellado (`Text` ahora, QR/sensores añaden variantes); `ChallengeRegistry` (tipo → `Challenge` + composable); `ChallengeSession` (máquina de estados pura); y `ChallengeConfig(difficulty, challenges: List<ChallengeSpec(type, params)>)` de `alarm-core-db`. `ChallengeSpec.params` es un JSON opaco por tipo que el motor transporta sin interpretar. La **reserva** es suya: si `availability()` es `Unavailable` o el reto avisa de que no puede completarse, la sesión lo sustituye por otro tipo disponible (o `MATH`) y emite `ChallengeFellBack`. Este change implementa los cuatro tipos sobre ese contrato sin redefinirlo; las respuestas de QR y sensores serán nuevas variantes de `ChallengeAnswer` (p. ej. `ScanVerified`, `MotionVerified`) emitidas cuando la verificación local concluye. Nombres a confirmar contra el código fusionado al aplicar.

## Goals / Non-Goals

**Goals:**
- Cuatro retos (QR, agitar, pasos, levantarse) como implementaciones del contrato del motor.
- Lógica de verificación pura y testeable en JVM, separada de los adaptadores Android (cámara, `SensorManager`).
- Funcionar sin conexión y sin datos salientes.

**Non-Goals:**
- Definir la interfaz de reto, la selección de retos o la pantalla de disparo (`challenge-engine`).
- Detección general de trampas, dificultad adaptativa, estadísticas (`anti-cheat`, `adaptive-difficulty`, `wake-stats`).
- Garantía criptográfica: es un despertador local; se busca disuadir, no resistir un dispositivo rooteado.

## Decisions

1. **Escaneo: CameraX + ML Kit Barcode Scanning empaquetado (aprobado), como componente compartido.** El lector y su vista de cámara se exponen como un componente independiente del reto QR (flujo de lecturas por fotograma + composable de previsualización) para que `sharing` lo reutilice; el reto QR añade encima la verificación anti foto-de-foto. API acordada con `sharing`: paquete `app.luxion.apexapp.scanner`; `@Composable fun BarcodeScanner(onScanned: (ScanResult) -> Unit, modifier: Modifier = Modifier)` con `ScanResult(value: String, format: Int)`, que gestiona cámara, previsualización y ciclo de vida, sin lógica de reto. Para el reto QR se ofrece además un `Flow` de lecturas por fotograma con marca de tiempo y datos de luminancia, que alimenta la verificación anti foto-de-foto. `sharing` solo consume el composable y añade su propia dependencia ZXing core para generar QR; el permiso de cámara lo pide el llamante con un helper del mismo paquete. `ImageAnalysis` entrega fotogramas al detector. Alternativa: ZXing (más antiguo, peor en poca luz) o el scanner de Google Play Services (UI ajena, requiere GMS, sin acceso a fotogramas para la verificación). Se elige la variante empaquetada por funcionar offline y dar control de los fotogramas.

2. **Almacenamiento del código: hash, no texto plano.** Se guarda `formato + SHA-256(valor)` serializado en el JSON de `ChallengeSpec.params` del tipo QR (decidido): sin tabla propia y sin tocar Room, cuyo esquema v2 fija `alarm-core`. Comparar hashes evita exponer el contenido (p. ej. un código con datos personales) y basta para validar igualdad. Alternativa: texto plano, más simple pero innecesariamente expuesto en copias de seguridad.

3. **Anti foto-de-foto por coherencia temporal, sin hardware especial.** El código debe detectarse en ≥N fotogramas consecutivos a lo largo de ≥T ms; se calcula la varianza de la luminancia de la región fuera del código y el desplazamiento del cuadro delimitador entre fotogramas. Una imagen estática en pantalla o papel plano tiende a dar un fondo idéntico y movimiento artificial sin parallax. Además se detecta el parpadeo/moiré típico de pantallas (variación periódica de luminancia a frecuencia de refresco). Alternativas descartadas: profundidad/ToF (no disponible en general), enviar a un servicio (viola "solo local"). Limitación asumida: no detiene una réplica física perfecta del código; el usuario que quiere hacer trampa puede imprimirlo y dejarlo junto a la cama, que es un problema de colocación del código, no de detección. Se mostrará un aviso al registrar ("coloca el código lejos de la cama").

4. **Verificadores como funciones puras sobre muestras.** `ShakeDetector`, `StepVerifier`, `StandUpDetector` y `FrameLivenessChecker` reciben muestras con marca de tiempo y devuelven estado/veredicto; los adaptadores Android solo transforman eventos en muestras. Permite pruebas unitarias con señales grabadas y sintéticas sin emulador.

5. **Agitar: acelerómetro sin gravedad (`TYPE_LINEAR_ACCELERATION`, con respaldo en `TYPE_ACCELEROMETER` filtrado).** Una sacudida es un pico sobre umbral seguido de pico opuesto dentro de una ventana, con refractario mínimo. Frecuencia de muestreo `SENSOR_DELAY_GAME`.

6. **Pasos: `TYPE_STEP_COUNTER` con línea base inicial más validación cruzada con acelerómetro.** Se toma la lectura al empezar como base. Cada incremento solo cuenta si en ese intervalo el acelerómetro muestra energía de caminar (banda ~1–3 Hz). Si no hay `STEP_COUNTER`, se usa `TYPE_STEP_DETECTOR`; si tampoco, el reto no se ofrece. `ACTIVITY_RECOGNITION` se pide al configurar el reto.

7. **Levantarse: orientación + marcha.** Se usa `TYPE_GRAVITY`/rotación para detectar la postura del dispositivo en reposo previa y después marcha sostenida con la misma validación que los pasos. Heurística explícita, no un clasificador ML.

8. **Detección de sensores falsos por plausibilidad.** Reglas: varianza mínima de ruido (los sensores reales nunca son planos), periodicidad no perfecta (coeficiente de variación de amplitud/periodo mínimo), coherencia entre sensores (pasos ↔ acelerómetro ↔ gravedad), y presencia continua de eventos con `timestamp` monótono y plausible (rechaza eventos con saltos). Alternativa: comprobar si hay apps de simulación instaladas, descartada por frágil e invasiva.

9. **Permisos pedidos en contexto.** `CAMERA` y `ACTIVITY_RECOGNITION` se solicitan al configurar el reto. Con la alarma sonando no se pueden pedir de forma fiable, por lo que el reto delega en la reserva del motor si faltan. `uses-feature` de cámara, acelerómetro y contador de pasos como `required="false"`. Se sube `minSdk` a 29 (aprobado).

## Risks / Trade-offs

- [Falsos positivos del anti foto-de-foto con poca luz o mano temblorosa] → umbrales ajustables como constantes únicas, probados con grabaciones; ante duda, pedir más tiempo de escaneo en lugar de rechazar.
- [Falsos rechazos de sensores en dispositivos baratos con poco ruido] → umbrales conservadores y fallo hacia "pedir más movimiento", nunca bloqueo permanente (la reserva del motor evita dejar al usuario sin poder apagar la alarma).
- [Fabricantes que matan sensores en segundo plano o con pantalla apagada] → la pantalla del reto mantiene la pantalla encendida (`FLAG_KEEP_SCREEN_ON`) y registra los listeners solo mientras es visible.
- [Aumento de tamaño del APK por ML Kit empaquetado (~2–3 MB)] → aceptado a cambio de funcionar sin conexión.
- [Acople con un contrato aún no definido] → mantener la capa de adaptación a `challenge-engine` mínima y revisar al aplicar.

## Migration Plan

Cambio aditivo: no hay migración de datos propia; la configuración viaja en la carga opaca que persiste `challenge-engine`. Revertir consiste en retirar los cuatro tipos del registro de retos. Subir `minSdk` a 29 (aprobado) es la única decisión no reversible para usuarios con API 28.

## Open Questions

- Esquema JSON de `params` de cada tipo (campos y límites); el hash vive en el del reto QR.
- Valores concretos de umbrales (sacudidas por defecto, ventana de escaneo, pasos mínimo/máximo) a calibrar con pruebas en dispositivo.
