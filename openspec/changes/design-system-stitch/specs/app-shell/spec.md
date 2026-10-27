## MODIFIED Requirements

### Requirement: Tema visual consistente
El sistema SHALL aplicar un tema único en todas las pantallas, con paleta clara u oscura según la preferencia del usuario (claro, oscuro o sistema; por defecto sistema).

#### Scenario: Modo oscuro
- **WHEN** el dispositivo está en modo oscuro
- **THEN** la app usa la paleta oscura del tema

#### Scenario: Preferencia explícita
- **WHEN** la preferencia es "claro" y el dispositivo está en modo oscuro
- **THEN** la app usa la paleta clara del tema

## ADDED Requirements

### Requirement: Pantalla de ajustes
El sistema SHALL ofrecer una pantalla de ajustes accesible desde la pantalla inicial que permita elegir tema e idioma, y a la que otros changes puedan añadir secciones.

#### Scenario: Abrir ajustes
- **WHEN** el usuario pulsa el acceso a ajustes en la pantalla inicial
- **THEN** se muestra la pantalla de ajustes y atrás vuelve a la lista de alarmas
