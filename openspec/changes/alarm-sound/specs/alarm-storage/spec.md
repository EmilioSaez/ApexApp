## MODIFIED Requirements

### Requirement: Persistencia de sonidos
El sistema SHALL guardar sonidos con nombre y origen (recurso de la app, tono del sistema o URI externa) y SHALL permitir asociar un sonido a una alarma.

#### Scenario: Asociar sonido
- **WHEN** se guarda una alarma referenciando un sonido existente
- **THEN** al consultarla se obtiene la referencia a ese sonido

#### Scenario: Sonido eliminado
- **WHEN** se elimina un sonido usado por una alarma
- **THEN** la alarma se conserva sin sonido asociado

#### Scenario: Tono del sistema
- **WHEN** se guarda un sonido con origen de tono del sistema
- **THEN** se recupera con ese mismo tipo de origen y valor

#### Scenario: Sonido incluido en la app
- **WHEN** se guarda un sonido incluido en la app
- **THEN** se identifica por una clave estable independiente de los ids de recurso

#### Scenario: Clave desconocida
- **WHEN** se resuelve una clave de sonido que no existe
- **THEN** se obtiene el sonido por defecto

## ADDED Requirements

### Requirement: Persistencia de ajustes de sonido ampliados
El sistema SHALL guardar, dentro de la configuración de sonido de cada alarma (`SoundConfig`), la duración de la rampa de volumen, el patrón de vibración, si se lee la hora y el intervalo de repetición de la voz, además de los ajustes de volumen, vibración y texto de voz que ya define la configuración de sonido, y SHALL conservarlos entre sesiones.

#### Scenario: Ajustes conservados
- **WHEN** se guarda una alarma con rampa, patrón de vibración y voz configurados
- **THEN** una consulta posterior devuelve esos mismos ajustes

#### Scenario: Valores por defecto
- **WHEN** se guarda una alarma sin indicar estos ajustes
- **THEN** se aplican los valores por defecto de rampa, vibración y voz

### Requirement: Tolerancia a ajustes ausentes
El sistema SHALL aplicar los valores por defecto a los ajustes de sonido ausentes o desconocidos en datos ya guardados, sin perder la alarma.

#### Scenario: Alarma guardada sin ajustes ampliados
- **WHEN** se lee una alarma guardada antes de existir los ajustes ampliados
- **THEN** la alarma se devuelve con los valores por defecto de esos ajustes
