## ADDED Requirements

### Requirement: Opción de verificación de despierto
El esquema SHALL guardar por alarma si la verificación de despierto está activa en la columna `awakeVerificationEnabled`, y la migración a la versión 8 SHALL añadirla con valor desactivado para las alarmas existentes sin perder datos.

#### Scenario: Migración
- **WHEN** se migra una base de datos anterior con alarmas guardadas
- **THEN** las alarmas conservan sus datos y tienen la verificación desactivada
