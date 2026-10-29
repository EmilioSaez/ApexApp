# Proposal

## Why

El tema Personalizado cambia los colores de la app, pero el icono del launcher sigue siendo el rojo original.

## What Changes

- En Ajustes > Apariencia, con tema Personalizado, el usuario puede aplicar el icono del launcher cuya variante es la más cercana a su color primario.
- Android no permite recolorear el icono en runtime. Se usan `activity-alias` (uno por variante: original rojo, naranja, ámbar, verde, turquesa, azul, morado, rosa y neutro) que se habilitan y deshabilitan con `PackageManager.setComponentEnabledSetting(..., DONT_KILL_APP)`. El color elegido se cuantiza por tono; los colores sin tono (blanco, negro, grises) usan la variante neutra.
- El filtro `LAUNCHER` pasa de `MainActivity` a los alias; `MainActivity` sigue habilitada para notificaciones y enlaces.
- El icono original es el valor por defecto y todas las variantes conservan el icono monocromo.
- La variante aplicada se guarda en DataStore. Aplicar es una acción explícita, no automática al cambiar el color.

## Límites

- El launcher puede tardar en refrescar el icono; en algunos launchers o versiones la app se cierra al cambiar de alias.
- Los accesos directos fijados con el icono anterior pueden desaparecer al cambiar de alias.
- Solo hay variantes predefinidas, no cualquier color.

## Fuera de alcance

- Icono dinámico por color arbitrario, cambio automático al editar los colores.

## Capabilities

### Modified Capabilities
- `app-shell`: el tema personalizado admite aplicar un icono de launcher a juego.
