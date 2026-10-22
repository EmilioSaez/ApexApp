# alarm-volume-lock Specification

## Purpose
Define el forzado del volumen de alarma mientras una alarma suena con nivel anti-trampas activo, para que no se pueda silenciar bajando el volumen.

## Requirements

### Requirement: Volumen máximo al sonar
El sistema SHALL poner el volumen del canal de alarma al máximo al empezar el lockdown, y SHALL restaurar el volumen previo al terminarlo.

#### Scenario: Inicio del lockdown
- **WHEN** empieza el lockdown y el volumen de alarma está al 30 %
- **THEN** el volumen de alarma pasa al máximo

#### Scenario: Fin del lockdown
- **WHEN** el lockdown termina
- **THEN** el volumen de alarma vuelve al valor que tenía antes de empezar

### Requirement: Reaplicación del volumen
El sistema SHALL reaplicar el volumen máximo si el usuario lo baja durante el lockdown, y SHALL consumir las teclas físicas de volumen en la pantalla de sonar.

#### Scenario: Bajar con la tecla física
- **WHEN** el usuario pulsa la tecla de bajar volumen durante el lockdown
- **THEN** el volumen de alarma permanece al máximo

#### Scenario: Bajar desde otra vía
- **WHEN** el volumen de alarma se reduce por otra vía durante el lockdown
- **THEN** el sistema lo devuelve al máximo en menos de un segundo y lo cuenta como intento de manipulación

### Requirement: Límites del volumen
El sistema SHALL indicar en la pantalla de estado que el volumen forzado no controla las salidas externas con volumen propio.

#### Scenario: Auriculares Bluetooth
- **WHEN** el audio se enruta a un dispositivo Bluetooth con volumen independiente
- **THEN** se aplica el máximo al volumen del teléfono y la pantalla de estado documenta la limitación
