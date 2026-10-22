# Spec Delta

## ADDED Requirements

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
