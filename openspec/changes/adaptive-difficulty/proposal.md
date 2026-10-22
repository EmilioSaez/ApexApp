# Proposal

## Why

Con dificultad fija, un reto que al principio despierta acaba siendo mecánico (se resuelve medio dormido) o, al contrario, resulta frustrante en una mala racha. La dificultad debería ajustarse sola según cómo se despierta realmente el usuario, manteniendo el reto en el punto en que obliga a despertar sin bloquearle.

## What Changes

- Nueva capacidad que decide, antes de cada disparo, el nivel de dificultad del reto de una alarma a partir de su historial reciente (snoozes, fallos, tiempo hasta despertar).
- Algoritmo determinista y explicable: ventana de las últimas sesiones, un solo paso de ±1 nivel por sesión, histéresis y límites mín/máx.
- Es **opt-in por alarma** (desactivado por defecto); sin activarlo, la alarma usa la dificultad fija configurada.
- Ajustes por alarma: activar/desactivar, nivel mínimo y máximo, y reinicio del aprendizaje.
- Explicación al usuario: texto en el editor, nivel actual y motivo del último cambio, y una línea breve en la pantalla del reto cuando el nivel cambia.
- Estado propio en una tabla nueva (`adaptive_difficulty_settings`) enlazada por `alarmId`; no se modifica el esquema de `alarms` ni los specs existentes, para no colisionar con `alarm-core` y `alarm-storage`.

### Dependencias (asumidas, no redefinidas)

- `challenge-engine` (otro change): expone una dificultad configurable con niveles ordenados y discretos, y un contrato de reto que acepta el nivel como entrada. Este change solo elige el nivel.
- `wake-stats` (otro change): registra eventos por sesión de alarma (disparo, snooze, intento fallido, reto resuelto, descarte, con marca de tiempo) y permite consultarlos por alarma. Este change solo los lee.
- `anti-cheat`: introduce el snooze (pueden existir alarmas sin snooze) y marca las sesiones sospechosas, que este change excluye del cálculo.
- `alarm-core`: fija Room v2; `AlarmFiringHandler` crea el `sessionId`. `design-system-stitch`: componentes de UI.
- Orden de merge acordado: este change es el 8º, tras `wake-stats`; su migración Room es v(n+1) según ese orden, sin número fijo en este change.

## Capabilities

### New Capabilities
- `adaptive-difficulty`: cálculo del nivel de dificultad de los retos según el historial de despertares, límites, opt-in por alarma y explicación al usuario.

### Modified Capabilities
<!-- Ninguna: el estado adaptativo vive en su propia tabla y no altera requisitos de specs existentes. -->

## Impact

- Código nuevo: dominio (`AdaptiveDifficultyEngine`, ajustes y resultado con motivo), tabla Room + DAO + repositorio, migración de base de datos, sección en `AlarmEditorScreen`, línea explicativa en la pantalla del reto.
- Integración en el punto donde se lanza el reto: pide el nivel al motor antes de crear el reto.
- Sin permisos nuevos ni red; todo local.
- Bloqueado por `challenge-engine` y `wake-stats` para implementar; el algoritmo puro puede desarrollarse y probarse antes con interfaces fake.
