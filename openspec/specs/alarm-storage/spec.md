# alarm-storage Specification

## Purpose
Define cómo la app guarda y recupera de forma persistente las alarmas y los sonidos, de modo que sobrevivan al cierre de la app y al reinicio del dispositivo.

## Requirements

### Requirement: Persistencia de alarmas
El sistema SHALL guardar cada alarma con hora, minutos, días de repetición, estado activo/inactivo, etiqueta opcional y sonido asociado opcional, y SHALL conservarla entre sesiones de la app.

#### Scenario: Crear una alarma
- **WHEN** se guarda una alarma nueva
- **THEN** la alarma recibe un identificador único y aparece al consultar todas las alarmas

#### Scenario: Persistencia tras reinicio de la app
- **WHEN** se cierra y se vuelve a abrir la app
- **THEN** las alarmas guardadas siguen disponibles con los mismos datos

### Requirement: Modificación y borrado de alarmas
El sistema SHALL permitir actualizar y eliminar una alarma existente por su identificador.

#### Scenario: Actualizar una alarma
- **WHEN** se modifica la hora de una alarma guardada
- **THEN** una consulta posterior devuelve la nueva hora

#### Scenario: Borrar una alarma
- **WHEN** se elimina una alarma
- **THEN** deja de aparecer en las consultas

### Requirement: Observación reactiva de alarmas
El sistema SHALL exponer la lista de alarmas como un flujo que emite de nuevo cuando cambia cualquier alarma.

#### Scenario: Cambio notificado
- **WHEN** hay un observador activo y se guarda una alarma
- **THEN** el observador recibe la lista actualizada sin una nueva consulta

### Requirement: Persistencia de sonidos
El sistema SHALL guardar sonidos con nombre y origen (recurso de la app o URI externa) y SHALL permitir asociar un sonido a una alarma.

#### Scenario: Asociar sonido
- **WHEN** se guarda una alarma referenciando un sonido existente
- **THEN** al consultarla se obtiene la referencia a ese sonido

#### Scenario: Sonido eliminado
- **WHEN** se elimina un sonido usado por una alarma
- **THEN** la alarma se conserva sin sonido asociado
