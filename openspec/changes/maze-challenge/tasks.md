# Tasks

## 1. Dominio

- [x] 1.1 Añadir `Maze` (generación por semilla con solución única), `MazeLevels` (tamaño por nivel) y `MazeLayout` (celda y pared en dp)
- [x] 1.2 Añadir `MazeTracker` (inicio en la entrada, interpolación, pared, salida) y la respuesta `MazeTrace`
- [x] 1.3 Añadir `MazeChallenge` con `ChallengeType("maze")` y `MazeInstance` con `check` por camino válido
- [x] 1.4 Tests de dominio: determinismo, árbol perfecto y solución única, tabla creciente y celda mínima, pared y borde, movimiento rápido, soltar/reiniciar, respuesta correcta e incorrecta, params ignorados

## 2. UI

- [x] 2.1 `MazeRenderer` con laberinto dibujado en Canvas, icono de alarma en la entrada, trazo con el dedo, reinicio al chocar o soltar y aviso de fallo
- [x] 2.2 `MazeParamsEditor` (solo información de dificultad, sin parámetros propios)
- [x] 2.3 Bindings en `ChallengeModule` y etiquetas en `ChallengeLabels` e `ImportScreen`
- [x] 2.4 Strings es/en (nombre, pista, avisos, descripciones de accesibilidad)

## 3. Verificación

- [x] 3.1 Test de sesión: el reto aparece en el registro y una sesión con laberinto termina
- [ ] 3.2 `./gradlew test assembleDebug` y prueba manual en emulador (trazo, pared, soltar, llegada)
