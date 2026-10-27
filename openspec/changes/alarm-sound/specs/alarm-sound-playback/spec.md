## ADDED Requirements

### Requirement: Reproducción por el stream de alarma
El sistema SHALL reproducir el sonido de una alarma que suena usando atributos de audio de alarma (`USAGE_ALARM`) para que respete el volumen de alarma, y SHALL repetirlo en bucle hasta que la alarma se detenga o se posponga.

#### Scenario: Alarma suena
- **WHEN** suena una alarma con sonido configurado
- **THEN** el audio se reproduce por el stream de alarma en bucle

#### Scenario: Detener la alarma
- **WHEN** la alarma se detiene o se pospone
- **THEN** el audio se detiene y se liberan los recursos

### Requirement: Audio focus
El sistema SHALL solicitar audio focus para alarmas al empezar a sonar y SHALL liberarlo al terminar.

#### Scenario: Otra app reproduce audio
- **WHEN** suena una alarma mientras otra app reproduce audio
- **THEN** la otra app pierde el focus y la alarma se oye

#### Scenario: Focus perdido durante la alarma
- **WHEN** otra app roba el focus mientras la alarma suena
- **THEN** la alarma sigue sonando sin pausarse

### Requirement: Fuentes de sonido admitidas
El sistema SHALL poder reproducir tonos del sistema, archivos locales referenciados por URI y el tono por defecto incluido en la app.

#### Scenario: Tono del sistema
- **WHEN** la alarma usa un tono del sistema
- **THEN** se reproduce ese tono

#### Scenario: Archivo local
- **WHEN** la alarma usa un archivo local elegido por el usuario
- **THEN** se reproduce ese archivo

#### Scenario: Sin sonido asociado
- **WHEN** la alarma no tiene sonido asociado
- **THEN** se reproduce el tono por defecto

### Requirement: Respaldo ante fallo de reproducción
El sistema SHALL reproducir el tono por defecto de la app si la fuente configurada no puede abrirse (archivo borrado, permiso revocado, error del reproductor), de modo que una alarma nunca suene en silencio.

#### Scenario: Archivo no disponible
- **WHEN** el archivo configurado ya no existe o no es legible
- **THEN** suena el tono por defecto

#### Scenario: Error durante la reproducción
- **WHEN** el reproductor falla a mitad de reproducción
- **THEN** se reinicia con el tono por defecto

### Requirement: Volumen progresivo
El sistema SHALL, si la alarma tiene rampa configurada, subir el volumen de forma gradual desde un nivel bajo hasta el volumen de alarma del dispositivo durante la duración indicada, y SHALL mantener ese volumen después.

#### Scenario: Rampa activa
- **WHEN** suena una alarma con rampa de 30 segundos
- **THEN** el volumen crece de forma monótona hasta el máximo en 30 segundos

#### Scenario: Sin rampa
- **WHEN** suena una alarma sin rampa
- **THEN** suena desde el inicio al volumen de alarma del dispositivo

#### Scenario: Reinicio del bucle
- **WHEN** el sonido vuelve a empezar su bucle durante o tras la rampa
- **THEN** el volumen no vuelve al nivel inicial
