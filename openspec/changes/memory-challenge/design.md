# Design

## Context

El motor de retos define `Challenge` (fábrica sin estado) y `ChallengeInstance` (`prompt`, `check(answer)`); los tipos se registran vía Hilt `@IntoSet` en `ChallengeModule` junto con su `ChallengeRenderer` y `ChallengeParamsEditor`. Los retos de movimiento sacan su objetivo de una tabla por nivel (`MotionTargets`) e ignoran los `params` guardados. La sesión filtra los tipos desconocidos y recurre a `math` si falta uno. Ver proposal.md para la motivación.

## Goals / Non-Goals

**Goals:**
- Reto jugable offline, sin permisos y determinista en tests (semilla inyectada vía `Random`).
- Mismo patrón de registro, renderer y editor que los retos existentes.

**Non-Goals:**
- Sonido, vibración por casilla o accesibilidad avanzada más allá de contentDescription.
- Cambiar el motor de sesión ni la política anti-trampas.

## Decisions

- **Tabla única por nivel** (`MemoryLevels`): longitud 3→7 y tiempo por casilla 700→400 ms en una malla de 3×3. Alternativa descartada: parámetro libre en `params`, que permitiría configurar la alarma en trivial y ya se evitó en los retos de movimiento.
- **Verificación local con respuesta propia**: `ChallengeAnswer.Pattern(type, taps: List<Int>)`; la instancia compara con la secuencia esperada. Alternativa: que la UI valide y envíe `MotionVerified`; se descarta porque `check` queda sin lógica y no se puede testear en dominio.
- **Fallo = misma secuencia**: el estado de la reproducción vive en el composable/ViewModel, la instancia solo guarda la secuencia. Mantiene `check` puro y evita que un fallo regenere una secuencia más fácil.
- **Malla 3×3 fija**: casillas con índice 0..8 y colores del tema (rojo/negro), con la posición como señal además del color.
- **Identificador `memory`**: `ChallengeType("memory")`; los `params` son opacos y se ignoran por ahora.

## Risks / Trade-offs

- Con la pantalla en baja visibilidad la animación puede pasar desapercibida: la reproducción repetida tras fallo lo mitiga, pero no hay botón "ver otra vez" (decisión consciente: sin ayudas).
- Alarmas compartidas con este tipo abiertas en versiones antiguas quedan sin ese reto y recurren a mates.
