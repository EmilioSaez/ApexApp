## ADDED Requirements

### Requirement: Registro del disparo
El sistema SHALL registrar un evento «sonó» con el instante programado y el instante real cada vez que se dispara una alarma activa, sin retrasar ni impedir la notificación si el registro falla.

#### Scenario: Disparo registrado
- **WHEN** llega la hora de una alarma activa
- **THEN** se notifica al usuario y se registra un evento «sonó»

#### Scenario: Fallo al registrar
- **WHEN** el registro del evento falla
- **THEN** la alarma se notifica y se reprograma igualmente
