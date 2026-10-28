# Proposal

## Why

El tema Personalizado sigue la variante clara/oscura del sistema. Quien quiere un tema oscuro propio con sus colores no puede fijarlo.

## What Changes

- Ajustes > Apariencia > Personalizado añade la **base**: Sistema (por defecto), Claro u Oscuro.
- La base se guarda en DataStore junto a los colores y el esquema generado usa las superficies de esa variante, con contraste AA.

No hay cambios **BREAKING**: sin base guardada se mantiene el comportamiento actual (Sistema).

## Capabilities

### Modified Capabilities
- `app-shell`: el tema personalizado admite elegir base clara u oscura.
