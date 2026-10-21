# Sistema de diseño de ApexApp

Referencia para quien implemente pantallas. Dirección de arte: skill `design-taste-compose`.

## Stitch

Proyecto `3824318768696066700` ("ApexApp"), design system `assets/8284090323799835001` ("Apex"):
oscuro primero, acento naranja señal, Sora (titulares/hora) + Inter (resto), radio 4 dp.
Las pantallas de referencia (lista, edición, sonar/retos, estadísticas, compartir, ajustes) **aún no se han
podido generar**: `generate_screen_from_text` agotó el tiempo en todos los intentos. Cuando funcione, guardar
aquí `DESIGN.md` y las capturas.

## Tema (`ui/theme`)

- Color: `DarkColorScheme` / `LightColorScheme` en `Color.kt`. Un acento (`primary`); éxito = `tertiary`,
  fallo = `error`. Niveles con `surfaceContainer*`, sin sombras. `dynamicColor = false` por defecto.
- Tipografía: `Type.kt`, fuentes variables en `res/font` (Sora, Inter; OFL). La hora usa `displayLarge`/`displayMedium`
  con cifras tabulares.
- Forma: un solo radio (`Shape.kt`, 4 dp).
- Espaciado: `ApexTheme.spacing` (`xs`…`xxxl`, `screen`).
- Nunca `Color(0xFF…)`, `sp` ni `dp` de diseño sueltos en pantallas: todo vía `MaterialTheme` / `ApexTheme`.

## Componentes (`ui/components`)

`PrimaryAction`, `TimeDisplay`, `SectionHeader`, `EmptyState`. Reutilízalos antes de crear variantes.
Listas como filas, no tarjetas.

## Ajustes (`ui/settings`)

Añade tu sección dentro de la columna de `SettingsContent` con `SectionHeader` + contenido.

## Strings

- Todo texto visible en `res/values/strings.xml` (español, base) y `res/values-en/strings.xml`.
- Prefijo de clave por área: `alarm_`, `challenge_`, `stats_`, `share_`, `settings_`…
- Lint `MissingTranslation` y `HardcodedText` son error.
- Los tests de UI obtienen los textos de recursos (`targetContext.getString`), no literales.
