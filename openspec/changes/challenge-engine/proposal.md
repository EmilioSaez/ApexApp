# Proposal

## Why

ApexApp es un despertador que obliga a despertar resolviendo retos, pero hoy solo existe la programación y persistencia de alarmas: no hay ningún concepto de reto. Sin un contrato común de reto y un motor que los ejecute, los demás cambios (retos QR/sensores, dificultad adaptativa, estadísticas, anti-trampas) no tienen sobre qué construirse.

## What Changes

- Nuevo contrato `Challenge` (dominio puro) que describe un reto, su tipo, su nivel de dificultad y cómo se genera y valida una instancia.
- Nuevo motor de ejecución de retos: una sesión que encadena N retos en orden o elige aleatoriamente entre los tipos activados, y termina solo cuando se superan todos.
- Configuración de sesión derivada de la alarma: dificultad 1..5, número de retos (1–10), modo (encadenado/aleatorio), tipos activados y parámetros opacos por tipo. Se lee de la `ChallengeConfig` que persiste `alarm-core` (`challengesJson`); este change **no** añade columnas ni migración, solo las propiedades con default `count` y `mode` dentro de ese JSON.
- Primer reto concreto: matemáticas/puzzle (operaciones cuya complejidad depende del nivel 1..5).
- Mecanismo de reserva: un reto no disponible o no completable (falta cámara, sensor o permiso) se sustituye por otro tipo habilitado o, si no hay, por matemáticas.
- Pantalla de prueba para ejecutar una sesión de retos con una configuración dada, sin alarma real ni disparo.
- Ganchos de extensión, sin implementar su lógica:
  - registro de retos (`ChallengeRegistry`) y parámetros opacos por tipo (`ChallengeSpec.params`, JSON) para que `challenges-qr-sensors` añada tipos;
  - proveedor de dificultad (`DifficultyProvider`) con implementación fija por defecto, sustituible por `adaptive-difficulty`;
  - emisor de eventos de sesión/reto (`ChallengeEventSink`) con implementación vacía por defecto, para `wake-stats`. El `sessionId` lo crea `AlarmFiringHandler` y se recibe como entrada.

## Capabilities

### New Capabilities
- `challenge-engine`: contrato de reto, registro de tipos, sesión de ejecución (encadenada o aleatoria), proveedor de dificultad, emisión de eventos y pantalla de prueba.
- `math-challenge`: reto de matemáticas/puzzle con generación y validación según nivel de dificultad.

### Modified Capabilities
Ninguna. La persistencia de la configuración de retos es de `alarm-core` (Room v2 y `ChallengeConfig`).

## Impact

- Código: nuevo paquete `domain/challenge` (contrato, sesión, registro), `ui/challenge` (pantalla de ejecución y de prueba), `AppNavigation` (ruta de prueba), módulo Hilt para el registro.
- Dependencias entre changes (no se invade su alcance):
  - `alarm-core` (se fusiona antes): fija Room v2 y `ChallengeConfig` (ids `String` + dificultad). Define `ChallengeConfig(difficulty, challenges: List<ChallengeSpec(type, params)>)`; este change le añade `count` y `mode` con default, y aporta una implementación de `DismissGate` para enganchar los retos.
  - `alarm-reliability` (se fusiona antes; único dueño del servicio en primer plano) y `AlarmFiringHandler`: lanzan la sesión real y crean el `sessionId`; aquí solo se ofrece la UI de sesión reutilizable.
  - `anti-cheat`: restricciones durante el reto y snooze; fuera de alcance.
  - `challenges-qr-sensors`: registra nuevos tipos (QR con ML Kit, sensores) y guarda su configuración como carga opaca.
  - `adaptive-difficulty`: sustituye el `DifficultyProvider`; asume niveles 1..5.
  - `wake-stats`: consume `ChallengeEventSink`.
  - `design-system-stitch` (se fusiona primero): puede modificar `HomeScreen.kt` y `AppNavigation.kt`, donde este change añade la acción de la barra y la ruta de prueba; se rebasa sobre su versión y se usa su tema.
  - `alarm-sound`, `sharing`: sin dependencia directa.
- Sin permisos nuevos ni red; todo local.
