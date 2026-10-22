# Tasks

## 1. Algoritmo puro

- [x] 1.1 Crear `SessionSummary`, `AdaptiveDifficultyConfig` (constantes por defecto) y `AdaptiveDecision` con `ChangeReason`; verificar que compila con `./gradlew :app:compileDebugKotlin`
- [x] 1.2 Implementar `AdaptiveDifficultyCalculator` (ventana de 5, mínimo 3, reglas de subida/bajada, bajada prioritaria, límites mín/máx, histéresis por `lastChangeSessionId`) y verificar con tests unitarios en `app/src/test` que cubren cada escenario de las specs (historial insuficiente, subida por snoozes, subida por reto fácil, bajada por fallos, contradicción, límites, estabilidad tras cambio)
- [x] 1.3 Excluir sesiones sin resultado fiable (no atendidas, sospechosas, alarma desactivada) y verificarlo con tests unitarios

## 2. Persistencia

- [ ] 2.1 Añadir `AdaptiveSettingsEntity` y `AdaptiveSettingsDao` (FK con `CASCADE` a `alarms`), registrarlos en `ApexDatabase` y verificar que se genera el esquema exportado
- [ ] 2.2 Escribir la migración Room aditiva v(n+1) sobre la versión vigente en el momento de implementar (tras el merge de `wake-stats`; `alarm-core` fija v2) y verificar con un test de migración instrumentado que las alarmas existentes se conservan
- [ ] 2.3 Crear `AdaptiveSettingsRepository` (observar, guardar, reiniciar, validar mín ≤ máx) y verificar con tests de repositorio, incluyendo borrado en cascada y persistencia tras reabrir la base

## 3. Integración con dependencias

- [ ] 3.1 Definir el puerto `WakeHistorySource` y la escala fija 1..5 (0 = sin definir → 3, fuera de rango se acota) y verificar que el motor se prueba con fakes sin depender de `wake-stats` ni `challenge-engine`
- [ ] 3.2 Implementar `AdaptiveDifficultyEngine` (`levelFor`, `onSessionFinished`, `reset`) con fallback al último nivel o a la dificultad base y verificar con tests unitarios, incluido el caso de fallo de lectura del historial
- [ ] 3.3 Con `challenge-engine` y `wake-stats` ya integrados, escribir el adaptador real de `wake-stats`, implementar `AdaptiveDifficultyProvider` (cacheado por `sessionId`) sustituyendo el binding de Hilt de `DifficultyProvider`, llamar a `onSessionFinished` al cierre de sesión, y verificar con un test instrumentado que un disparo usa el nivel adaptativo y que con el ajuste desactivado usa el fijo

## 4. Interfaz y explicación

- [ ] 4.1 Añadir en `AlarmEditorScreen` la sección con interruptor, texto explicativo, selector de rango y botón de reiniciar, y verificar con tests de Compose que el rango inválido se rechaza y que el interruptor persiste
- [ ] 4.2 Mostrar nivel actual y motivo del último cambio en el editor, con strings localizados desde `ChangeReason`, y verificar con test de Compose para cada motivo
- [ ] 4.3 Mostrar la línea de aviso en la pantalla del reto cuando el nivel cambia y verificar con test de Compose (visible solo si hay cambio)

## 5. Verificación de conjunto

- [ ] 5.1 Ejecutar `./gradlew :app:testDebugUnitTest` y los tests instrumentados en el emulador y verificar que pasan; recorrer manualmente activar → varios snoozes → subida de nivel → explicación visible
