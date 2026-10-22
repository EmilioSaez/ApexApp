## Purpose
Define el posponer una alarma: configurable y limitado por alarma, dentro de la misma sesión de despertar, y registrado como evento para las estadísticas.

## ADDED Requirements

### Requirement: Política de snooze por alarma
El sistema SHALL permitir configurar por alarma si se puede posponer, cuántos minutos y cuántas veces como máximo, con el snooze desactivado por defecto.

#### Scenario: Alarma sin snooze
- **WHEN** suena una alarma sin snooze permitido
- **THEN** la pantalla de sonar no ofrece posponer

#### Scenario: Snooze configurado
- **WHEN** el usuario configura una alarma con snooze de 5 minutos y 3 veces como máximo
- **THEN** la configuración se conserva al volver a abrir la alarma

### Requirement: Posponer la alarma
El sistema SHALL, al posponer, detener el sonado y volver a hacer sonar la misma alarma pasados los minutos configurados.

#### Scenario: Posponer
- **WHEN** el usuario pospone una alarma con snooze de 5 minutos
- **THEN** el sonado se detiene y la alarma vuelve a sonar 5 minutos después

#### Scenario: Alarma repetitiva
- **WHEN** se pospone una alarma con días de repetición
- **THEN** su siguiente ocurrencia habitual permanece programada

### Requirement: Límite de snoozes
El sistema SHALL dejar de ofrecer posponer cuando se alcanza el máximo configurado en la sesión.

#### Scenario: Máximo alcanzado
- **WHEN** la alarma ya se ha pospuesto el máximo de veces en la sesión
- **THEN** la pantalla de sonar no ofrece posponer y solo se puede resolver con los retos

### Requirement: Misma sesión de despertar
El sistema SHALL mantener el identificador de sesión de despertar entre el sonado original y los sonados tras posponer.

#### Scenario: Sonar tras posponer
- **WHEN** la alarma vuelve a sonar tras un snooze
- **THEN** pertenece a la misma sesión de despertar que el primer sonado

### Requirement: Evento de snooze
El sistema SHALL emitir un evento de snooze con la sesión, la alarma y el número de snooze cada vez que se pospone.

#### Scenario: Registro del snooze
- **WHEN** el usuario pospone una alarma
- **THEN** se emite un evento de snooze con el identificador de la sesión y el contador actualizado
