# Design

## Context

Los retos implementan `Challenge`/`ChallengeInstance` y se registran en `ChallengeModule`; el estado de sesión lo lleva `ChallengeSession`, que ante un fallo regenera la instancia. Los retos de movimiento derivan su objetivo del nivel y ignoran `params`; QR usa `params` para su código. Hay dos puntos del encargo que no coinciden con el código:

- Mates/Pasos **no** tienen parámetro de rondas (su exigencia sale del nivel). El patrón más cercano para «un parámetro por alarma» es `ChallengeSpec.params` + `ChallengeParamsEditor` (como QR).
- El nivel anti-trampas (`Alarm.antiCheat.level`) hoy solo gobierna el lockdown; no llega a los retos, que no reciben la alarma.

## Goals / Non-Goals

**Goals:** un reto de reflejos con lógica pura testeable en JVM, escalable por nivel 1–5 como Mates, un parámetro propio (rondas), penalización sin reinicio, anti-trampas según el nivel de la alarma, cambios mínimos en archivos compartidos.
**Non-Goals:** Ritmo y Mantener y soltar; cambiar el motor de sesiones.

## Decisions

1. **Una instancia = el juego completo.** `TapTargetsChallenge` genera una `TapTargetsInstance` que lleva la regla de rondas; la UI le informa de aciertos/fallos de ronda y, al completarse, entrega `ChallengeAnswer.GameVerified(type)` (variante común a los tres juegos táctiles, ya en el commit compartido). El motor ve un reto que se supera o se falla como los de movimiento. La instancia toma el nivel que le da la sesión (proveedor de dificultad, adaptativo incluido) y escala como Mates.
2. **Dificultad por nivel (tabla única `TapTargetsTuning.BY_LEVEL`, como `MotionTargets`).**

   | Nivel | Círculos | Diámetro | Plazo |
   |---|---|---|---|
   | 1 | 2 | 72 dp | 4,0 s |
   | 2 | 3 | 64 dp | 3,5 s |
   | 3 | 3 | 56 dp | 3,0 s |
   | 4 | 4 | 52 dp | 2,5 s |
   | 5 | 5 | 48 dp | 2,0 s |

   Valores a calibrar en dispositivo. Las rondas no dependen del nivel.
3. **`params`: `{"rounds":N}`.** Se lee con una función tolerante (falta/ inválido → 8; fuera de rango → acotado a 3..15). El editor es un stepper; no hace falta `isValid`.
4. **Duración ~1 min** en nivel 3: 3 círculos, 3,0 s de plazo, pausa aleatoria de 0,6–1,2 s entre rondas, 8 rondas ≈ 45–60 s.
5. **La alarma no se apaga sin superar el reto.** No hay salida: el fallo solo repite la ronda (la sesión ya mantiene el sonado hasta `passed`). Si el reto no puede completarse (p. ej. fallo de la vista), el renderer usa `onUnavailable` y se aplica la reserva del motor.
6. **Regla de fallo (documentada en el spec).** Ronda fallida (tiempo agotado) → no cuenta y se añade 1 ronda extra, con tope `extras ≤ rondas configuradas`. Superado el tope, la ronda se repite sin crecer. Nunca se descartan rondas ya superadas. Alternativas descartadas: reinicio total (lo prohíbe el encargo) y reinicio tras X fallos (introduce estados difíciles de explicar al usuario).
7. **Posiciones.** El generador coloca los círculos del nivel (≥ 48 dp de diámetro táctil) por muestreo con rechazo para evitar solapes, en coordenadas normalizadas 0..1 que la UI traduce al tamaño real. Usa el `Random` recibido (por sesión) y se regenera en cada ronda y reintento.
8. **Anti-trampas por nivel de la alarma.**
   - `OFF`: sin validación.
   - `STANDARD`: se descarta el toque si no es de pantalla táctil, si llega con `FLAG_WINDOW_IS_OBSCURED`/`FLAG_WINDOW_IS_PARTIALLY_OBSCURED`, o si el intervalo al toque anterior es < 60 ms.
   - `STRICT`: lo anterior más regularidad sintética: con ≥ 6 toques recientes, coeficiente de variación de intervalos < 2 % (mismo criterio que `SensorPlausibility`) o desplazamientos idénticos al píxel; además, tras 3 descartes se marca la sesión como sospechosa.
   **Piezas compartidas (commit `123561f`, ya hechas):** `TouchPlausibility(level)` es genérica (pulsar/soltar, sin geometría) y la regla de regularidad solo mira intervalos de pulsaciones. El nivel efectivo llega a los renderers por `LocalTouchGuard` (CompositionLocal con defecto `OFF`), que provee `ChallengeSessionScreen` a partir de dos parámetros opcionales alimentados desde `RingActivity` con el nivel efectivo del lockdown; así no cambia la firma de `Render`. En `STRICT` cada descarte llama a `SessionIntegrityTracker.syntheticInput`; con 3 se emite `SuspicionReason.SYNTHETIC_INPUT`, que sigue el cauce existente (evento «sospechosa», excluida de estadísticas y adaptativa) sin impedir apagar la alarma.
9. **Disponibilidad:** siempre `Available` (no hay sensores ni permisos).
10. **Textos:** `challenge_type_tap_targets` y los de progreso/ayuda en `values` y `values-en`, en un bloque propio al final del fichero.

## Risks / Trade-offs

- [Falsos descartes en `STANDARD` por superposiciones legítimas (burbujas de chat)] → solo se descarta el toque, no se falla la ronda; el usuario vuelve a tocar.
- [STRICT con falsos positivos] → umbrales en constantes únicas; marcar sospechosa no bloquea la alarma.
- [Conflictos de merge con Ritmo / Mantener y soltar] → cambios en archivos compartidos de una línea por sitio, bloques propios en strings.

## Migration Plan

Aditivo: sin migración de datos. Revertir = quitar los tres `@Binds` y el tipo del registro; las alarmas con este tipo lo ignoran (tipo desconocido ya se ignora en sesión).

## Open Questions

- (Resuelto) Marcar sospechosa en `STRICT` tras 3 descartes; regla de fallo y plumbing aprobados.
- ¿Valores de la tabla por nivel y 8 rondas por defecto (rango 3–15) son razonables?
- Plumbing del nivel anti-trampas hacia el renderer: Ritmo y Mantener y soltar lo necesitan también; conviene que lo introduzca una sola sesión (esta, salvo que otra lo haga antes) para evitar conflictos.
