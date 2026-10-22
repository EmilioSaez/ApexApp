## ADDED Requirements

### Requirement: Vibración al sonar
El sistema SHALL vibrar con el patrón configurado mientras la alarma suena, repitiéndolo hasta que se detenga o se posponga, usando atributos de vibración de alarma.

#### Scenario: Vibración activa
- **WHEN** suena una alarma con vibración activada
- **THEN** el dispositivo vibra con su patrón en bucle

#### Scenario: Vibración desactivada
- **WHEN** suena una alarma con vibración desactivada
- **THEN** el dispositivo no vibra

#### Scenario: Detener la alarma
- **WHEN** la alarma se detiene o se pospone
- **THEN** la vibración se cancela

### Requirement: Patrones predefinidos y personalizados
El sistema SHALL ofrecer patrones de vibración predefinidos y SHALL permitir definir un patrón personalizado como secuencia de pulsos y pausas, validado y limitado a una longitud y duración máximas.

#### Scenario: Patrón predefinido
- **WHEN** el usuario elige un patrón predefinido
- **THEN** la alarma lo usa al sonar

#### Scenario: Patrón personalizado válido
- **WHEN** el usuario define pulsos y pausas dentro de los límites
- **THEN** el patrón se guarda y se usa al sonar

#### Scenario: Patrón personalizado inválido
- **WHEN** el patrón está vacío o supera los límites
- **THEN** no se guarda y se informa al usuario

### Requirement: Dispositivo sin vibrador
El sistema SHALL ignorar la vibración sin error si el dispositivo no tiene vibrador.

#### Scenario: Sin hardware
- **WHEN** suena una alarma con vibración en un dispositivo sin vibrador
- **THEN** la alarma suena normalmente sin vibrar
