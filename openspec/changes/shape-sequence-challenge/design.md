# Design

## Context

El motor de retos define `Challenge` (fábrica sin estado) y `ChallengeInstance` (`prompt`, `check(answer)`); los tipos se registran vía Hilt `@IntoSet` en `ChallengeModule` junto con su `ChallengeRenderer` y `ChallengeParamsEditor`. `memory-challenge` es la plantilla: tabla por nivel, `params` ignorados y `keepOnFailure` para repetir la misma secuencia. Ver proposal.md para la motivación.

## Goals / Non-Goals

**Goals:**
- Reto jugable offline, sin permisos y determinista en tests (semilla inyectada vía `Random`).
- Mismo patrón de registro, renderer y editor que los retos existentes.

**Non-Goals:**
- Sonido, vibración o accesibilidad avanzada más allá de `contentDescription`.
- Cambiar el motor de sesión ni la política anti-trampas.

## Decisions

- **Tabla única por nivel** (`ShapeSequenceLevels`): longitud 3→7, formas distintas 3→5 (3, 3, 4, 5, 5) y ms por forma 1200→700. Alternativa descartada: parámetros libres en `params`, que permitirían dejar la alarma en trivial.
- **Formas como enum** (`ShapeKind`: círculo, cuadrado, triángulo, estrella, rombo) con el `ordinal` como identificador en la respuesta. Se pueden añadir formas al final; reordenarlas cambiaría el significado de las respuestas.
- **Paleta por instancia**: se eligen al azar `distinct(level)` formas del enum, ordenadas por `ordinal` para que la disposición de la paleta sea estable y no delate la secuencia. La secuencia solo usa formas de la paleta.
- **Reutilizar `ChallengeAnswer.Pattern`**: `taps` lleva los `ordinal` pulsados; evita tocar la jerarquía sellada y reduce conflictos al fusionar. La instancia compara con la secuencia esperada, así `check` es puro y testeable.
- **Fallo = misma secuencia**: `keepOnFailure = true`; el estado de la muestra vive en el composable y la instancia solo guarda secuencia, paleta y ritmo.
- **Identificador `shapes`**: `ChallengeType("shapes")`; los `params` son opacos y se ignoran.

## Risks / Trade-offs

- Las formas deben distinguirse por silueta y no solo por color: la UI usa forma + `contentDescription`.
- Sin botón "ver otra vez" (decisión consciente: sin ayudas); la muestra repetida tras fallo lo mitiga.
- Alarmas compartidas con este tipo abiertas en versiones antiguas quedan sin ese reto y recurren a mates.
