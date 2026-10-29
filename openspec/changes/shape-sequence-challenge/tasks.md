# Tasks

## 1. Dominio

- [x] 1.1 Añadir `ShapeKind`, `ShapeSequenceLevels` (longitud, formas distintas y ms por forma por nivel) y `ShapeSequenceChallenge` con `ChallengeType("shapes")`
- [x] 1.2 Añadir `ShapeSequenceInstance` con `check` por orden exacto sobre `ChallengeAnswer.Pattern` y `keepOnFailure`
- [x] 1.3 Tests de dominio: tablas crecientes, nivel acotado, params ignorados, paleta coherente, respuesta correcta e incorrecta, fallo de sesión

## 2. UI

- [ ] 2.1 `ShapeSequenceRenderer`: fase de muestra (una forma cada vez), fase de elección con paleta, bloqueo de toques durante la muestra y aviso de fallo
- [ ] 2.2 `ShapeSequenceParamsEditor` (solo información de dificultad, sin parámetros propios)
- [ ] 2.3 Bindings en `ChallengeModule` y etiquetas en `ChallengeLabels` e `ImportScreen`
- [ ] 2.4 Strings es/en (nombre, aviso de fallo, nombres de forma para accesibilidad, info de dificultad)

## 3. Verificación

- [x] 3.1 Test de sesión: el reto aparece en el registro y una sesión con formas termina
- [ ] 3.2 `./gradlew test assembleDebug` y prueba manual en emulador (muestra, fallo, éxito)
