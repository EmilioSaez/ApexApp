# Design

## Context

`SnoozePolicy(allowed, minutes, maxCount)` se guarda dentro de `AntiCheatPolicy` en `antiCheatJson`, y el contador de posposiciones es por `sessionId` (`SnoozeCounts`), que se limpia al terminar la sesión; cada disparo programado crea una sesión nueva, así que el contador se reinicia en cada disparo. El único camino para posponer es `SnoozeController.snooze`, llamado desde `RingViewModel`; no hay acciones de notificación que pospongan.

## Decisions

1. **Reutilizar `maxCount` con centinela `-1` en lugar de columna nueva.** Las extensiones de `Alarm` viven como JSON sin migrar esquema (ver `AlarmConfig.kt`) y `maxCount` ya existe. Una columna duplicaría el dato y obligaría a renumerar la versión de Room al fusionar con otros changes. El defecto 3 conserva el comportamiento actual.
2. **`SnoozePolicy.remaining(done): Int?`** (`null` = sin límite) y `canSnooze(done)` concentran la regla; `SnoozeController` solo las consume y expone `remaining(session)`.
3. **`RingUiState.snoozesLeft`** alimenta la UI; `canSnooze` sigue siendo la fuente de verdad para mostrar el botón.
4. **Validación de alarmas compartidas:** `maxCount` válido es `-1` o `0..10`.

## Risks

- Versiones antiguas de la app que lean `-1` lo tratan como «0 posposiciones» (más restrictivo, no rompe).
