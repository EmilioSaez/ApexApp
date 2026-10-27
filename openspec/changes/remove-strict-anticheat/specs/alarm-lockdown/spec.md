## MODIFIED Requirements

### Requirement: Inicio del lockdown con el sonado
El sistema SHALL activar el lockdown cuando empieza el sonado de una alarma cuyo nivel efectivo es `STANDARD`, y SHALL no activarlo con nivel `OFF`.

#### Scenario: Alarma STANDARD empieza a sonar
- **WHEN** empieza a sonar una alarma con nivel efectivo `STANDARD`
- **THEN** la pantalla de sonar queda en modo kiosko y el volumen forzado se activa

#### Scenario: Alarma OFF
- **WHEN** empieza a sonar una alarma con nivel `OFF`
- **THEN** no se aplica ninguna medida y el usuario puede salir de la app libremente

### Requirement: Fin del lockdown
El sistema SHALL liberar el modo kiosko, las medidas de resistencia y el volumen forzado cuando el sonado termina por resolución, snooze o agotamiento del tiempo máximo.

#### Scenario: Alarma resuelta
- **WHEN** el sonado termina porque el usuario completó los retos
- **THEN** se libera el lockdown y la app vuelve a su navegación normal

#### Scenario: Tiempo máximo agotado
- **WHEN** el sonado termina por superar el tiempo máximo sin resolverse
- **THEN** se libera el lockdown completamente

#### Scenario: Snooze
- **WHEN** el sonado termina porque el usuario pospone la alarma
- **THEN** se libera el lockdown hasta que la alarma vuelva a sonar
