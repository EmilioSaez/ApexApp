## MODIFIED Requirements

### Requirement: Tema visual consistente
El sistema SHALL aplicar un tema único con soporte de modo claro y oscuro en todas las pantallas, y SHALL ofrecer un tema personalizado generado a partir de dos colores elegidos por el usuario.

#### Scenario: Elegir tema personalizado
- **WHEN** el usuario selecciona Personalizado y elige un color primario y uno secundario
- **THEN** la app aplica un esquema generado con esos colores en la variante clara u oscura del sistema, y lo conserva al reiniciar

#### Scenario: Contraste legible
- **WHEN** el usuario elige cualquier color, incluidos blanco, negro o muy saturados
- **THEN** el texto sobre primario, secundario y sus contenedores mantiene al menos contraste 4.5:1
