## ADDED Requirements

### Requirement: Campos de silencio durante los retos
El sistema SHALL persistir en columnas propias de `alarms` si la alarma silencia durante los retos (`muteDuringChallenges`, por defecto desactivado) y su inactividad en segundos (`muteInactivitySeconds`, por defecto 30), y SHALL migrar de la versión 5 a la 6 sin pérdida de datos.

#### Scenario: Migración v5 → v6
- **WHEN** se abre una base de datos v5 con alarmas guardadas
- **THEN** conservan todos sus datos, con la opción desactivada y 30 s de inactividad

#### Scenario: Ida y vuelta
- **WHEN** se guarda una alarma con la opción activa y 60 s y se lee de nuevo
- **THEN** se recuperan los mismos valores
