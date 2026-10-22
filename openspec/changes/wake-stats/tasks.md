# Tasks

## 1. Modelo y persistencia

- [x] 1.1 Añadir `WakeEventEntity`, `SleepLogEntity`, `SleepPreferencesEntity` y sus DAOs; registrar en `ApexDatabase` con una migración aditiva N→N+1 sobre la versión vigente al rebasar (no fijar el número de versión: lo fija `alarm-core`); verificar que se genera el schema JSON de la versión resultante
- [x] 1.2 Test instrumentado de migración N→N+1 con `MigrationTestHelper`: alarmas y sonidos intactos, tablas nuevas vacías
- [x] 1.3 Test de DAO: eliminar una alarma deja sus eventos con `alarmId` nulo; `INSERT OR IGNORE` ignora «sonó»/«completado» duplicados

## 2. Dominio y contrato

- [x] 2.1 Definir modelos (`WakeEvent`, `WakeEventType`, `WakeSession`), `WakeEventRecorder` y `WakeHistoryRepository`, con implementaciones Room y bindings Hilt; verificar con `RepositoryTest` instrumentado (registro, flujo reactivo, filtro por rango y por `challengeType`, `recentSessions` con límite y exclusión de sospechosas)
- [x] 2.2 Implementar el agrupador de eventos en sesiones con `Clock` inyectado (sesión sin resolver tras 24 h, sesión implícita); verificar con tests unitarios JVM
- [x] 2.3 Implementar `WakeStatsCalculator` (tiempo hasta despertar, snoozes, fallos, puntualidad) y `StreakCalculator` y `observeStreak()`; tests unitarios con los escenarios de `wake-statistics` (racha continúa, día fallido, día sin alarma)

## 3. Disparo de la alarma

- [x] 3.1 En `AlarmFiringHandler`, generar `sessionId` y registrar «sonó» tras notificar, con `runCatching`; verificar con test unitario que un recorder que lanza excepción no impide notificar ni reprogramar

## 4. Sueño

- [x] 4.1 Implementar `SleepRepository` y `SleepCalculator` (manual prevalece sobre estimada, sin datos, sin completado); tests unitarios JVM de los escenarios de `sleep-tracking` y test instrumentado de persistencia

## 5. UI de estadísticas

- [ ] 5.1 Añadir `StatsRoute` en `AppNavigation` y un acceso desde `HomeScreen`; verificar con test de Compose que se abre y que atrás vuelve a la lista
- [x] 5.2 Crear `StatsViewModel` (rango 7/30/90 días, estado vacío) con test unitario del cambio de rango
- [ ] 5.3 Implementar gráficas con Canvas (tiempo hasta despertar, snoozes y fallos, puntualidad y racha) con `semantics` descriptivas y colores del tema, siguiendo `design-taste-compose`; verificar con test de Compose en claro/oscuro y estado vacío
- [ ] 5.4 Implementar gráfica de horas de sueño y diálogo de hora de acostarse y hora habitual, distinguiendo estimadas de manuales; verificar con test de Compose que registrar una hora actualiza la gráfica

## 6. Integración

- [ ] 6.1 Test instrumentado de extremo a extremo con eventos simulados (sonó → snooze → fallo → completado) que verifica métricas, racha y horas de sueño; ejecutar `./gradlew test connectedAndroidTest` (tras `shogun emulator start`) sin fallos
- [x] 6.2 Documentar en `README.md` el contrato `WakeEventRecorder` / `WakeHistoryRepository` para los demás changes y verificar que coincide con el código

## Workflow follow-up

- Archivar el change tras la revisión del proyecto.
