# Design

## Context

El reto QR ya resuelve cámara (CameraX), permiso, componente de escáner y verificación de vida (`FrameLivenessChecker`, que recibe `FrameObservation` con valor, posición y luminancia de fondo). Los `params` de `ChallengeSpec` son JSON opaco por tipo y la sesión filtra los tipos desconocidos. Ver proposal.md para la motivación.

## Goals / Non-Goals

**Goals:**
- Reutilizar el escáner de cámara y la verificación de vida del reto QR.
- Reconocimiento 100 % local y sin guardar imágenes.

**Non-Goals:**
- Objetos personalizados con entrenamiento o fotos de referencia.
- Mejorar la precisión de ML Kit más allá de fijar un umbral y una ventana.

## Decisions

- **ML Kit Image Labeling con modelo base**: etiquetas genéricas, sin red ni cuenta. Alternativa: modelo personalizado con TensorFlow Lite, descartada por coste de entrenamiento y tamaño. Alternativa: comparar fotos de referencia, descartada por fragilidad con la luz.
- **Guardar la etiqueta, no la imagen**: `params = {"label":"Tap"}`; ningún fotograma se persiste. La etiqueta se normaliza y compara por identificador de ML Kit, no por texto traducido.
- **Adaptador a `FrameObservation`**: cada fotograma etiquetado se reduce a un valor (la etiqueta registrada si coincide con confianza ≥ 0,7, `null` si no) más centro y luminancia, y se alimenta al `FrameLivenessChecker` existente; así se hereda la ventana, el reinicio y el rechazo de pantalla/foto.
- **Extraer el escáner a un componente genérico** de fotogramas en `ui/components` solo si el QR no permite inyectar el analizador; si sí, se reutiliza tal cual.
- **Editor**: vista previa con lista de etiquetas detectadas (las 5 de mayor confianza) y selección explícita; requiere el permiso antes de abrir la cámara.

## Risks / Trade-offs

- Las etiquetas genéricas pueden coincidir con otros objetos (p. ej. "Habitación"): el editor avisa si la etiqueta es muy genérica y propone elegir otra más específica.
- Tamaño del APK por el modelo base y falsos negativos con poca luz al despertar; la ventana corta y el umbral de 0,7 equilibran fricción y seguridad.
- La verificación de vida es heurística: una foto muy buena o un vídeo podrían burlarla, igual que en QR; se acepta el mismo nivel de riesgo.

## Decisions on open questions

- **Un objeto por alarma, una foto por reto de foto** (decidido con el usuario): el número de fotos pedidas es el número de retos de foto que salen en la sesión (1 reto = 1 foto, 2 retos = 2 fotos). El motor admite una sola configuración por tipo, así que todos los retos de foto de la alarma piden el mismo objeto registrado; la verificación de vida impide repetirlo con la misma imagen. Objetos distintos por reto exigirían una configuración por reto en el motor y quedan fuera de alcance.
