## ADDED Requirements

### Requirement: Rondas del reto Tocar objetivos en el editor
El sistema SHALL mostrar, mientras el tipo «Tocar objetivos» esté habilitado en una alarma en modo «Con retos», un control del número de rondas (3 a 15) con su valor actual, y SHALL guardarlo en los parámetros de ese tipo sin bloquear el guardado. La dificultad del reto sigue la dificultad general de la sección.

#### Scenario: Cambiar las rondas
- **WHEN** el usuario habilita «Tocar objetivos», fija 12 rondas y guarda la alarma
- **THEN** al reabrirla el control muestra 12 rondas

#### Scenario: Tipo deshabilitado
- **WHEN** el usuario deshabilita «Tocar objetivos» o cambia la alarma a modo Normal
- **THEN** el control de rondas no se muestra
