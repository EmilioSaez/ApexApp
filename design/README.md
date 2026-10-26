# Sistema de diseño de ApexApp

Referencia para quien implemente pantallas. Dirección de arte: skill `design-taste-compose`.

## Stitch

Proyecto `3824318768696066700` ("ApexApp"), design system `assets/8284090323799835001` ("Apex"):
oscuro primero, acento naranja señal, Sora (titulares/hora) + Inter (resto), radio 4 dp.
`design/DESIGN.md` resume el sistema; capturas en `design/screens/` y HTML en `design/screens/html/`.

### Pantallas de referencia

| Pantalla | Oscuro | Claro |
|---|---|---|
| Lista de alarmas | `lista-alarmas` | `lista-alarmas-claro` |
| Editor | `editor` | `editor-claro` |
| Alarma sonando | `alarma-sonando` | `alarma-sonando-claro` |
| Reto matemático | `reto-matematico` | `reto-matematico-claro` |
| Estadísticas | `estadisticas` | `estadisticas-claro` |
| Ajustes | `ajustes` | `ajustes-claro` |
| Compartir | `compartir` | `compartir-claro` |

Cada nombre existe como `.png` (780 px de ancho) y como `html/<nombre>.html`. Los ids de Stitch están en `DESIGN.md`.

### Limitaciones de la API de Stitch

- `list_screens` devuelve vacío y `get_project` no trae `screenInstances`: un id solo se conoce si la llamada de
  generación lo devuelve. Dos generaciones del reto matemático (oscuro y claro) y una `generate_variants` acabaron en
  timeout sin id, así que no se pudieron recuperar; se regeneraron con prompts más cortos.
- `generate_variants` no sirvió para las versiones en claro (timeout sin id). Las 7 pantallas en claro son pantallas
  nuevas generadas con `generate_screen_from_text` y el design system Apex, indicando el modo claro y `#B93E00` en el prompt.
  El tema guardado del proyecto sigue siendo oscuro (`colorMode: DARK`); el claro vive solo en esas pantallas.
- `download_file` devuelve los bytes en línea; las capturas se bajaron de la `downloadUrl` de `get_screen` (`lh3...=w780`).
- Stitch añade por su cuenta cosas que el sistema descarta: tarjetas con borde en las listas claras, barra inferior
  Alarmas/Retos/Historial y numeración. Son guía de composición, no especificación (ver la app real para lo vigente).

### Accesibilidad (verificado)

Cálculo WCAG 2.x sobre `ui/theme/Color.kt` (luminancia relativa; texto >= 4.5:1, componentes >= 3:1):

- **Oscuro**: todos los pares de texto cumplen (mínimo 5.37:1, `onSurfaceVariant` sobre `surfaceContainerHighest`;
  `primary` sobre `surface` 7.76:1; `onPrimary` sobre `primary` 6.95:1; `error` 6.53:1; éxito 9.86:1). Componentes:
  `primary` 7.76, `outline` 4.61 (4.06 sobre `surfaceContainer`), éxito 9.86, error 6.53.
- **Claro**: todos los pares de texto cumplen (mínimo 4.72:1, `primary` sobre `surfaceContainer`;
  `primary` sobre `surface` 5.22:1; `onPrimary` sobre `primary` 5.60:1; `error` 6.03:1; éxito 5.01:1). Componentes:
  `primary` 5.22, `outline` 4.53 (4.09 sobre `surfaceContainer`), éxito 5.01, error 6.03.
- Excepción conocida: `outlineVariant` (1.71:1 oscuro, 1.55:1 claro) es solo separador decorativo, exento por WCAG 1.4.11;
  la app no lo usa fuera del tema. Un borde que delimite un campo debe usar `outline`.
- Colores de las pantallas de Stitch en claro (`#B93E00`/`#F8F9FA` 5.31:1, blanco/`#B93E00` 5.60:1, éxito `#1B7F4B` 4.76:1,
  fallo `#B3261E` 6.20:1) y en oscuro (`#FF8A4C`/`#0C0E12` 8.27:1, `#FF6B6B` 6.96:1) también cumplen.

### Fuente 1.3 sobre los diseños

Revisión de las capturas (780 px = 360 dp a 2.17x) con texto un 30 % mayor:

- Alarma sonando (claro): el botón "Resolver reto para apagar" ya se parte en dos líneas a 1.0; a 1.3 hay que
  dejar que el botón crezca en alto, nunca fijar su altura.
- Reto matemático (claro): botón y teclado ocupan casi todo el alto; a 1.3 el contenido debe hacer scroll por encima del teclado.
- Editor: las siete casillas de días son de ancho fijo; a 1.3 el rótulo de dos letras ("L", "M"...) sigue cabiendo,
  pero la fila no admite etiquetas más largas. El título "Reto para apagar" + "Imposible posponer" compite por línea: debe poder envolver.
- Lista, estadísticas, ajustes y compartir: filas con texto que envuelve; sin cortes previsibles.

La app se verificó en emulador con fuente 1.3 (change `design-system-stitch`, 7.1) y cada `@Preview` de pantalla incluye `fontScale` 1.3.

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
