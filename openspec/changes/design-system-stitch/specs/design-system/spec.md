## ADDED Requirements

### Requirement: Tema propio de la app
El sistema SHALL definir un tema único con esquema de color claro y oscuro propios, tipografía, formas y espaciado, accesibles en toda la UI mediante `MaterialTheme` y sin valores de color o tipografía sueltos en las pantallas.

#### Scenario: Pantalla consume el tema
- **WHEN** una pantalla necesita un color, estilo de texto o forma
- **THEN** lo obtiene de `MaterialTheme` o de los tokens del tema, no de un literal

#### Scenario: Sin paleta de plantilla
- **WHEN** se renderiza cualquier pantalla
- **THEN** no se usan los colores morados por defecto de la plantilla ni colores dinámicos del sistema

### Requirement: Modo de tema claro, oscuro o sistema
El sistema SHALL permitir elegir entre tema claro, oscuro o seguir al sistema, SHALL aplicar la elección de inmediato y SHALL conservarla entre sesiones. El valor por defecto es seguir al sistema.

#### Scenario: Forzar oscuro con el dispositivo en claro
- **WHEN** el usuario elige tema oscuro y el dispositivo está en modo claro
- **THEN** la app muestra la paleta oscura

#### Scenario: Seguir al sistema
- **WHEN** el usuario elige "sistema" y el dispositivo cambia de modo
- **THEN** la app cambia a la paleta correspondiente

#### Scenario: Persistencia de la elección
- **WHEN** el usuario elige un tema y reinicia la app
- **THEN** se mantiene el tema elegido

### Requirement: Contraste y escala de fuente
El sistema SHALL cumplir contraste WCAG AA (4.5:1 texto, 3:1 componentes) en ambos temas y SHALL mantener las pantallas utilizables con escala de fuente de 1.3 o superior.

#### Scenario: Fuente grande
- **WHEN** el usuario tiene la escala de fuente del sistema a 1.3
- **THEN** los textos no se truncan de forma que oculten la hora ni la acción principal

### Requirement: Componentes base reutilizables
El sistema SHALL ofrecer componentes Compose compartidos construidos sobre el tema: acción principal, display de hora, cabecera de sección y estado vacío, de modo que otras pantallas los reutilicen en lugar de crear variantes propias.

#### Scenario: Estado vacío compartido
- **WHEN** una lista no tiene elementos
- **THEN** se muestra el componente de estado vacío con mensaje localizado

### Requirement: Diseños de referencia en Stitch
El sistema SHALL mantener en el repositorio el diseño de referencia generado con Stitch (`DESIGN.md` y capturas) para las pantallas: lista de alarmas, edición, sonar/retos, estadísticas, compartir y ajustes, en tema claro y oscuro.

#### Scenario: Referencia disponible para otros changes
- **WHEN** otro change implementa una de esas pantallas
- **THEN** encuentra su diseño de referencia y los tokens en el repositorio
