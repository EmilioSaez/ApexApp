# sleep-tracking Specification

## Purpose
Estimar y registrar las horas de sueño por noche a partir de la hora de acostarse (manual o habitual) y del primer despertar completado, distinguiendo las estimadas de las manuales.

## Requirements

### Requirement: Hora de acostarse manual
El sistema SHALL permitir registrar y modificar manualmente la hora a la que el usuario se acostó una noche concreta, y SHALL marcar ese registro como manual.

#### Scenario: Registrar hora
- **WHEN** el usuario introduce que se acostó a las 23:30
- **THEN** la noche queda registrada con 23:30 como hora de acostarse manual

#### Scenario: Corregir hora
- **WHEN** el usuario modifica una hora de acostarse ya registrada
- **THEN** la noche usa el valor nuevo

### Requirement: Hora de acostarse estimada
El sistema SHALL permitir configurar una hora habitual de acostarse y SHALL usarla como hora estimada en las noches sin registro manual, marcándola como estimada. Sin hora habitual configurada ni registro manual, la noche SHALL quedar sin horas de sueño.

#### Scenario: Noche sin registro
- **WHEN** hay hora habitual 23:00 y una noche sin registro manual
- **THEN** esa noche usa 23:00 marcada como estimada

#### Scenario: Registro manual prevalece
- **WHEN** hay hora habitual 23:00 y registro manual 00:15
- **THEN** la noche usa 00:15 marcada como manual

#### Scenario: Sin datos de acostarse
- **WHEN** no hay hora habitual ni registro manual
- **THEN** la noche no cuenta en las gráficas de sueño

### Requirement: Horas de sueño
El sistema SHALL calcular las horas dormidas de cada noche como el tiempo entre la hora de acostarse y el instante en que el usuario completó la primera sesión de despertar del día, y SHALL mostrar su evolución y la media del rango, distinguiendo visualmente las noches estimadas de las manuales.

#### Scenario: Cálculo
- **WHEN** el usuario se acostó a las 23:30 y completó el despertar a las 07:00
- **THEN** la noche muestra 7 h 30 min

#### Scenario: Noche sin despertar completado
- **WHEN** la sesión de la mañana no se completó
- **THEN** la noche no tiene horas dormidas y no entra en la media

### Requirement: Persistencia local del sueño
El sistema SHALL conservar los registros de sueño y la hora habitual entre sesiones de la app y SHALL guardarlos solo en el dispositivo.

#### Scenario: Reapertura
- **WHEN** se cierra y se vuelve a abrir la app
- **THEN** los registros y la hora habitual siguen disponibles
