# Proposal

## Why

El tema actual es el de plantilla de Android Studio (morado por defecto, `dynamicColor` activado, tipografía de sistema) y los textos son solo español, en un único `strings.xml`. Los otros changes en paralelo (alarm-core, challenges, stats, sharing…) van a construir pantallas sobre esta base; si no hay un sistema de diseño y una infraestructura de idiomas antes, cada uno inventará los suyos y la app saldrá genérica e inconsistente.

## What Changes

- Definir con Stitch (MCP `stitch`) un sistema de diseño propio para un despertador de retos: paleta clara y oscura, tipografía, formas, espaciado y componentes base, siguiendo la skill `design-taste-compose`.
- Diseñar en Stitch las pantallas clave como referencia visual: lista de alarmas, edición de alarma, pantalla de sonar/retos, estadísticas, compartir y ajustes. Exportar el `DESIGN.md` y capturas al repo.
- Implementar el tema en `ui/theme` (`ColorScheme` claro/oscuro propios, `Typography`, `Shapes`, tokens de espaciado) y reemplazar la plantilla morada.
- Añadir selección de tema: claro / oscuro / sistema, persistida localmente.
- Componentes Compose compartidos mínimos (acción principal, display de hora, cabecera de sección, estado vacío) para que otros changes los reutilicen.
- Infraestructura multi-idioma: strings en español (por defecto) e inglés (`values-en`), selección de idioma (sistema / español / inglés) con per-app language, y migración de los strings existentes.
- Convención: todo texto visible va a recursos de strings; sin literales en Compose.
- Fuera de alcance: la lógica de cada pantalla (alarmas, retos, sonido, estadísticas, compartir). Este change entrega aspecto, tema, componentes base y pantallas de referencia; el contenido funcional lo aportan los demás changes. La pantalla de ajustes solo incluye tema e idioma.
- **BREAKING**: `dynamicColor` deja de estar activo por defecto (paleta propia).

## Capabilities

### New Capabilities
- `design-system`: tokens de tema (color, tipografía, formas, espaciado) con modo claro/oscuro/sistema, componentes base reutilizables y diseños de referencia de las pantallas clave en Stitch.
- `localization`: recursos de texto en español e inglés, selección de idioma de la app y regla de no usar texto literal en la UI.

### Modified Capabilities
- `app-shell`: el requisito "Tema visual consistente" pasa de claro/oscuro automático a respetar la preferencia de tema del usuario (claro, oscuro o sistema); se añade un destino de navegación de ajustes.

## Impact

- Código: `ui/theme/*` (Color, Type, Theme, nuevos Shape/Spacing), `res/values/strings.xml`, nuevo `res/values-en/strings.xml`, `res/font/`, `ui/components/` (nuevo), `ui/settings/` (nuevo, solo tema e idioma), `ui/navigation/AppNavigation.kt`, `AndroidManifest` (`localeConfig`), `res/xml/locales_config.xml`.
- Persistencia: preferencia de tema e idioma (mecanismo a decidir en design, reutilizando lo existente en `alarm-storage` si encaja).
- Dependencias: posible `androidx.appcompat` para per-app language en API < 33; fuentes (Google Fonts, licencia OFL).
- Herramientas: MCP `stitch` (proyecto y design system en Stitch); artefactos en `design/` del repo.
- Coordinación: los 9 changes paralelos consumen el tema y los componentes; deben usar `stringResource` y añadir sus textos en ambos idiomas.
