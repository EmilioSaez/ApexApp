## MODIFIED Requirements

### Requirement: Tema visual consistente
El sistema SHALL aplicar un tema único con soporte de modo claro y oscuro en todas las pantallas, y SHALL ofrecer un tema personalizado generado a partir de dos colores y una base (sistema, clara u oscura) elegidos por el usuario.

#### Scenario: Elegir base oscura
- **WHEN** el usuario selecciona Personalizado y la base Oscuro
- **THEN** la app aplica sus colores sobre las superficies oscuras aunque el sistema sea claro, y lo conserva al reiniciar

#### Scenario: Contraste legible
- **WHEN** el usuario elige cualquier color con base clara u oscura
- **THEN** el texto sobre primario, secundario y sus contenedores mantiene al menos contraste 4.5:1
