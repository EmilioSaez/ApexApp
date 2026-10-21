# Apex

App Android de alarmas (Kotlin, Jetpack Compose, Room, Hilt).

## Estructura de paquetes

Un solo módulo `:app`, paquete `app.luxion.apexapp`, organizado en capas:

| Paquete   | Contenido |
|-----------|-----------|
| `domain`  | Modelos (`Alarm`, `Sound`), interfaces de repositorio y `nextOccurrence` (función pura, sin Android). |
| `data`    | Room (`data.local`: entidades, DAOs, `ApexDatabase`), repositorios y mapeo entidad/dominio. |
| `alarm`   | Programación con `AlarmManager.setAlarmClock` (`AlarmScheduler`), `AlarmController`, receivers de disparo y de arranque, notificaciones y permisos. |
| `ui`      | Pantallas Compose, ViewModels (`StateFlow`), navegación tipada y tema. |
| `di`      | Módulos Hilt. |

El esquema de Room se exporta a `app/schemas/`. En `debug` hay un receiver para crear alarmas desde `adb`:

```
adb shell am broadcast -n app.luxion.apexapp/.debug.DebugAlarmReceiver --ei minutesFromNow 2 --es label Prueba
```

## Tests

```
./gradlew test                      # unitarios (dominio, scheduler, receivers, ViewModels)
./gradlew connectedDebugAndroidTest # instrumentados: Room en memoria y Compose (requiere emulador/dispositivo)
./gradlew assembleDebug             # compilar
```
