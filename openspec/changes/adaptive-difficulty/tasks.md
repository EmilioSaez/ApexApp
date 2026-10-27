# Tasks

## 1. Algoritmo puro

- [x] 1.1 Crear `SessionSummary`, `AdaptiveDifficultyConfig` (constantes por defecto) y `AdaptiveDecision` con `ChangeReason`; verificar que compila con `./gradlew :app:compileDebugKotlin`
- [x] 1.2 Implementar `AdaptiveDifficultyCalculator` (ventana de 5, mínimo 3, reglas de subida/bajada, bajada prioritaria, límites mín/máx, histéresis por `lastChangeSessionId`) y verificar con tests unitarios en `app/src/test` que cubren cada escenario de las specs (historial insuficiente, subida por snoozes, subida por reto fácil, bajada por fallos, contradicción, límites, estabilidad tras cambio)
- [x] 1.3 Excluir sesiones sin resultado fiable (no atendidas, sospechosas, alarma desactivada) y verificarlo con tests unitarios

## 2. Persistencia

- [x] 2.1 Añadir `AdaptiveSettingsEntity` y `AdaptiveSettingsDao` (FK con `CASCADE` a `alarms`), registrarlos en `ApexDatabase` y verificar que se genera el esquema exportado
- [x] 2.2 Escribir la migración Room aditiva v(n+1) sobre la versión vigente en el momento de implementar (tras el merge de `wake-stats`; `alarm-core` fija v2) y verificar con un test de migración instrumentado que las alarmas existentes se conservan
- [x] 2.3 Crear `AdaptiveSettingsRepository` (observar, guardar, reiniciar, validar mín ≤ máx) y verificar con tests de repositorio, incluyendo borrado en cascada y persistencia tras reabrir la base

## 3. Integración con dependencias

- [x] 3.1 Definir el puerto `WakeHistorySource` y la escala fija 1..5 (0 = sin definir → 3, fuera de rango se acota) y verificar que el motor se prueba con fakes sin depender de `wake-stats` ni `challenge-engine`
- [x] 3.2 Implementar `AdaptiveDifficultyEngine` (`levelFor`, `onSessionFinished`, `reset`) con fallback al último nivel o a la dificultad base y verificar con tests unitarios, incluido el caso de fallo de lectura del historial
- [x] 3.3 Con `challenge-engine` y `wake-stats` ya integrados, escribir el adaptador real de `wake-stats`, implementar `AdaptiveDifficultyProvider` (cacheado por `sessionId`) sustituyendo el binding de Hilt de `DifficultyProvider`, llamar a `onSessionFinished` al cierre de sesión, y verificar con tests unitarios (`AdaptiveDifficultyEngineTest`, `AdaptiveSessionListenerTest`) que el reto usa el nivel adaptativo, que con el ajuste desactivado usa el fijo y que al cerrar la sesión se evalúa el ajuste; la comprobación instrumentada de extremo a extremo queda en 5.1

## 4. Interfaz y explicación

- [x] 4.1 Añadir en `AlarmEditorScreen` la sección con interruptor, texto explicativo, selector de rango y botón de reiniciar, y verificar con tests de Compose que el rango inválido se rechaza y que el interruptor persiste
- [x] 4.2 Mostrar nivel actual y motivo del último cambio en el editor, con strings localizados desde `ChangeReason`, y verificar con test de Compose para cada motivo
- [x] 4.3 Mostrar la línea de aviso en la pantalla del reto cuando el nivel cambia y verificar con test de Compose (visible solo si hay cambio)

## 5. Verificación de conjunto

- [ ] 5.1 Ejecutar `./gradlew :app:testDebugUnitTest` y los tests instrumentados en el emulador y verificar que pasan; ejecutar en emulador los tests instrumentados pendientes (2.2, 2.3, 4.1–4.3) y recorrer manualmente activar → varios snoozes → subida de nivel → explicación visible

## Estado de la implementación

Suite instrumentada ejecutada sobre `develop` (e50cb8f más los arreglos de la PR #14) en emulador API 36: 57 de 57 en verde. Sin emulador en este worktree.

- Marcadas por sus tests: 2.2 (`MigrationTest.migrate4To5_*`, además de 1→2, 2→3 y 3→4), 2.3 (`AdaptiveSettingsRepositoryTest`: lectura completa, actualización, fila ausente, borrado en cascada y persistencia tras reabrir la base) y 4.2 (`AdaptiveDifficultyContentTest`: nivel actual y motivo en el editor, y el texto de cada `ChangeReason`).
- 4.1 marcada: `AdaptiveViewModelsTest` comprueba que el interruptor persiste y se refleja en el estado, que un rango válido se guarda y uno inválido se rechaza conservando el anterior, y que una alarma nueva no guarda nada; `AdaptiveDifficultyContentTest.movingTheRangeSliderNeverProducesAnInvalidRange` mueve el `RangeSlider` y comprueba que nunca emite mín > máx. Revisada a ojo en emulador (API 36): la sección aparece tras Retos y el interruptor sigue activado al reabrir la alarma.
- 4.3 marcada: `ChallengeSessionContent` muestra el aviso solo cuando recibe uno (`challengeScreenShowsTheNoticeOnlyWhenTheLevelChanged`, `challengeScreenHasNoNoticeWithoutAChange`), y `AdaptiveViewModelsTest` cubre que el aviso se consume una vez y no aparece sin cambio, con ajuste desactivado o sin alarma.
- 5.1 sin marcar: `testDebugUnitTest` y la suite instrumentada (76 de 76, emulador API 36) pasan, pero falta el recorrido manual activar → varios snoozes → subida de nivel → explicación visible.
- Decisión: el rango inválido (mín > máx) se rechaza en el motor (`setRange`); en la UI el `RangeSlider` no puede producirlo.
- Decisión: en una alarma nueva (sin id) la sección aparece desactivada con el aviso "Guarda la alarma para poder activarla", porque el estado cuelga del id de la alarma.
- Integración: migración Room v4→v5 (`MIGRATION_4_5`, esquema `5.json`), `WakeHistorySource` enlazado a `WakeStatsHistorySource`, sección registrada como `EditorSection` (order 150, tras Retos).
- 3.3: `AdaptiveSessionListener` se registra en el multibinding `Set<SessionFinishedListener>`; al terminar la sesión libera el nivel y evalúa el ajuste. El sink real registra `COMPLETED` de forma asíncrona, así que el motor espera (máx. 5 lecturas cada 200 ms) a que la sesión conste en el historial. Con `alarmId` nulo (pantalla de prueba) solo libera.
