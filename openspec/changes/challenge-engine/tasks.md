# Tasks

## 1. Contrato y configuración de retos (dominio)

- [x] 1.1 Crear en `domain/challenge` `ChallengeMode`, `ChallengeType`, `SessionConfig` (dificultad acotada a 1..5, `require(count in 1..10)`), `ChallengeAnswer`, `Availability`, `Challenge` y `ChallengeInstance`; verificar con tests JVM de `SessionConfig` (acotado, rango de nº de retos) con `./gradlew :app:testDebugUnitTest`
- [x] 1.2 Añadir `count: Int = 1` y `mode` (default `CHAINED`) a la `ChallengeConfig` de alarm-core (sin columnas ni migración) e implementar `SessionConfig.from(ChallengeConfig)` (tipos con `ChallengeSpec.params`, dificultad con 0 = 3); verificar con tests: config solo con tipos y dificultad da 1 reto encadenado, JSON antiguo sin `count`/`mode` se deserializa. Requiere alarm-core fusionado
- [x] 1.3 Crear `ChallengeRegistry` (tipo → `Challenge` + UI) y verificar con test que un tipo registrado se resuelve y uno desconocido se ignora

## 2. Reto de matemáticas

- [x] 2.1 Implementar `MathChallenge` con plantillas por nivel 1..5 desde el resultado y validación de `ChallengeAnswer.Text`; verificar con tests con `Random` semillado: resultado entero, sin divisiones inexactas, complejidad creciente, entrada vacía o no numérica rechazada

## 3. Motor de sesión y ganchos

- [x] 3.1 Crear `DifficultyProvider` + `FixedDifficultyProvider` y `ChallengeEvent` + `ChallengeEventSink` + `NoOpChallengeEventSink`; verificar con test que el proveedor por defecto devuelve el nivel base
- [x] 3.2 Implementar `ChallengeSession` (estados, `submit`, regeneración tras fallo, selección encadenada en ciclo y aleatoria, `sessionId` recibido, tipos desconocidos ignorados, entrega de `params` solo a su tipo, emisión de eventos); verificar con tests: orden A,B,A, aleatorio dentro de activados, fallo no avanza, `Passed` solo tras el último, secuencia de eventos con el `sessionId` dado
- [x] 3.3 Implementar la reserva (`availability()` y aviso durante la ejecución → otro tipo habilitado, o `MATH`, con `ChallengeFellBack`); verificar con tests de ambos casos y de config sin tipos utilizables
- [x] 3.4 Añadir `di/ChallengeModule.kt` con el registro (solo `MATH`) y los bindings por defecto; verificar que `./gradlew :app:assembleDebug` compila con el grafo de Hilt
- [x] 3.5 Implementar un `DismissGate` que solo permita descartar con la sesión de la alarma superada (sin activarlo en el flujo real hasta coordinarlo con alarm-reliability/anti-cheat); verificar con test unitario de la puerta

## 4. UI de sesión y pantalla de prueba

- [ ] 4.1 Crear `ChallengeSessionViewModel` y `ChallengeSessionScreen` reutilizable (progreso `n/total`, enunciado, campo numérico, confirmar, aviso de error, estado superado) con strings con prefijo `challenge_` en `res/values` y `res/values-en` (MissingTranslation es error de lint) y colores/estilos solo vía `MaterialTheme`; verificar con test de ViewModel y revisión en emulador
- [ ] 4.2 Crear `ChallengeTestScreen` (dificultad 1..5, nº de retos, modo, tipos) y `ChallengeTestRoute` en `AppNavigation`, accesible desde una acción adicional en el slot `actions` de la barra de `HomeScreen`, junto a la de Ajustes de design-system-stitch, con callback propio `onOpenChallengeTest` y `ChallengeTestRoute` (`data object` + `composable<>`) junto a `SettingsRoute` (rebasado antes sobre design-system-stitch para no pisar sus cambios en `HomeScreen.kt` y `AppNavigation.kt`); verificar en emulador (`shogun emulator start`) que se ejecuta una sesión completa, se vuelve atrás y no se crea ni programa ninguna alarma
- [ ] 4.3 Aplicar la skill `design-taste-compose` y verificar modo claro y oscuro en el emulador

## 5. Integración

- [x] 5.1 Ejecutar `./gradlew :app:testDebugUnitTest :app:assembleDebug` y verificar que todo pasa
- [x] 5.2 Ejecutar `openspec validate challenge-engine` y verificar que no hay errores

## Workflow follow-up

- Rebasar sobre `develop` tras alarm-core y alarm-reliability (orden de merge: 4.º) y ajustar `SessionConfig.from` a los nombres finales de `ChallengeConfig`.
- Archivar el change tras la revisión de la PR.
