## ADDED Requirements

### Requirement: Textos en español e inglés
El sistema SHALL ofrecer todos los textos visibles al usuario en español e inglés mediante recursos de strings, con el español como idioma base.

#### Scenario: Dispositivo en inglés
- **WHEN** el idioma del dispositivo es inglés y la app está en "sistema"
- **THEN** los textos se muestran en inglés

#### Scenario: Idioma no soportado
- **WHEN** el idioma del dispositivo no es español ni inglés
- **THEN** los textos se muestran en español

### Requirement: Selección de idioma de la app
El sistema SHALL permitir elegir idioma entre sistema, español e inglés, SHALL aplicarlo sin reinstalar y SHALL conservarlo entre sesiones.

#### Scenario: Cambiar a inglés
- **WHEN** el usuario elige inglés en ajustes
- **THEN** la interfaz pasa a inglés

#### Scenario: Persistencia
- **WHEN** el usuario elige un idioma y reinicia la app
- **THEN** se mantiene el idioma elegido

### Requirement: Sin texto literal en la UI
El sistema SHALL obtener todo texto visible de recursos de strings, incluidas descripciones de contenido y notificaciones, y SHALL usar formatos de hora y plurales acordes al idioma.

#### Scenario: Cadena nueva
- **WHEN** se añade una cadena visible
- **THEN** existe en `values` y en `values-en`

#### Scenario: Plurales
- **WHEN** se muestra una cantidad (p. ej. días de racha)
- **THEN** se usa un recurso `plurals` correcto en ambos idiomas
