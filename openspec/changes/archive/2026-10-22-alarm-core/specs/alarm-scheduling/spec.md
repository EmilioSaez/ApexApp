# Spec Delta

## MODIFIED Requirements

### Requirement: Disparo de la alarma
El sistema SHALL presentar la pantalla de sonar al usuario cuando llega la hora de una alarma activa, incluso con la app cerrada, y SHALL reprogramar las alarmas repetitivas para su siguiente ocurrencia.

#### Scenario: Alarma puntual
- **WHEN** llega la hora de una alarma sin repetición
- **THEN** se presenta la pantalla de sonar y la alarma pasa a inactiva

#### Scenario: Alarma repetitiva
- **WHEN** llega la hora de una alarma con días de repetición
- **THEN** se presenta la pantalla de sonar y queda programada la siguiente ocurrencia

#### Scenario: Alarma ya eliminada o inactiva
- **WHEN** llega la hora de una alarma que se ha borrado o desactivado
- **THEN** no se presenta nada
