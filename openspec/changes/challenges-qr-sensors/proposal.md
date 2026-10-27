# Proposal

## Why

ApexApp obliga a despertar mediante retos, pero los retos que obligan a moverse o a estar físicamente en otro sitio (escanear un código, agitar, caminar, levantarse) son los más eficaces contra volver a dormirse. También son los más fáciles de burlar: una foto del código, agitar el móvil sobre la mesa con un objeto, o una app que simule sensores. Este change añade esos retos con verificación anti-falsificación desde el primer momento.

## What Changes

- Reto **QR / código de barras**: el usuario registra un código físico (p. ej. el del champú o la cafetera) al configurar la alarma; para apagarla debe escanear ese mismo código con la cámara.
- Verificación anti foto-de-foto en el reto QR: se exige un escaneo en vivo (varios fotogramas consecutivos con variación real de la escena) y se rechazan imágenes estáticas o pantallas.
- Reto **agitar**: acumular un número de sacudidas válidas detectadas con el acelerómetro.
- Reto **contar pasos**: dar un número de pasos reales detectados con el sensor de pasos.
- Reto **levantarse**: pasar de estar tumbado/quieto a estar de pie y en movimiento, validado con sensores de movimiento.
- Verificación anti-falsificación de sensores: detectar patrones sintéticos (señal perfectamente periódica, sin ruido, ausencia del sensor) y movimientos incompatibles entre sensores.
- Pantallas de configuración de cada reto y de ejecución durante el disparo.
- Los cuatro retos se implementan contra la interfaz de reto del change `challenge-engine`, que se asume tal cual y no se redefine aquí.

## Capabilities

### New Capabilities
- `qr-challenge`: reto de código QR/barras: registro del código de la alarma, escaneo con cámara y verificación anti foto-de-foto.
- `sensor-challenges`: retos de agitar, contar pasos y levantarse, con detección de sensores falsos o ausentes.

### Modified Capabilities
<!-- Ninguna: no cambian requisitos de alarm-storage, alarm-scheduling ni app-shell. El almacenamiento de la configuración de cada reto por alarma lo define challenge-engine. -->

## Impact

- **Dependencia**: `challenge-engine` (`Challenge`, `ChallengeInstance`, `ChallengeAnswer`, `ChallengeType`, `ChallengeRegistry`, `ChallengeConfig`, `ChallengeSession`). Este change no puede aplicarse hasta que exista. Orden de merge acordado: `challenge-engine` (4) antes que este (9). No toca Room: la configuración de cada reto (incluido el hash del código QR) viaja en `ChallengeSpec.params` (JSON opaco) de la `ChallengeConfig` de `alarm-core-db`.
- **Componente compartido**: el escáner de cámara (CameraX + ML Kit) se diseña como componente reutilizable, coordinado con el change `sharing`, que lo usará también.
- **Código**: nuevo paquete de retos bajo `app/src/main/java/app/luxion/apexapp/` (implementaciones, detectores, UI de configuración y ejecución) y registro en el módulo Hilt de retos.
- **Dependencias**: CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) y ML Kit Barcode Scanning en su variante empaquetada (`com.google.mlkit:barcode-scanning`, funciona sin conexión).
- **Permisos**: `CAMERA` (runtime, solo al registrar/escanear), `ACTIVITY_RECOGNITION` (runtime, API 29+, para el contador de pasos). El acelerómetro y el sensor de gravedad/rotación no requieren permiso. Declarar `uses-feature` de cámara, acelerómetro y step counter como `required="false"` para no excluir dispositivos.
- **minSdk**: `app/build.gradle.kts` declara `minSdk = 28` pero el proyecto es API 29+; `ACTIVITY_RECOGNITION` en runtime exige 29. Subir `minSdk` a 29 (aprobado) forma parte de este change.
- **Fuera de alcance**: interfaz de reto (`challenge-engine`), detección general de trampas (`anti-cheat`), dificultad adaptativa, estadísticas y sonido.
