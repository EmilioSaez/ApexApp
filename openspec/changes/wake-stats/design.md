# Design

## Context

Estado actual: Room v1 (`alarms`, `sounds`) con `exportSchema = true`, Hilt (`DatabaseModule`/`RepositoryModule`), repositorios de dominio como interfaces con implementación Room, `AlarmFiringHandler` que notifica y reprograma/desactiva, navegación con rutas `@Serializable` en `AppNavigation`. No hay DataStore ni librería de gráficas. Ver proposal.md para la motivación.

Otros changes en paralelo escriben o leen datos de despertar: `alarm-core` y `challenge-engine` (emiten snooze/fallo/completado), `anti-cheat` (puede emitir eventos), `adaptive-difficulty` (lee historial por tipo de reto), `sharing` (lee racha). Este change fija el contrato; no implementa su lógica.

## Goals / Non-Goals

**Goals:**
- Modelo de datos estable que otros changes consuman sin migraciones propias del historial.
- Métricas como funciones puras, testeables en JVM sin Room ni Android.
- Cambio mínimo en código existente.

**Non-Goals:**
- Exportar o compartir estadísticas; UI de alarma sonando o de retos; estimar el sueño con sensores o uso del móvil.

## Decisions

1. **Eventos append-only como única fuente de verdad; sesiones derivadas.** Tabla `wake_events(id, sessionId TEXT, alarmId INTEGER NULL, type, timestamp, scheduledAt NULL, challengeType TEXT NULL, difficulty INTEGER NULL)`. `sessionId` es un UUID generado al sonar. FK a `alarms` con `SET_NULL`, índices en `sessionId` y `timestamp`. `WakeSession` (programada, inicio, snoozes, fallos, completado, resultado) se calcula en dominio.
   *Alternativa:* tabla `wake_sessions` materializada además de eventos → dos escrituras y riesgo de incoherencia; el volumen (unas pocas filas al día) no lo justifica.

2. **`sessionId` lo crea quien dispara.** `AlarmFiringHandler` genera el UUID, registra «sonó» y lo pasa a la notificación/pantalla de alarma (el paso exacto lo define `alarm-core`). Los demás emisores (p. ej. `anti-cheat` emite el snooze) reciben el `sessionId` y llaman a `WakeEventRecorder.record(...)`.
   *Alternativa:* inferir sesión por `alarmId` + ventana de tiempo → frágil con snoozes y alarmas solapadas.

3. **Contrato para otros changes** (paquete `domain`): `WakeEventRecorder` (suspend, escritura), `WakeHistoryRepository` (`observeSessions(range, challengeType?)`, `sessions(...)`, `recentSessions(alarmId, limit, excludeSuspicious)`), `StreakCalculator` (puro) y `observeStreak()`. `WakeEventType` = `RANG`, `SNOOZED`, `CHALLENGE_FAILED`, `COMPLETED`, `MISSED` (alarma perdida, la emite `alarm-reliability`), `SUSPICIOUS` (este último lo emite `anti-cheat`; `WakeSession.suspicious` = existe alguno). `challengeType` es `String` (id estable del reto) para no depender del enum de `challenge-engine`. `difficulty` es `Int` opaco.

4. **Sesión sin resolver = no completada pasadas 24 h**, calculado contra un `Clock` inyectado, sin worker ni escritura de eventos «perdido».

5. **Puntualidad y racha como constantes de dominio**: puntual = completado ≤ 10 min tras `scheduledAt`. Día de racha = todas sus sesiones puntuales; días sin sesiones neutros. Día = fecha local de `scheduledAt`.

6. **Sueño en tablas propias**: `sleep_logs(date PK, bedtimeAt, source)` (manual) y `sleep_preferences(id=1, usualBedtimeMinutes NULL)`. Horas dormidas = primer «completado» del día − bedtime (manual, o estimada con la hora habitual del día anterior). Se usa Room y no DataStore para no añadir dependencia.

7. **Gráficas con Canvas de Compose.** Tres o cuatro gráficas simples (barras/líneas) con colores del tema M3 y `semantics` con descripción. Sin librería externa. Las gráficas siguen `design-taste-compose` y se adaptarán a `design-system-stitch` cuando exista.

8. **Registro de «sonó» a prueba de fallos**: en `AlarmFiringHandler`, el registro se envuelve en `runCatching` y va tras la notificación, para que un fallo de Room nunca retrase o impida la alarma.

9. **Idempotencia** vía índice único parcial lógico en `(sessionId, type)` para «sonó» y «completado» (`INSERT OR IGNORE`); los snoozes y fallos se permiten repetidos.

## Risks / Trade-offs

- [Otros changes tocan `ApexDatabase` (versión/migraciones)] → Este change no fija número de versión: `alarm-core` fija v2 y el orden de merge acordado sitúa a `wake-stats` después; la migración es aditiva y se escribe sobre la versión vigente (N → N+1) al rebasar.
- [Eventos nunca emitidos por los changes propietarios, métricas incompletas] → Specs y tasks documentan el contrato; tests de integración con eventos simulados.
- [Estimación de sueño imprecisa] → Se marca explícitamente como «estimada» y se distingue visualmente.
- [Racha por día ambigua con varias alarmas] → Regla estricta y documentada; las constantes son fáciles de cambiar.

## Migration Plan

Migración `N → N+1` (N = versión vigente en `develop` al hacer merge) solo con `CREATE TABLE` e índices; sin tocar tablas existentes. Se genera el schema JSON de la versión resultante. Test de migración con `MigrationTestHelper`. Rollback: no aplica (la app es local); no se borra ninguna tabla existente.

## Open Questions

- ¿Umbral de puntualidad de 10 min configurable por el usuario? (por ahora constante.)
- ¿Estimar la hora de acostarse con el último apagado de pantalla en el futuro? (fuera de este change.)
- ¿Debe `anti-cheat` registrar un tipo de evento propio? Hoy no; se añadiría un tipo sin cambiar el modelo.
