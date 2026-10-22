# Proposal

## Why

ApexApp es un despertador que obliga a despertar resolviendo retos, pero hoy solo existe la programación y persistencia de alarmas: no hay ningún concepto de reto. Sin un contrato común de reto y un motor que los ejecute, los demás cambios (retos QR/sensores, dificultad adaptativa, estadísticas, anti-trampas) no tienen sobre qué construirse.

## What Changes

- Nuevo contrato `Challenge` (dominio puro) que describe un reto, su tipo, su nivel de dificultad y cómo se genera y valida una instancia.
- Nuevo motor de ejecución de retos: una sesión que encadena N retos en orden o elige aleatoriamente entre los tipos activados, y termina solo cuando se superan todos.
- Configuración de sesión derivada de la alarma: dificultad 1..5, número de retos (1–10), modo (encadenado/aleatorio), tipos activados y parámetros opacos por tipo. Se lee de la `ChallengeConfig` que persiste `alarm-core` (`challengesJson`); este change **no** añade columnas ni migración, solo las propiedades con default `count` y `mode` dentro de ese JSON.
- Primer reto concreto: matemáticas/puzzle (operaciones cuya complejidad depende del nivel 1..5).
- Mecanismo de reserva: un reto no disponible o no completable (falta cámara, sensor o permiso) se sustituye por otro tipo habilitado o, si no hay, por matemáticas.
- Sección «Retos» en el editor de alarmas (tipos desde el registro, nº de retos, dificultad, modo) y `ChallengeParamsEditor` por tipo con validación previa al guardado. Para componerla se introduce un agregador `EditorSection` (multibinding Hilt, con orden) que el editor renderiza en su hueco `extraContent`; otros changes (sonido, anti-trampa, dificultad adaptativa, QR/sensores) añaden sus secciones ahí.
- Pantalla de prueba para ejecutar una sesión de retos con una configuración dada, sin alarma real ni disparo.
- Ganchos de extensión, sin implementar su lógica:
  - registro de retos (`ChallengeRegistry`) y parámetros opacos por tipo (`ChallengeSpec.params`, JSON) para que `challenges-qr-sensors` añada tipos;
  - proveedor de dificultad (`DifficultyProvider`) con implementación fija por defecto, sustituible por `adaptive-difficulty`;
  - emisor de eventos de sesión/reto (`ChallengeEventSink`) con implementación vacía por defecto, para `wake-stats`. El `sessionId` lo crea `AlarmFiringHandler` y se recibe como entrada.

## Capabilities

### New Capabilities
- `challenge-engine`: contrato de reto, registro de tipos, sesión de ejecución (encadenada o aleatoria), proveedor de dificultad, emisión de eventos y pantalla de prueba.
- `math-challenge`: reto de matemáticas/puzzle con generación y validación según nivel de dificultad.
- `challenge-editor`: sección de retos del editor de alarmas, editores de parámetros por tipo con validación y agregador de secciones del editor.

### Modified Capabilities
Ninguna. La persistencia de la configuración de retos es de `alarm-core` (Room v2 y `ChallengeConfig`).

## Impact

- Código: nuevo paquete `domain/challenge` (contrato, sesión, registro), `ui/challenge` (pantalla de ejecución y de prueba), `ui/editor` (agregador `EditorSection`, borrador de alarma y validación en `AlarmEditorViewModel`/`AlarmEditorScreen`), `AppNavigation` (ruta de prueba), módulo Hilt para el registro.
- Dependencias entre changes (no se invade su alcance):
  - `alarm-core` (se fusiona antes; su editor solo hacía modelo y persistencia, la sección de retos se asigna a este change): fija Room v2 y `ChallengeConfig` (ids `String` + dificultad). Define `ChallengeConfig(difficulty, challenges: List<ChallengeSpec(type, params)>)`; este change le añade `count` y `mode` con default, y aporta una implementación de `DismissGate` para enganchar los retos.
  - `alarm-reliability` (se fusiona antes; único dueño del servicio en primer plano) y `AlarmFiringHandler`: lanzan la sesión real y crean el `sessionId`; aquí solo se ofrece la UI de sesión reutilizable.
  - `anti-cheat`: restricciones durante el reto y snooze; fuera de alcance.
  - `challenges-qr-sensors`: registra nuevos tipos (QR con ML Kit, sensores), guarda su configuración como carga opaca y aporta su `ChallengeParamsEditor` (registro de código QR, objetivo de movimiento) con la validación «no guardar alarma QR sin código».
  - `adaptive-difficulty`: sustituye el `DifficultyProvider`; asume niveles 1..5.
  - `wake-stats`: consume `ChallengeEventSink`.
  - `design-system-stitch` (se fusiona primero): puede modificar `HomeScreen.kt` y `AppNavigation.kt`, donde este change añade la acción de la barra y la ruta de prueba; se rebasa sobre su versión y se usa su tema.
  - `alarm-sound`, `anti-cheat`, `adaptive-difficulty`: migran o aportan sus secciones de editor como `EditorSection`.
  - `sharing`: sin dependencia directa.
- Sin permisos nuevos ni red; todo local.
