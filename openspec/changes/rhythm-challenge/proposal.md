# Proposal

## Why

Los retos actuales piden cálculo (Mates), un objeto físico (QR) o movimiento del cuerpo (Agitar, Pasos, Levantarse). Falta uno que exija atención sostenida y reflejos sin depender de sensores ni permisos: seguir un ritmo con los dedos mantiene al usuario despierto y funciona en cualquier dispositivo.

## What Changes

- Nuevo reto **Ritmo**: notas que caen por tres carriles y hay que tocar el carril correcto cuando la nota llega a la línea de impacto.
- Dificultad configurable por alarma con un único parámetro: el **número de rondas** (guardado en `ChallengeSpec.params`). Con los valores por defecto el reto dura ~1 min.
- Un fallo no reinicia el reto entero: al acumular un número fijo de fallos en una ronda, solo esa ronda se reinicia con un patrón nuevo.
- El patrón de notas se genera al azar en cada sesión, para que no se pueda memorizar.
- Sigue el modo anti-trampas de la alarma tal cual (bloqueo de pantalla y salidas contadas por `session-integrity`); no añade un nivel propio.
- Integración en el editor de retos (selector de tipo y editor de rondas) y en el modo Normal / Con retos; textos en los mismos idiomas que los retos actuales (es, en).

## Capabilities

### New Capabilities
- `rhythm-challenge`: reto de ritmo: generación de rondas, reglas de acierto, fallo y reinicio de ronda, parámetro de rondas por alarma y su UI de ejecución y de edición.

### Modified Capabilities
<!-- Ninguna: challenge-editor y challenge-engine ya admiten tipos nuevos sin cambiar sus requisitos. -->

## Impact

- **Código nuevo**: `domain/challenge/rhythm/` (reto, parámetros, generador de patrones, lógica de ronda) y `ui/challenge/rhythm/` (renderer y editor de rondas).
- **Puntos compartidos (cambio mínimo)**: `ChallengeModule` (tres `@Binds @IntoSet`: `Challenge`, `ChallengeRenderer`, `ChallengeParamsEditor`), `strings.xml` en `values` y `values-en` (bloque propio al final de los retos). Sin cambios en Room, en `ChallengeSession` ni en el editor.
- **Sin dependencias ni permisos nuevos.** Disponible siempre (`Availability.Available`).
- **Fuera de alcance**: música/audio sincronizado, niveles de dificultad por velocidad (la dificultad 1–5 de la alarma no escala este reto), ranking o estadísticas propias, los juegos «Tocar objetivos» y «Mantener y soltar» (changes aparte).
