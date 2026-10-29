# Proposal

## Why

El ajuste de tema solo ofrece sistema, claro y oscuro con la paleta roja de la app. Quien quiere otro acento no tiene cómo cambiarlo.

## What Changes

- Nueva opción de tema **Personalizado** en Ajustes > Apariencia, junto a Sistema, Claro y Oscuro.
- Con Personalizado, el usuario elige dos colores (primario y secundario) con un selector de color; la app genera el `ColorScheme` de Material 3 a partir de ellos, ajustando la luminosidad para cumplir contraste AA.
- La variante clara/oscura de Personalizado sigue al sistema. Superficies y colores semánticos no cambian.
- Los colores se guardan en DataStore junto al modo de tema.

No hay cambios **BREAKING**.

## Fuera de alcance

- Color dinámico (Material You), selector de modo claro/oscuro dentro de Personalizado, tema de la pantalla de alarma sonando (sigue el tema del sistema).

## Capabilities

### Modified Capabilities
- `app-shell`: el requisito de tema visual admite un tema personalizado a partir de dos colores.
