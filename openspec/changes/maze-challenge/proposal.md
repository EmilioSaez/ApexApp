# Proposal

## Why

Los retos de memoria, reflejos y ritmo exigen atención, pero ninguno pide coordinación sostenida entre vista y mano: basta con tocar. Un laberinto que hay que recorrer deslizando el dedo en las cuatro direcciones obliga a mirar y planificar la ruta durante varios segundos, sin teclado, sensores ni permisos, por lo que funciona en cualquier dispositivo.

## What Changes

- Nuevo reto **laberinto**: se genera un laberinto con una única solución y el usuario hace swipe (norte, sur, oeste, este) y la ficha con el icono de alarma se desliza animada, celda a celda, hasta chocar con un muro; así hasta llegar a la salida.
- Un swipe contra un muro no mueve la ficha y no cuenta como fallo; el reto no tiene intentos fallidos (ver design.md).
- La dificultad (1..5) fija el tamaño del laberinto con una tabla única; el tamaño máximo y la celda mínima garantizan que sigue siendo tocable en pantallas pequeñas.
- Pantalla de ejecución del reto y editor de parámetros en el editor de alarmas, con el mismo patrón que los demás tipos.
- Strings en español e inglés.

## Capabilities

### New Capabilities
- `maze-challenge`: reto de recorrer un laberinto con swipes, con tamaño según la dificultad.

### Modified Capabilities
<!-- Ninguna: el tipo se registra en el motor de retos existente sin cambiar sus requisitos. -->

## Impact

- **Dominio**: nuevo paquete `domain/challenge/maze` (`Maze` con `slide`, `MazeDirection`, `MazeChallenge`) y la respuesta `MazeTrace` (archivo propio, variante de `ChallengeAnswer`); sin dependencia de hardware, `availability()` siempre disponible.
- **UI**: renderer, editor de parámetros y bindings Hilt en `ChallengeModule`; etiquetas en `ChallengeLabels` e `ImportScreen`.
- **Datos**: sin migración Room; el tipo viaja como `ChallengeSpec("maze")` con `params` opaco que se ignora. Las versiones antiguas de la app ignorarán el tipo desconocido (se filtra al crear la sesión).
- **Fuera de alcance**: laberintos con varias salidas, temporizador, sonido o vibración al chocar y estadísticas específicas.
