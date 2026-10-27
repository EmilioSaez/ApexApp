# Proposal

## Why

Los retos actuales (mates, agitar, pasos, QR) cubren cálculo, movimiento y presencia física, pero ninguno exige memoria de trabajo: un usuario medio dormido puede resolverlos casi en piloto automático. Un reto de patrón tipo Simon obliga a prestar atención sostenida y sin teclado ni sensores, por lo que funciona en cualquier dispositivo y no depende de permisos.

## What Changes

- Nuevo reto **memoria / patrón**: se muestra una secuencia de casillas iluminadas y el usuario debe repetirla tocándolas en el mismo orden.
- La dificultad (1..5) fija la longitud de la secuencia y el ritmo de reproducción; la longitud sale de una tabla única, igual que en los retos de movimiento.
- Un fallo reproduce de nuevo la misma secuencia (sin regenerarla ni dar pistas) y cuenta como intento fallido.
- Pantalla de ejecución del reto y editor de parámetros en el editor de alarmas, con el mismo patrón que los demás tipos.
- Strings en español e inglés.

## Capabilities

### New Capabilities
- `memory-challenge`: reto de repetir una secuencia de casillas, con longitud y ritmo según la dificultad.

### Modified Capabilities
<!-- Ninguna: el tipo se registra en el motor de retos existente sin cambiar sus requisitos. -->

## Impact

- **Dominio**: nueva clase `Challenge` + `ChallengeInstance` y variante de `ChallengeAnswer` para el patrón introducido; sin dependencia de hardware, `availability()` siempre disponible.
- **UI**: renderer, editor de parámetros y bindings Hilt en `ChallengeModule`; etiquetas en `ChallengeLabels` e `ImportScreen`.
- **Datos**: sin migración Room; el tipo viaja como `ChallengeSpec("memory")` con `params` opaco. Las versiones antiguas de la app ignorarán el tipo desconocido (se filtra al crear la sesión).
- **Fuera de alcance**: variantes con sonido, modo daltónico dedicado y estadísticas específicas.
