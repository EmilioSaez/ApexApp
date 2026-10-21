# Design

## Context

Estado observado: `ui/theme` es la plantilla de Android Studio (`Purple40/80`, `dynamicColor = true`, `Typography` con solo `bodyLarge`). Hay dos pantallas (`HomeScreen`, `AlarmEditorScreen` placeholder) y navegación type-safe (`HomeRoute`, `AlarmEditorRoute`). Los strings viven en un único `res/values/strings.xml` en español. Persistencia con Room y DI con módulos en `di/`; no hay DataStore. Ver proposal.md - Why.

Otros 9 changes tocan pantallas en paralelo, así que este change fija contratos (tokens, componentes, convención de strings) y evita tocar su lógica.

## Goals / Non-Goals

**Goals:**
- Un tema y unos componentes base estables que los demás changes consuman sin cambiarlos.
- Diseños de referencia de las 6 pantallas clave versionados en el repo.
- Cambio de tema e idioma en caliente, persistente.

**Non-Goals:**
- Implementar la lógica o el contenido real de alarmas, retos, sonido, estadísticas o compartir; las pantallas de referencia son solo diseño.
- Más idiomas que es/en (la infraestructura debe permitir añadirlos con solo un `values-xx`).
- Animaciones complejas y diseño de iconografía de lanzador.

## Decisions

1. **Stitch como fuente del diseño, Compose como fuente de verdad del código.** Se crea un proyecto y un design system en Stitch (`create_project`, `create_design_system`), se generan las 6 pantallas (`generate_screen_from_text`) en claro y oscuro y se guarda en `design/` el `DESIGN.md` y capturas (`download_file`). El código no se genera desde Stitch: se traduce a `ColorScheme`/`Typography` a mano, guiado por `design-taste-compose`. Alternativa: exportar código de Stitch; descartada por ser HTML/web y producir justo el aspecto genérico que se quiere evitar.

2. **Dirección de arte: oscura primero, un solo acento, datos con color semántico.** Despertador de uso nocturno y matutino: superficies neutras, escalones de `surfaceContainer*` en vez de sombras, un acento para la acción principal y colores semánticos para estado (alarma activa, reto superado/fallado). La hora es el elemento tipográfico dominante (display grande, cifras tabulares). La paleta concreta y las fuentes (máx. dos familias, OFL, en `res/font`) se fijan en el design system de Stitch al inicio de la implementación.

3. **Sin `dynamicColor`.** Se mantiene el parámetro en `ApexAppTheme` con valor `false` por defecto, para poder activarlo después sin cambiar la API. Alternativa: dejarlo activo; descartada porque diluye la identidad y hace el contraste impredecible.

4. **Tokens en `ui/theme`:** `Color.kt` (esquemas claro/oscuro), `Type.kt` (pocos tamaños bien separados), `Shape.kt`, `Spacing.kt` expuesto vía `CompositionLocal` (`LocalSpacing`). Componentes compartidos en `ui/components/`: `PrimaryAction`, `TimeDisplay`, `SectionHeader`, `EmptyState`. Solo se crean los que al menos dos pantallas necesitan.

5. **Preferencias en DataStore (Preferences), no en Room.** Tema e idioma son ajustes clave-valor sin relación con las alarmas; Room obligaría a migración y entidad. Se añade `androidx.datastore:datastore-preferences` y un `SettingsRepository` (interfaz en `domain`, implementación en `data`, binding en `di/`), con `Flow<ThemeMode>`. `MainActivity` observa el flujo y pasa `darkTheme` a `ApexAppTheme`. Alternativa: `SharedPreferences`; descartada por no ser reactiva.

6. **Idioma con per-app language de AndroidX.** `AppCompatDelegate.setApplicationLocales` (compatible API 29+, con `AppLocalesMetadataHolderService` en el manifest para API < 33) más `res/xml/locales_config.xml` y `android:localeConfig`, de modo que el usuario también pueda cambiarlo en los ajustes del sistema en API 33+. La app guarda la elección en el propio mecanismo del sistema (no se duplica en DataStore). `MainActivity` debe pasar a `AppCompatActivity` (o `ComponentActivity` con la API de `LocaleManager` en 33+); se decide al implementar según el impacto en tests. Alternativa: contexto envuelto manualmente; descartada por frágil.

7. **Strings:** español como `values/` (base) e inglés en `values-en/`. Convención con lint: `MissingTranslation` y `HardcodedText` como error en el módulo `app`. Los strings existentes se migran tal cual y se traducen. Los otros changes añaden sus claves en ambos archivos; para evitar conflictos de merge se recomienda prefijar por área (`alarm_`, `challenge_`, `stats_`, `share_`, `settings_`).

8. **Ajustes:** nueva `SettingsRoute` con solo tema e idioma, estructurada en secciones (`SectionHeader`) para que alarm-sound, anti-cheat, etc. añadan las suyas. Acceso desde un icono en `HomeScreen`.

9. **Pantallas de referencia no se implementan en Compose** salvo ajustes y los componentes base. Home y editor se restilizan solo para usar el tema/componentes, sin cambiar su comportamiento.

## Risks / Trade-offs

- [Conflictos con 9 changes paralelos en `strings.xml`, `AppNavigation.kt` y `HomeScreen.kt`] → Mergear este change primero, o al menos el tema y los strings; prefijos de claves; cambios mínimos en archivos compartidos.
- [Stitch genera un diseño poco viable en Compose o genérico] → Iterar con `edit_screens`/`generate_variants` y revisar con la skill; el `DESIGN.md` manda, las capturas son guía.
- [Per-app language exige `AppCompatActivity` y puede romper tests de UI existentes (`HomeContentTest`)] → Probar pronto en el emulador y ajustar los tests.
- [Fuentes añaden peso al APK] → Limitar a dos familias y pesos usados.
- [Paleta propia sin dynamic color pierde integración con el sistema] → Aceptado; se puede reactivar como opción en ajustes más adelante.

## Open Questions

- Nombre de producto/tono visual final (se concreta al crear el design system en Stitch).
- Si el idioma de pantalla de alarma/notificaciones debe seguir el idioma de la app o el del sistema (por defecto, el de la app).
