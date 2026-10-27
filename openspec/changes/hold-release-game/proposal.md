# Proposal

## Why

Los retos actuales (Mates, QR, Agitar, Pasos, Levantarse) cubren cálculo, cámara y movimiento, pero ninguno exige reflejos con la atención despierta. «Mantener y soltar» añade un reto táctil corto (~1 min) que obliga a esperar una señal y reaccionar a tiempo, difícil de superar medio dormido.

## What Changes

- Nuevo tipo de reto `holdrelease` («Mantener y soltar»): el usuario mantiene pulsado un control hasta que una señal cambia y debe soltar dentro de la ventana de tiempo correcta; se superan N rondas.
- Parámetro único por alarma: número de rondas (`ChallengeSpec.params`), con editor propio en la sección «Retos»; por defecto 3 rondas (~1 min).
- Aleatoriedad por sesión: el instante de la señal de cada ronda se sortea de nuevo en cada ronda y sesión.
- Fallo (soltar antes de la señal o tarde): penalización sin reinicio total; la ronda se repite y se añade una ronda extra (con tope). Tras 3 fallos seguidos en una ronda se reinicia solo esa ronda.
- Sin nivel anti-trampas propio: hereda el lockdown y la integridad de sesión de la alarma y valida las pulsaciones con el validador táctil compartido `TouchPlausibility(level)` según el nivel de la alarma (introducido por el change de juegos táctiles; este change no lo duplica).
- Cadenas en los mismos idiomas/recursos que los retos actuales (es, en).

## Capabilities

### New Capabilities
- `hold-release-challenge`: reglas del reto de mantener y soltar (rondas, señal aleatoria, ventana de acierto, penalización por fallo, configuración y disponibilidad).

### Modified Capabilities

Ninguna: el editor de parámetros por tipo y el motor de retos ya cubren lo necesario.

## Impact

- Código: `domain/challenge/holdrelease/` (lógica pura + `Challenge`), `ui/challenge/` (renderer y editor de params), `ChallengeModule` (registro), `ChallengeLabels` e `ImportScreen` (etiqueta), `strings.xml` es/en.
- Dependencia: el commit compartido de los juegos táctiles aporta `TouchPlausibility`, el paso del nivel al renderer, `SuspicionReason.SYNTHETIC_INPUT` y `ChallengeAnswer.GameVerified(type)`. Este change los consume y no añade variante de respuesta propia.
- Puntos compartidos con los otros juegos: registro Hilt, etiquetas y strings; se mantienen como añadidos mínimos en bloque propio.
- Sin dependencias nuevas ni migración (tipo y params se guardan como texto).
