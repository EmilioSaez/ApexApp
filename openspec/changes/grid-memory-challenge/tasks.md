# Tasks

## 1. Dominio

- [x] 1.1 Añadir `GridMemoryLevels` (lado, cuadrados y ms de exposición por nivel) y `GridMemoryChallenge` con `ChallengeType("grid_memory")`
- [x] 1.2 Añadir `GridMemoryInstance` con `check` por conjunto exacto (sin importar el orden)
- [x] 1.3 Tests de dominio: tablas crecientes, nivel acotado, params ignorados, determinismo por semilla, validación y fallo en sesión

## 2. UI

- [ ] 2.1 `GridMemoryRenderer`: tablero, fase de exposición, bloqueo de toques, marcado de aciertos y fallo inmediato
- [ ] 2.2 `GridMemoryParamsEditor` (solo información de dificultad, sin parámetros propios)
- [ ] 2.3 Bindings en `ChallengeModule` y etiquetas en `ChallengeLabels` e `ImportScreen`
- [ ] 2.4 Strings es/en (nombre, prompts, aviso de fallo, descripciones de accesibilidad)

## 3. Verificación

- [ ] 3.1 `./gradlew test assembleDebug`
- [ ] 3.2 Prueba manual en emulador (exposición, fallo, éxito, niveles 1 y 5)
