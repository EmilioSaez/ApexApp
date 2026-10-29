# Design

## Context

Mismo motor que `memory-challenge`: `Challenge` (fábrica sin estado) y `ChallengeInstance` se registran vía Hilt `@IntoSet` en `ChallengeModule` junto con su `ChallengeRenderer` y `ChallengeParamsEditor`. La sesión filtra los tipos desconocidos y recurre a `math` si falta uno. Ver proposal.md.

## Goals / Non-Goals

**Goals:**
- Lógica pura y determinista (conjunto generado por semilla vía `Random`, validación sin UI) con tests unitarios.
- Mismo patrón de registro, renderer y editor que los retos existentes; sin migración Room.

**Non-Goals:**
- Sonido, vibración por casilla o accesibilidad avanzada más allá de `contentDescription`.
- Tocar `memory` o el motor de sesión.

## Decisions

- **Tabla única por nivel** (`GridMemoryLevels`): lado 3,4,4,5,6; cuadrados a recordar 3,4,6,8,10; exposición 1500→1100 ms. Alternativa descartada: parámetros libres en `params` (permitirían configurar la alarma en trivial).
- **Conjunto, no secuencia**: `GridMemoryInstance.cells: Set<Int>`; `check` compara el conjunto de toques y exige el mismo número de toques, de modo que repetir una casilla no basta. Se generan barajando `0 until side²` con el `Random` inyectado, sin repeticiones.
- **Respuesta**: se reutiliza `ChallengeAnswer.Pattern(type, taps)` (ya existe para `memory`), así no se toca el motor. El orden de `taps` se ignora.
- **Fallo = mismo patrón**: `keepOnFailure = true`; la instancia conserva el conjunto y es el renderer quien reinicia la fase de exposición. La UI envía la respuesta en cuanto se toca un cuadrado equivocado (fallo inmediato) o cuando se completa el conjunto (acierto); los cuadrados correctos tocados quedan marcados hasta el final del intento.
- **Identificador `grid_memory`**: `ChallengeType("grid_memory")`; `params` opacos, ignorados.

## Risks / Trade-offs

- Con el tablero grande (6×6) en pantallas pequeñas las casillas son menores: se calcula el tamaño de casilla a partir del ancho disponible.
- Sin botón "ver otra vez" (decisión consciente: sin ayudas); el fallo repite la exposición.
- Alarmas compartidas con este tipo abiertas en versiones antiguas quedan sin ese reto y recurren a mates.
