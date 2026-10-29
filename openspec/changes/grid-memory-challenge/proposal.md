# Proposal

## Why

El reto `memory` (Simon) exige recordar una secuencia ordenada. Falta un ejercicio de memoria espacial: recordar *dónde* se iluminó algo, sin importar el orden. Es una carga cognitiva distinta, sin teclado ni sensores, y funciona en cualquier dispositivo sin permisos.

## What Changes

- Nuevo reto **memoria de cuadrícula**: se muestra un tablero de cuadrados, se iluminan a la vez un conjunto de ellos y, al apagarse, el usuario debe tocar exactamente los que se iluminaron (el orden no importa).
- La dificultad (1..5) fija el tamaño del tablero (3×3 hasta 6×6) y cuántos cuadrados hay que recordar, desde una tabla única.
- Tocar un cuadrado que no estaba iluminado cuenta como intento fallido y vuelve a mostrar el mismo patrón, sin pistas.
- Pantalla de ejecución y editor de parámetros en el editor de alarmas, con el mismo patrón que los demás tipos.
- Strings en español e inglés.
- El reto `memory` existente no cambia.

## Capabilities

### New Capabilities
- `grid-memory-challenge`: reto de recordar y tocar un conjunto de cuadrados iluminados, con tablero y cantidad según la dificultad.

### Modified Capabilities
<!-- Ninguna: el tipo se registra en el motor de retos existente sin cambiar sus requisitos. -->

## Impact

- **Dominio**: nueva clase `Challenge` + `ChallengeInstance` en `domain/challenge/gridmemory`; reutiliza `ChallengeAnswer.Pattern` (las casillas tocadas), sin cambios en el motor.
- **UI**: renderer, editor de parámetros y bindings Hilt en `ChallengeModule`; etiquetas en `ChallengeLabels` e `ImportScreen`.
- **Datos**: sin migración Room; el tipo viaja como `ChallengeSpec("grid_memory")` con `params` opaco. Las versiones antiguas ignorarán el tipo desconocido (se filtra al crear la sesión).
- **Fuera de alcance**: sonido, modo daltónico dedicado, estadísticas específicas y cualquier cambio en `memory`.
