## ADDED Requirements

### Requirement: Persistencia del historial de despertar y sueño
El sistema SHALL guardar en la misma base de datos local los eventos de despertar y los registros de sueño, SHALL migrar una base de datos existente conservando alarmas y sonidos sin cambios, y SHALL NOT borrar historial ni sueño al eliminar una alarma o un sonido.

#### Scenario: Actualización de la app
- **WHEN** se abre la app con una base de datos creada antes de este cambio
- **THEN** las alarmas y sonidos existentes se conservan y el historial empieza vacío

#### Scenario: Borrar alarma con historial
- **WHEN** se elimina una alarma con eventos
- **THEN** la alarma desaparece y sus eventos permanecen
