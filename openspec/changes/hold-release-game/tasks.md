# Tasks

## 1. Lógica de dominio

- [x] 1.1 Crear `domain/challenge/holdrelease/` con los parámetros (`{"rounds":N}`, rango 2..6, defecto 3, parseo tolerante) y la tabla de tiempos y ventanas por nivel; verificar con `HoldReleaseParamsTest` (válido, vacío, corrupto, fuera de rango).
- [x] 1.2 Implementar la máquina de rondas (señal aleatoria, ventana, soltar pronto/tarde, ronda extra con tope, reinicio de ronda tras 3 fallos, cancelación = fallo) con reloj y `Random` inyectados; verificar con `HoldReleaseGameTest` cubriendo los escenarios del spec.
- [x] 1.3 Añadir los datos de pulsación que consume el validador (tiempos de reacción) a la máquina de rondas; verificar con test de que se exponen por ronda.

## 2. Integración del commit compartido

- [x] 2.1 Integrar en la rama el commit de los juegos táctiles (merge o cherry-pick del hash que indique el usuario); verificar que compila y que `git log` muestra `TouchPlausibility` y `ChallengeAnswer.GameVerified`.
- [x] 2.2 Añadir `HoldReleaseChallenge` (`Challenge` + `ChallengeInstance`) que acepta `GameVerified` de su tipo; verificar con tests de `check` y de aleatoriedad por semilla.

## 3. UI y registro

- [x] 3.1 Crear el renderer táctil (control, señal visual y textual, progreso, fallo por pérdida de foco, validación con `TouchPlausibility` de superposición, origen y duraciones idénticas) y el editor de rondas con duración estimada; verificar que compila y que los tests de UI existentes siguen pasando.
- [x] 3.2 Registrar el tipo en `ChallengeModule` (challenge, renderer, editor) y añadir su etiqueta en `ChallengeLabels` e `ImportScreen`; verificar con `ChallengeRegistryTest` y el test de importación de alarmas compartidas.
- [x] 3.3 Añadir los strings en `values/strings.xml` y `values-en/strings.xml`; verificar que no faltan claves en ningún idioma.

## 4. Integración y documentación

- [ ] 4.1 Documentar el reto junto al resto de retos en `docs/` o README; verificar que la descripción coincide con el spec.
- [ ] 4.2 Tras `shogun emulator start`, probar en el emulador Con retos → Mantener y soltar → fallo → superar, y ejecutar `./gradlew testDebugUnitTest`; verificar que el flujo funciona y los tests pasan.
