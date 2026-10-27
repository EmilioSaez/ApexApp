# Proposal

## Why

El reto QR obliga a ir a un sitio concreto, pero exige pegar o localizar un código físico. Un reto de "foto a un objeto" (el grifo, la cafetera, el cepillo de dientes) consigue lo mismo sin imprimir nada: el usuario elige un objeto de su casa que le obligue a salir de la cama y, al sonar la alarma, tiene que enfocarlo con la cámara.

## What Changes

- Nuevo reto **foto a un objeto**: al configurar la alarma el usuario apunta la cámara al objeto y elige la etiqueta que el reconocedor local le asigna (p. ej. "Grifo", "Taza"); para apagar la alarma debe enfocar de nuevo un objeto con esa etiqueta.
- Reconocimiento **en el dispositivo** (ML Kit Image Labeling): ninguna imagen sale del móvil ni se guarda.
- Verificación en vivo: la etiqueta debe mantenerse varios fotogramas consecutivos con confianza mínima, y se reutiliza la comprobación anti foto-de-foto del reto QR (imagen estática o pantalla).
- Editor de parámetros con vista previa de cámara y lista de etiquetas detectadas; no se puede guardar la alarma sin etiqueta elegida.
- Disponibilidad condicionada a cámara y permiso, con reserva al comportamiento ya existente cuando no están.
- Strings en español e inglés.

## Capabilities

### New Capabilities
- `object-photo-challenge`: reto de reconocer en vivo un objeto elegido por el usuario mediante etiquetado de imagen local, con verificación anti-falsificación.

### Modified Capabilities
<!-- Ninguna: se registra como un tipo más del motor de retos. -->

## Impact

- **Dependencias**: `com.google.mlkit:image-labeling` (modelo base incluido en la app; aumenta el tamaño del APK) y el escáner de cámara CameraX ya existente del reto QR, extraído como componente compartido si hace falta.
- **Dominio/UI**: nuevo `Challenge`, `ChallengeRenderer` y `ChallengeParamsEditor`; reutilización de `FrameLivenessChecker`; bindings Hilt, etiquetas y strings.
- **Datos**: sin migración Room; la etiqueta elegida viaja en `ChallengeSpec.params` (JSON). Tipo desconocido en versiones antiguas se filtra al crear la sesión.
- **Supuestos** (a validar): se usa etiquetado genérico, no un modelo entrenado por el usuario; la etiqueta es un identificador de ML Kit, estable entre versiones del modelo base.
- **Fuera de alcance**: reconocer objetos personalizados con fotos de referencia propias, y compartir fotos de referencia.
