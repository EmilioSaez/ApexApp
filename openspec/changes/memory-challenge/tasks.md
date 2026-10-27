# Tasks

## 1. Dominio

- [x] 1.1 Añadir `MemoryLevels` (longitud y ms por casilla por nivel) y `MemoryChallenge` con `ChallengeType("memory")`
- [x] 1.2 Añadir `ChallengeAnswer.Pattern` y `MemoryInstance` con `check` por orden exacto
- [x] 1.3 Tests de dominio: tabla creciente, nivel acotado, params ignorados, secuencia estable en la instancia, respuesta correcta e incorrecta

## 2. UI

- [ ] 2.1 `MemoryRenderer` con malla 3×3, reproducción, bloqueo de toques durante la animación y aviso de fallo
- [ ] 2.2 `MemoryParamsEditor` (solo información de dificultad, sin parámetros propios)
- [ ] 2.3 Bindings en `ChallengeModule` y etiquetas en `ChallengeLabels` e `ImportScreen`
- [ ] 2.4 Strings es/en (nombre, prompt, aviso de fallo, descripciones de accesibilidad)

## 3. Verificación

- [ ] 3.1 Test de sesión: el reto aparece en el registro y el editor, y una sesión mixta con memoria termina
- [ ] 3.2 `./gradlew test assembleDebug` y prueba manual en emulador (reproducción, fallo, éxito)
