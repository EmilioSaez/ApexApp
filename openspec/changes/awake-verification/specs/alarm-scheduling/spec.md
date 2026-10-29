## ADDED Requirements

### Requirement: Restauración de verificaciones pendientes
Al reprogramar las alarmas tras reinicio, cambio de hora o zona, actualización de la app o cambio del permiso de alarma exacta, el sistema SHALL reprogramar también las verificaciones de despierto pendientes desde su estado guardado.

#### Scenario: Reinicio con verificación pendiente
- **WHEN** el dispositivo arranca con una verificación pendiente con menos de 15 minutos de retraso
- **THEN** se reprograma para su instante, o para ya si ese instante pasó
