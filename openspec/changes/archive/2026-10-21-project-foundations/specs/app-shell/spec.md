# Spec Delta

## Purpose

Define el contenedor de la aplicación: arranque, tema visual y navegación entre pantallas, sobre el que se añadirán las funcionalidades posteriores.

## ADDED Requirements

### Requirement: Arranque de la aplicación
El sistema SHALL abrir la app en una pantalla inicial que muestra la lista de alarmas, o un estado vacío si no hay ninguna.

#### Scenario: Primera apertura
- **WHEN** se abre la app sin alarmas guardadas
- **THEN** se muestra un estado vacío

#### Scenario: Apertura con alarmas
- **WHEN** se abre la app con alarmas guardadas
- **THEN** se muestra la lista de esas alarmas

### Requirement: Navegación entre pantallas
El sistema SHALL permitir navegar entre pantallas mediante un grafo de navegación y volver atrás con el gesto o botón de retroceso del sistema.

#### Scenario: Navegar y volver
- **WHEN** el usuario navega a otra pantalla y pulsa atrás
- **THEN** regresa a la pantalla anterior

### Requirement: Tema visual consistente
El sistema SHALL aplicar un tema único con soporte de modo claro y oscuro en todas las pantallas.

#### Scenario: Modo oscuro
- **WHEN** el dispositivo está en modo oscuro
- **THEN** la app usa la paleta oscura del tema
