# alarm-ring Specification

## Purpose
Define la pantalla que se muestra cuando suena una alarma y cómo el usuario la descarta, dejando un punto de extensión para los retos y las políticas anti-trampa.

## Requirements

### Requirement: Pantalla de sonar
El sistema SHALL mostrar una pantalla de sonar con la hora y la etiqueta de la alarma cuando esta se dispara, también con el dispositivo bloqueado o la pantalla apagada.

#### Scenario: Dispositivo bloqueado
- **WHEN** se dispara una alarma con el dispositivo bloqueado
- **THEN** la pantalla se enciende y muestra la pantalla de sonar sobre el bloqueo

#### Scenario: Dispositivo en uso
- **WHEN** se dispara una alarma con el dispositivo desbloqueado
- **THEN** se muestra una notificación de alarma de alta prioridad que abre la pantalla de sonar

#### Scenario: Pantalla completa no permitida
- **WHEN** el sistema no permite lanzar la pantalla completa
- **THEN** se muestra la notificación de alarma, que abre la pantalla de sonar al pulsarla

### Requirement: Descartar la alarma
El sistema SHALL permitir descartar una alarma que está sonando desde la pantalla de sonar, retirando su notificación y cerrando la pantalla.

#### Scenario: Descartar
- **WHEN** el usuario descarta la alarma en la pantalla de sonar
- **THEN** la notificación desaparece y la pantalla se cierra

#### Scenario: Alarma repetitiva descartada
- **WHEN** se descarta una alarma con días de repetición
- **THEN** su siguiente ocurrencia permanece programada

### Requirement: Punto de extensión de descarte
El sistema SHALL decidir si una alarma puede descartarse a través de un único punto de extensión que, sin retos ni política anti-trampa configurados, permite descartar de inmediato.

#### Scenario: Alarma sin retos
- **WHEN** una alarma sin retos configurados suena
- **THEN** la pantalla de sonar ofrece descartarla directamente

### Requirement: Sesión de alarma
El sistema SHALL asociar cada disparo de una alarma a un identificador de sesión único y SHALL pasarlo a la pantalla de sonar.

#### Scenario: Dos disparos
- **WHEN** una alarma repetitiva se dispara en dos días distintos
- **THEN** cada disparo tiene un identificador de sesión diferente
