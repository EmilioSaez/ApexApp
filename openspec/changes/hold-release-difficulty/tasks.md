# Tasks

## 1. Escalado por dificultad

- [x] 1.1 Sustituir `HoldReleaseTiming` por `HoldReleaseTuning` (tabla `BY_LEVEL`: mantenimiento mín./máx., ventana, tope de extras) acotando el nivel a 1..5; verificar con test de monotonía y de niveles fuera de rango.
- [x] 1.2 `HoldReleaseGame` usa el nivel de exigencia para sortear la señal, la ventana y el tope de extras; verificar con tests por nivel.

## 2. Verificación

- [x] 2.1 `./gradlew assembleDebug` y `./gradlew test` en verde.
