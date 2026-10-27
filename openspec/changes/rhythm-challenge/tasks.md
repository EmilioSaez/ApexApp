# Tasks

## 1. Lógica del reto (dominio)

- [x] 1.1 `RhythmParams` (parse/paramsFor, rango 2..8, defecto 4) y `RhythmTuning` (constantes); verificar con tests JVM de ausente, ilegible, fuera de rango y ida y vuelta
- [x] 1.2 `RhythmPattern.generate` con `Random` inyectado; verificar con tests de longitud, carriles válidos, límite de repetición y patrones distintos entre semillas
- [x] 1.3 `RhythmRound` (aciertos, nota perdida, toque en vacío, 3 fallos → reinicio, una nota se resuelve una sola vez); verificar con tests JVM sin reloj
- [ ] 1.4 `RhythmChallenge`/`RhythmInstance` (`Available`, rondas desde `params`, `check` solo con `GameVerified` de su tipo; requiere el commit de `touch-games-c9`); verificar con tests del patrón de `MotionChallengesTest`

## 2. Integrar el commit compartido

- [ ] 2.0 Con el hash de `touch-games-c9`, traerlo a la rama (merge o cherry-pick); verificar que compila y que sus tests pasan

## 3. UI

- [ ] 3.1 `RhythmRenderer`: carriles, notas cayendo con `withFrameMillis`, progreso de rondas y fallos, aviso de ronda reiniciada; siguiendo `design-taste-compose`; los toques pasan por `TouchPlausibility(level)` antes de `RhythmRound.tap`; verificar con previews, un test con toques sintéticos regulares (descartados) y a mano en el emulador
- [ ] 3.2 `RhythmParamsEditor`: selector de rondas 2..8; verificar con un test de UI/lógica y a mano en el editor, en modo «Con retos»
- [ ] 3.3 Strings es/en (tipo, instrucciones, rondas, fallos, ronda reiniciada); verificar que ambos idiomas tienen las mismas claves

## 4. Integración

- [ ] 4.1 Tres `@Binds @IntoSet` en `ChallengeModule`; verificar con `ChallengeRegistryTest` ampliado y que el tipo aparece en el editor
- [ ] 4.2 Comprobar el modo Normal / Con retos y la convivencia con otros tipos (encadenado y aleatorio); verificar a mano
- [ ] 4.3 Prueba en emulador (`shogun emulator start`): alarma con Ritmo, ~1 min con 4 rondas, fallo → reinicio de ronda, superación apaga la alarma
- [ ] 4.4 Ejecutar `./gradlew testDebugUnitTest` y `assembleDebug` en este worktree; revisar secretos y archivos sobrantes antes de commit
