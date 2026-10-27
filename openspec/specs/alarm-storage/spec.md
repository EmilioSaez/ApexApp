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

### Requirement: Persistencia del historial de despertar y sueño
El sistema SHALL guardar en la misma base de datos local los eventos de despertar y los registros de sueño, SHALL migrar una base de datos existente conservando alarmas y sonidos sin cambios, y SHALL NOT borrar historial ni sueño al eliminar una alarma o un sonido.

#### Scenario: Actualización de la app
- **WHEN** se abre la app con una base de datos creada antes de este cambio
- **THEN** las alarmas y sonidos existentes se conservan y el historial empieza vacío

#### Scenario: Borrar alarma con historial
- **WHEN** se elimina una alarma con eventos
- **THEN** la alarma desaparece y sus eventos permanecen

### Requirement: Campos de extensión de la alarma
El sistema SHALL guardar con cada alarma la configuración de retos (lista con parámetros por tipo y dificultad), la configuración de sonido (volumen, rampa, vibración y lectura por voz) y la política anti-trampa, todas con valores por defecto neutros, sin que este almacenamiento defina su comportamiento.

#### Scenario: Alarma con valores por defecto
- **WHEN** se guarda una alarma sin indicar esos campos
- **THEN** al consultarla se obtienen los valores por defecto: sin retos, volumen 100, vibración activada, sin lectura por voz y política anti-trampa permisiva

#### Scenario: Persistencia de la configuración
- **WHEN** se guarda una alarma con configuración de retos, sonido y anti-trampa
- **THEN** una consulta posterior devuelve esa misma configuración

### Requirement: Migración sin pérdida de datos
El sistema SHALL conservar las alarmas y sonidos existentes al actualizar la base de datos a una versión con campos nuevos.

#### Scenario: Actualización desde la versión anterior
- **WHEN** la app se actualiza con alarmas guardadas en el esquema anterior
- **THEN** las alarmas siguen disponibles con los mismos datos y los campos nuevos con sus valores por defecto
