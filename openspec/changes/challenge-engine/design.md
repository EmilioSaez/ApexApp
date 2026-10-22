# Design

## Context

Estado actual (ver `proposal.md` para la motivación): `Alarm` es un `data class` de dominio con `AlarmEntity` en Room (`ApexDatabase` v1 en `develop`; `alarm-core` fija la v2 y añade `Alarm.challenges: ChallengeConfig` = ids `String` + `difficulty: Int`, persistido en `challengesJson`), DI con Hilt (`di/DataModule.kt`, `di/AlarmModule.kt`) y navegación Compose con rutas `@Serializable` en `ui/navigation/AppNavigation.kt`. El editor de alarmas es un placeholder (lo completará `alarm-core`). Hay otros nueve changes en paralelo que tocarán `Alarm`, la base de datos o la navegación, por lo que este cambio debe mantener su huella pequeña y aditiva.

## Goals / Non-Goals

**Goals:**
- Dominio de retos en Kotlin puro, testeable con tests JVM sin Android, sin tocar el esquema de Room.
- Puntos de extensión estables (`ChallengeRegistry`, `DifficultyProvider`, `ChallengeEventSink`) para tres changes posteriores.
- Una UI de sesión reutilizable que `alarm-reliability` pueda alojar en la pantalla de alarma sonando.

**Non-Goals:**
- Editor de configuración de retos dentro de la alarma, y cualquier columna o migración de Room (`alarm-core`).
- Lanzar la sesión al sonar la alarma, bloqueo de pantalla o de salida (`alarm-reliability`, `anti-cheat`).
- Retos QR/sensores, dificultad adaptativa, persistencia de estadísticas, sonido.
- Estilo visual final (`design-system-stitch`); se usa el tema M3 actual.

## Decisions

**1. Contrato `Challenge` como fábrica de instancias.**
`Challenge` expone `type: ChallengeType` (id `String` estable, el mismo que persiste `alarm-core`), `generate(level: Int, params: String, random: Random): ChallengeInstance` y `availability(): Availability` (`Available` / `Unavailable(reason)`). `ChallengeInstance` lleva un `prompt` y `check(answer: ChallengeAnswer): Boolean`. Separar fábrica e instancia hace la generación determinista con un `Random` inyectado y evita estado mutable en el reto.
Alternativa: un objeto con estado `start()/submit()`. Descartada: más difícil de probar y de reutilizar con QR/sensores.

**2. Respuesta tipada y parámetros opacos por tipo.**
`ChallengeAnswer` es sellado (`Text` ahora; QR/sensores añaden variantes). Los parámetros por tipo son `ChallengeSpec.params: String` (JSON opaco; `""` o `"{}"` si no hay) de la `ChallengeConfig` de alarm-core; el motor los transporta sin interpretar y entrega a cada reto solo los suyos. La UI de cada tipo se registra junto al reto en `ChallengeRegistry`.

**3. `ChallengeSession` como máquina de estados pura.**
Entradas: `SessionConfig`, `sessionId: String` (lo crea `AlarmFiringHandler`; la pantalla de prueba genera el suyo), `alarmId: Long?`, registro, `DifficultyProvider`, `ChallengeEventSink`, `Random` y reloj. Expone `current instance`, `solved/total` y `Passed`; operación `submit(answer)`. Un fallo regenera el mismo tipo. Un ViewModel la envuelve en `StateFlow`.

**4. Configuración: vive en `ChallengeConfig` de alarm-core, sin esquema propio.**
`alarm-core` (merge anterior) fija Room v2 y `Alarm.challenges: ChallengeConfig(difficulty: Int = 0, challenges: List<ChallengeSpec>)` con `ChallengeSpec(type: String, params: String)`, persistido en `challengesJson TEXT NOT NULL DEFAULT '{}'` con `ignoreUnknownKeys`. Este change **no** añade columnas ni migración ni sube la versión. Alarm-core permite añadir propiedades con default a `ChallengeConfig`: se añaden `count: Int = 1` (1..10) y `mode: String = "CHAINED"` (`CHAINED`/`RANDOM`), serializados dentro del mismo JSON. El orden de `challenges` es el orden de la cadena. `SessionConfig.from(ChallengeConfig)` los lee; `difficulty = 0` significa "sin definir" y se interpreta como 3. Es el único punto de acoplamiento con `ChallengeConfig`.
Alternativa: columnas propias y migración 1→2. Descartada por decisión del usuario (Room v2 es de alarm-core).

**5. Dificultad 1..5.**
Entero ordenado en 1..5; el de la alarma es el nivel base. `adaptive-difficulty` lo asume igual y recibe el nivel al crear cada reto. Un valor fuera de 1..5 se acota; 0 (sin definir en alarm-core) equivale a 3.

**6. Ganchos como interfaces con implementación por defecto.**
- `DifficultyProvider.levelFor(config, sessionContext)` → por defecto `FixedDifficultyProvider` (nivel base).
- `ChallengeEventSink.onEvent(ChallengeEvent)` → por defecto `NoOpChallengeEventSink`.
Se inyectan con Hilt (`@Binds`); `adaptive-difficulty` y `wake-stats` solo sustituyen el binding. `ChallengeEvent` es sellado: `SessionStarted`, `ChallengeStarted`, `ChallengeFailed`, `ChallengeSolved`, `ChallengeFellBack`, `SessionFinished`, todos con `sessionId`, `alarmId?`, tipo, nivel y marca de tiempo.

**7. Mecanismo de reserva.**
Antes de cada reto se consulta `availability()`; si es `Unavailable` (falta cámara, permiso o sensor), o el reto avisa durante la ejecución de que no puede completarse (p. ej. permiso revocado), la sesión lo sustituye por otro tipo habilitado y disponible de la configuración y, si no hay ninguno, por `MATH` al mismo nivel, emitiendo `ChallengeFellBack`. La alarma nunca queda sin forma de resolverse. Si la configuración no deja tipos utilizables, se usa `MATH`.

**8. Math: plantillas por nivel 1..5 con resultado entero.**
1: `a ± b` (1–20). 2: `a ± b` mayores. 3: `a × b` (2–12). 4: `a × b ± c`. 5: expresiones de tres términos con paréntesis. Se construyen desde el resultado para garantizar enteros y sin divisiones inexactas.

**9. Punto de enganche en el flujo real.**
`alarm-core` ofrece `DismissGate.canDismiss(alarm)` (por defecto `true`) en la pantalla de sonar. Este change aporta una implementación que solo permite descartar cuando la sesión de retos de la alarma está superada, pero su activación en el flujo real se coordina con `alarm-reliability` y `anti-cheat`; en este change queda probada con la pantalla de prueba. El `sessionId` lo crea `AlarmFiringHandler` y llega por `RingViewModel`.

**10. Agregador de secciones del editor.**
`EditorSection` (en `ui/editor`) = `order: Int` + `@Composable Content(draft: Alarm, onChange: (Alarm) -> Unit)` + `isValid(draft): Boolean = true`. Se bindea con `@Binds @IntoSet` y `AlarmEditorViewModel` recibe `Set<EditorSection>` ordenado por `order` y, a igual orden, por nombre de clase (estable). El estado del editor incorpora un borrador `Alarm` con todos los campos de extensión (retos, sonido, anti-trampa): las secciones lo transforman con `onChange` y `save` parte de él, así que los campos que ninguna sección toca se conservan. `AlarmEditorContent` renderiza las secciones en `extraContent` y desactiva guardar si alguna `isValid` es falsa.
Alternativa: un callback por sección en la firma del editor. Descartada: obliga a tocar el editor con cada change.

**11. Sección «Retos» y editores de parámetros.**
`ChallengesSection` (`order = 100`) lee el `ChallengeRegistry` para ofrecer los tipos, edita `Alarm.challenges` (el orden de `challenges` es el de la cadena; al habilitar un tipo se añade al final) y delega los parámetros en `ChallengeParamsEditor` (`type`, `@Composable Content(params: String, onChange: (String) -> Unit)`, `isValid(params): Boolean = true`), multibound por tipo. La sección es inválida si algún tipo habilitado tiene un editor que rechaza sus parámetros. La etiqueta visible de cada tipo la da `ChallengeRenderer.label()` (por defecto, el id).

**12. Pantalla de prueba.**
Ruta `ChallengeTestRoute` con formulario (dificultad 1..5, nº de retos, modo, tipos del registro) que lanza `ChallengeSessionScreen`. Acceso desde una acción en la barra de `HomeScreen` (decidido). No usa `AlarmRepository` ni `AlarmScheduler`.

## Risks / Trade-offs

- [`count` y `mode` son propiedades añadidas por este change a la `ChallengeConfig` de alarm-core] → con default y dentro del mismo JSON (sin migración); si alarm-core cambia la clase, ajustar solo `SessionConfig.from`.
- [Contrato de respuesta insuficiente para QR/sensores] → `ChallengeAnswer` sellado, `ChallengeSpec.params` opaco y reserva; validar con `challenges-qr-sensors`.
- [Nombres provisionales de alarm-core pueden cambiar] → solo afecta a `SessionConfig.from`.
- [Reto de matemáticas esquivable con calculadora] → aceptado; lo abordan `adaptive-difficulty` y `anti-cheat`.

## Migration Plan

Sin migración de datos propia. Rebasar sobre `develop` tras la fusión de `alarm-core` y `alarm-reliability` (orden de merge: este change es el 4.º). Rollback: revertir el change; no hay cambios de esquema.

## Open Questions

Ninguna. El acceso a la pantalla de prueba queda decidido: acción en la barra de `HomeScreen`. `design-system-stitch` se fusiona antes y puede rehacer `HomeScreen.kt` y `AppNavigation.kt`; al aplicar, rebasar y añadir la acción y la ruta sobre su versión, sin reescribir lo suyo.
