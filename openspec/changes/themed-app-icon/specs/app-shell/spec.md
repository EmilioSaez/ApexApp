## MODIFIED Requirements

### Requirement: Tema visual consistente
El sistema SHALL aplicar un tema único con soporte de modo claro y oscuro en todas las pantallas, SHALL ofrecer un tema personalizado generado a partir de dos colores elegidos por el usuario, y SHALL permitir aplicar un icono de launcher a juego entre un conjunto de variantes predefinidas.

#### Scenario: Aplicar icono a juego
- **WHEN** el tema es Personalizado y el usuario aplica el icono sugerido
- **THEN** se habilita el icono de la variante más cercana a su color primario, se deshabilitan las demás y la elección se conserva al reiniciar

#### Scenario: Volver al icono original
- **WHEN** hay una variante aplicada y el usuario restaura el icono original
- **THEN** el launcher vuelve a mostrar el icono original

#### Scenario: Colores sin tono
- **WHEN** el color primario es blanco, negro o gris
- **THEN** la variante sugerida es la neutra
