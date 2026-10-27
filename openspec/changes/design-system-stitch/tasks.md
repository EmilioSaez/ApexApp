## 1. Diseño en Stitch

- [x] 1.1 Crear proyecto y design system en Stitch (paleta clara/oscura, tipografía, formas) siguiendo `design-taste-compose`
- [x] 1.2 Generar las pantallas de referencia: lista de alarmas, edición, sonar/retos, estadísticas, compartir y ajustes, en claro y oscuro. Hecho: las 7 pantallas (lista, editor, alarma sonando, reto matemático, estadísticas, ajustes, compartir) en oscuro y en claro, con PNG y HTML en `design/screens/`. Limitación de la API: `list_screens` sale vacío y los timeouts no devuelven id, así que las variantes en claro son pantallas nuevas generadas con el design system Apex (ver `design/README.md`). Proyecto Stitch 3824318768696066700.
- [x] 1.3 Validar contraste AA (texto 4.5:1, componentes 3:1) de la paleta clara y oscura con cálculo real (cumple; `outlineVariant` es decorativo y queda exento) y revisar la escala de fuente 1.3 sobre las capturas; resultados en `design/README.md`. Comparación con la app en emulador: corregido el recorte de cifras del selector de hora del editor; el resto de pantallas (alarma sonando, reto, compartir, estadísticas, ajustes) no se compararon en emulador por falta de datos de prueba.
- [x] 1.4 Guardar `DESIGN.md` y capturas en `design/` del repo

## 2. Tema

- [x] 2.1 Sustituir `Color.kt` y los esquemas de `Theme.kt` por la paleta propia; `dynamicColor` falso por defecto
- [x] 2.2 Añadir fuentes en `res/font` y definir `Typography` en `Type.kt`
- [x] 2.3 Añadir `Shape.kt` y `Spacing.kt` (`LocalSpacing`)
- [x] 2.4 Previews claro/oscuro del tema

## 3. Componentes base

- [x] 3.1 Crear en `ui/components/` `PrimaryAction`, `TimeDisplay`, `SectionHeader` y `EmptyState` con previews
- [x] 3.2 Aplicar tema y componentes a `HomeScreen` y `AlarmEditorScreen` sin cambiar su comportamiento
- [x] 3.3 Test de UI del estado vacío y comprobar que `HomeContentTest` sigue pasando

## 4. Preferencia de tema

- [x] 4.1 Añadir dependencia `datastore-preferences` y `SettingsRepository` (dominio, implementación y DI) con `ThemeMode`
- [x] 4.2 Observar la preferencia en `MainActivity` y pasar `darkTheme` a `ApexAppTheme`
- [x] 4.3 Tests unitarios del repositorio y de la resolución claro/oscuro/sistema

## 5. Localización

- [x] 5.1 Crear `values-en/strings.xml` y traducir los strings existentes; prefijar claves por área
- [x] 5.2 Activar lint `MissingTranslation` y `HardcodedText` como error y corregir literales existentes
- [x] 5.3 Añadir `locales_config.xml`, `android:localeConfig` y per-app language (`AppCompatDelegate`, metadata para API < 33)
- [x] 5.4 Adaptar `MainActivity` si es necesario y verificar tests existentes
- [x] 5.5 Comprobar fecha/hora y plurales en ambos idiomas

## 6. Ajustes

- [x] 6.1 Crear `SettingsRoute` y `SettingsScreen` con secciones de tema e idioma
- [x] 6.2 Añadir acceso desde `HomeScreen` y navegación de vuelta
- [x] 6.3 Test de UI: cambiar tema e idioma desde ajustes y comprobar persistencia

## 7. Cierre

- [x] 7.1 Verificar en emulador claro/oscuro, es/en y fuente 1.3
- [x] 7.2 Documentar en `design/README.md` la convención de tokens, componentes y strings para los demás changes
