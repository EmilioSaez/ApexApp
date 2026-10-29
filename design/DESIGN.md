# Apex — design system (exportado de Stitch)

Proyecto Stitch `3824318768696066700`, design system `assets/8284090323799835001` (v2). Modo base: oscuro.

Despertador que obliga a despertar con retos. Oscuro primero (uso nocturno y matutino).

- Superficies neutras frías en escalones de tono, sin sombras ni degradados.
- Un único acento naranja señal (#FF8A4C oscuro / #B93E00 claro) solo en la acción principal y el dato clave.
- Colores semánticos: éxito #5BD68A, fallo #FF6B6B.
- La hora es lo dominante: display grande Sora, cifras tabulares, interlineado 1.0.
- Radio pequeño único (4 dp), listas como filas separadas por espacio, no rejillas de tarjetas.
- Sin emoji, sin iconos decorativos, sin glassmorphism.
- Áreas táctiles >= 48 dp, contraste AA.

## Tema de Stitch

| Campo | Valor |
|---|---|
| Titulares | Sora |
| Cuerpo / etiquetas | Inter |
| Roundness | ROUND_FOUR |
| Color primario (seed) | #FF8A4C |
| Neutro | #14161A |
| Variante | NEUTRAL |

Paleta tonal calculada por Stitch (oscuro): background `#0c0e12`, surface_container `#161a21`,
surface_container_high `#1b2029`, surface_container_highest `#202631`, on_surface `#e0e5f4`,
outline `#6f7682`, outline_variant `#424854`, primary `#ffb693`, primary_container `#7a3000`.

> La app **no** usa la paleta tonal de Stitch: `ui/theme/Color.kt` define sus propios esquemas claro y oscuro
> con el mismo acento. Esta tabla es solo referencia visual.

## Pantallas de referencia

Capturas en `design/screens/*.png` y HTML en `design/screens/html/*.html`. Las versiones en claro llevan el sufijo `-claro`.

| Pantalla | Archivo | Id Stitch (oscuro) | Id Stitch (claro) |
|---|---|---|---|
| Lista de alarmas | lista-alarmas | c1d90860c8b54da78178d6629f136955 | 1c03c20eb27344b68e9543629bc4b06b |
| Editar alarma | editor | 9175576ae0454293a65d74ed78caa505 | 905bb3c6c1f34dda9afa6fc941f3d356 |
| Alarma sonando | alarma-sonando | 959a782682d04f118a55b5859ccade8a | e39744b40d234b7da4d1559d1a968b6a |
| Reto matemático | reto-matematico | 57bbfaebd2c846e1b168c8b9c3b99c79 | cc8fad1cf5f34a6886b434496f30a577 |
| Estadísticas | estadisticas | f0cb0f8a71994c3989349a213375bce2 | 75906a895be244368ee2fcb760ef332b |
| Ajustes | ajustes | 01110cd4dab4462a8f4076cf83340d1a | 127c7fd7fcf44bb3abad46dc36ec48ae |
| Compartir | compartir | 25f10474d34e401a81bf8da098f02bd3 | cb6be50ef9b1419f96d1ec9d10a7f2cc |

Son guía de composición, no especificación pixel a pixel: p. ej. la barra inferior (Alarmas / Retos / Historial)
que propone Stitch en la lista no forma parte de ningún change.

## Pantallas sin Stitch (retos nuevos y mejoras de alarma)

El acento vigente es el rojo de `ui/theme/Color.kt` (`#FF4D4F` oscuro / `#C4161C` claro), no el naranja de Stitch.
Éxito = `tertiary` y aviso o fallo = `error` en ámbar. Estas pantallas usan esos tokens directamente
(`screens/html/apex-tokens.css`) y no tienen id de Stitch. Especificación completa en `README.md`, sección «Retos
nuevos y mejoras de alarma».

| Pantalla | Archivo |
|---|---|
| Secuencia de formas | reto-secuencia-formas, reto-secuencia-formas-observar |
| Laberinto | reto-laberinto, reto-laberinto-error, reto-laberinto-grande |
| Memoria de cuadrícula | reto-memoria-cuadricula, reto-memoria-cuadricula-observar, reto-memoria-cuadricula-grande |
| Editor · silencio, posponer y verificar | editor-silencio-posponer-verificar |
| Editor · ajustes de cada reto | editor-dificultad-retos |
| Alarma sonando sin posposiciones | alarma-sonando-sin-posposiciones |
| Notificación de verificación | notificacion-verificar |

Solo existen en HTML (`screens/html/<archivo>.html` y `-claro`); los PNG quedan pendientes. El margen de 2 min de
«Estoy despierto» y las cifras de ejemplo de dificultad son supuestos que hay que confirmar con el código real.

El laberinto en oscuro de Stitch (`dde4d599f1004427a0c3442ab8a91b80`, trazo en curso) queda solo como referencia:
usa la paleta naranja de Stitch y lo sustituye `reto-laberinto`.
