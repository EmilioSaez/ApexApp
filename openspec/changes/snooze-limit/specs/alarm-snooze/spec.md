## MODIFIED Requirements

### Requirement: Límite de snoozes
El sistema SHALL permitir configurar por alarma el máximo de posposiciones por disparo (de 0 a 10 o sin límite), SHALL reiniciar el contador en cada disparo y SHALL dejar de ofrecer posponer, por cualquier vía, cuando se alcanza el máximo. El valor por defecto SHALL ser 3.

#### Scenario: Máximo alcanzado
- **WHEN** la alarma ya se ha pospuesto el máximo de veces en la sesión
- **THEN** la pantalla de sonar no ofrece posponer y solo se puede resolver con los retos

#### Scenario: Sin límite
- **WHEN** la alarma está configurada sin límite de posposiciones
- **THEN** se puede posponer siempre y no se muestra contador de restantes

#### Scenario: Contador reiniciado por disparo
- **WHEN** una alarma que agotó sus posposiciones vuelve a dispararse en su siguiente ocurrencia
- **THEN** dispone de nuevo de todas sus posposiciones

#### Scenario: Alarmas existentes
- **WHEN** se abre una alarma guardada antes de este cambio
- **THEN** conserva su máximo anterior

### Requirement: Posposiciones restantes visibles
El sistema SHALL mostrar en la pantalla de alarma sonando cuántas posposiciones quedan cuando hay límite, y SHALL indicar que se agotaron cuando no queda ninguna.

#### Scenario: Quedan posposiciones
- **WHEN** suena una alarma con máximo 3 tras posponerla una vez
- **THEN** la pantalla muestra el botón de posponer y «2 restantes»

#### Scenario: Agotadas
- **WHEN** no queda ninguna posposición
- **THEN** el botón se oculta y la pantalla indica que se agotaron
