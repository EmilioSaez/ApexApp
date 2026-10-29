# Proposal

## Why

El reto de memoria con casillas exige recordar posiciones, pero el estímulo es siempre el mismo: un usuario que lo ha resuelto varias veces lo hace por memoria muscular. Un reto de secuencia de formas obliga a recordar identidades y orden de símbolos distintos y a elegirlos de una paleta, de modo que cada sonada es una prueba nueva. No usa teclado, sensores ni permisos.

## What Changes

- Nuevo reto **secuencia de formas**: se muestran varias formas (círculo, cuadrado, triángulo, rombo, cruz, hexágono) una tras otra y el usuario debe elegirlas, de una paleta, en el mismo orden en que salieron.
- La dificultad (1..5) fija la longitud de la secuencia, el número de formas distintas de la paleta y el tiempo que se muestra cada forma, desde una tabla única.
- Un fallo cuenta como intento fallido y repite la misma secuencia, sin pistas.
- Pantalla de ejecución del reto y editor de parámetros en el editor de alarmas, con el mismo patrón que los demás tipos.
- Strings en español e inglés.

## Capabilities

### New Capabilities
- `shape-sequence-challenge`: reto de repetir una secuencia de formas, con longitud, formas distintas y ritmo según la dificultad.

### Modified Capabilities
<!-- Ninguna: el tipo se registra en el motor de retos existente sin cambiar sus requisitos. -->

## Impact

- **Dominio**: nueva `Challenge` + `ChallengeInstance` en `domain/challenge/shapes`; reutiliza `ChallengeAnswer.Pattern` (los `taps` son el `ordinal` de cada forma). Sin hardware: `availability()` siempre disponible.
- **UI**: renderer, editor de parámetros y bindings Hilt en `ChallengeModule`; etiquetas en `ChallengeLabels` e `ImportScreen`.
- **Datos**: sin migración Room; el tipo viaja como `ChallengeSpec("shapes")` con `params` opaco. Las versiones antiguas de la app ignorarán el tipo desconocido (se filtra al crear la sesión).
- **Fuera de alcance**: sonido, modo daltónico dedicado y estadísticas específicas.
