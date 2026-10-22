# Design

## Context

Estado actual: `Alarm` (hora, días, activa, etiqueta, sonido) en Room v1 con `exportSchema = true`; programación en `alarm/`. Todavía no existen retos ni estadísticas. Este change depende de dos changes en paralelo, cuyas interfaces **se asumen** y no se redefinen aquí:

- `challenge-engine` (contrato confirmado; nombres provisionales hasta su apply): dificultad entera ordenada 1..5 (0 = sin definir → 3; fuera de rango se acota). La base está en `Alarm.challenges.difficulty` (`ChallengeConfig` de `alarm-core`) y llega al motor como `SessionConfig.difficulty`. El nivel de cada reto sale de `DifficultyProvider.levelFor(config: SessionConfig, context: ChallengeContext): Int` (`domain/challenge/ChallengeHooks.kt`, rama `feature/challenge-engine` commit 95d3440; `ChallengeContext(sessionId, alarmId, type, index, failures)`; se llama al generar cada reto y tras cada fallo, y el resultado se acota a 1..5; este change ignora `failures` para mantener el nivel fijo en la sesión) (por defecto `FixedDifficultyProvider` = nivel base), y el motor lo pasa a `Challenge.generate(level, params, random)`. Este change **sustituye el binding de Hilt** de `DifficultyProvider` por un `AdaptiveDifficultyProvider`, que delega en el nivel base cuando la alarma no es adaptativa.
- `wake-stats`: eventos por alarma y sesión con `WakeEventType` = `RANG`, `SNOOZED`, `CHALLENGE_FAILED`, `COMPLETED` (cierra la sesión; sin él tras 24 h queda como no completada) `MISSED` (sesión no atendida; se excluye del cálculo) y `SUSPICIOUS` (lo emite `anti-cheat`). Contrato confirmado por `wake-stats`: `WakeHistoryRepository.recentSessions(alarmId, limit, excludeSuspicious): List<WakeSession>` (más reciente primero; con `excludeSuspicious=true` el límite se rellena con sesiones previas no sospechosas) y `WakeSession` con `sessionId`, `snoozeCount`, `failCount`, `completedAt?`, `timeToWake?` (desde que suena hasta `COMPLETED`, incluye snoozes), `suspicious`. Se consumen mediante un puerto propio (`WakeHistorySource`) cuyo adaptador llama a ese repositorio, con `excludeSuspicious=true`; las firmas definitivas se fijan al implementar.

Motivación en proposal.md; requisitos en specs/adaptive-difficulty/spec.md.

## Goals / Non-Goals

**Goals:**
- Núcleo del algoritmo como función pura, sin Android, testeable con tablas de casos.
- Decisiones explicables: cada cambio devuelve su motivo para mostrarlo al usuario.
- Cero acoplamiento de esquema con `alarms`.

**Non-Goals:**
- Elegir o cambiar el tipo de reto (solo el nivel).
- Registrar eventos (es `wake-stats`) ni detectar trampas (es `anti-cheat`).
- Aprendizaje global entre alarmas, ajuste por franja horaria, ML.
- Definir qué es un snooze (es `anti-cheat`) ni crear el `sessionId` (es `AlarmFiringHandler`).

## Decisions

**1. Algoritmo de reglas con ventana, no puntuación continua.** Resumen por sesión (`snoozes`, `failures`, `timeToWake`) sobre las últimas 5 sesiones completadas (mín. 3); reglas de subida/bajada de las specs, paso ±1, bajada prioritaria. `timeToWake` incluye snoozes y el trayecto hasta el móvil, por lo que solo se usa en sesiones sin snoozes y con umbral de reto fácil de 30 s (no solo tiempo del reto; no se pide a `wake-stats` un evento de inicio de reto). Constantes (5, 3, 3 fallos, 120 s, 30 s) agrupadas en un `AdaptiveDifficultyConfig` con valores por defecto, no editables por el usuario. *Alternativas:* media móvil ponderada con umbrales (menos explicable: "¿por qué subió?") y bandit/ML (excesivo, sin datos).

**2. Histéresis por `lastChangeSessionId`.** Tras un cambio solo cuentan sesiones posteriores, evitando oscilación. Reiniciar aprendizaje fija ese marcador a la última sesión. *Alternativa:* cooldown por días; falla con alarmas poco frecuentes.

**3. Estado en tabla propia `adaptive_difficulty_settings`** (`alarmId` PK/FK con `CASCADE`, `enabled`, `minLevel`, `maxLevel`, `currentLevel`, `lastChangeReason`, `lastChangeAt`, `lastChangeSessionId`). Requiere una migración Room v(n+1), encadenada sobre la versión vigente según el orden de merge (`alarm-core` fija v2). *Alternativa:* columnas en `alarms`; descartada porque `alarm-core` y `alarm-storage` modifican esa entidad en paralelo y generaría conflictos de migración.

**4. Evaluar al cerrar la sesión, aplicar al iniciarla.** `AdaptiveDifficultyEngine.onSessionFinished(alarmId)` recalcula y persiste; `levelFor(alarmId)` es una lectura barata al lanzar el reto; como `DifficultyProvider.levelFor` se invoca por reto, el proveedor cachea el nivel por `sessionId` para que no cambie durante la sesión, con fallback a último nivel/dificultad base si `wake-stats` falla. Así el nivel es estable durante la sesión y el disparo nunca depende de cálculos pesados.

**5. Motivo estructurado, texto en la UI.** El resultado lleva un `ChangeReason` (enum + parámetros: cuántas de las últimas 3) y la UI lo traduce a recursos de string. Evita guardar texto localizado en base de datos.

**6. Dirección de las señales.** Snoozes y retos triviales indican que el reto no obliga a despertar → subir. Fallos repetidos o tiempos largos indican bloqueo (riesgo de frustración o de no poder apagar la alarma) → bajar, con prioridad.

**7. UI.** Sección "Dificultad adaptativa" en `AlarmEditorScreen`: interruptor, texto explicativo, selector de rango, nivel actual con último motivo y botón "Reiniciar". Línea informativa en la pantalla del reto cuando cambia de nivel. Componentes según `design-system-stitch` y la skill `design-taste-compose`.

## Risks / Trade-offs

- [Interfaces asumidas de `challenge-engine`/`wake-stats` difieren] → puerto propio `WakeHistorySource` y adaptación a `DifficultyProvider` en una clase fina; los nombres son provisionales, reconciliar al integrar.
- [Definición de sesión/snooze distinta de la asumida] → el motor solo consume `sessionId` y conteos ya agregados por el adaptador.
- [Alarmas sin snooze: la regla de somnolencia nunca se activa] → aceptado; quedan la regla de reto fácil y la de bajada.
- [Subir sin parar hasta el máximo con usuarios que simplemente son rápidos] → límites por alarma y regla de "reto fácil" que exige 3 sesiones limpias consecutivas más la histéresis.
- [Bajada puede reducir la eficacia del despertar] → el mínimo configurable y bajar solo con señal clara de bloqueo.
- [Colisión de versiones de Room con otros changes] → migración aditiva y reordenable; coordinar número de versión al hacer merge.

## Migration Plan

Tabla nueva, sin tocar datos existentes: migración aditiva que solo crea `adaptive_difficulty_settings`. Alarmas existentes sin fila equivalen a "desactivado". Rollback: ignorar la tabla; no hay datos de otras funciones que dependan de ella.

## Decisiones de coordinación (aprobadas)

- Orden de merge: design-system-stitch, alarm-core, alarm-reliability, challenge-engine, alarm-sound, anti-cheat, wake-stats, **adaptive-difficulty (8º)**, challenges-qr-sensors, sharing.
- Room v2 lo fija `alarm-core`. La migración de este change es v(n+1) según el orden de merge (tras wake-stats); el número no se fija en este change.
- El SNOOZE lo introduce `anti-cheat`, no `alarm-core`. Pueden existir alarmas sin snooze: la regla de somnolencia no aplica y siguen vigentes la de reto fácil y la de bajada.
- El `sessionId` lo crea `AlarmFiringHandler`; el adaptador de `WakeHistorySource` lo consume tal cual.
- `anti-cheat` marca las sesiones sospechosas; este change las excluye del cálculo.
- minSdk 29; sin impacto en este change.

## Open Questions

- Nombres definitivos de `DifficultyProvider`, `SessionConfig` y `Alarm.challenges.difficulty`: provisionales hasta el apply de `challenge-engine` y `alarm-core`; solo afectan al adaptador.
- `wake-stats` ya confirmó su contrato. Nota: `difficulty` en los eventos `CHALLENGE_FAILED` es un `Int` opaco; este change no lo interpreta.
