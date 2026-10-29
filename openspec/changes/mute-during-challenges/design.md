## Context

El sonado lo gestiona `AlarmRingingService` con `AlarmSoundEngine` (tono + rampa + vibración + voz). La pantalla de sonar (`RingScreen`) aloja la `ChallengeSession`. El bloqueo de volumen (`VolumeLock`) fuerza el volumen del stream de alarma al máximo durante el lockdown. Hay changes en paralelo que suben Room a v7 (límite de posposición) y v8 (verificar despierto).

## Goals / Non-Goals

**Goals:** silenciar durante la interacción con los retos; volver a sonar por inactividad; lógica pura testeable; no romper anti-trampas ni volumen.
**Non-Goals:** compartir la opción; sensores como señal de interacción; cambiar el tiempo máximo de sonado.

## Decisions

1. **Qué se silencia: sonido, vibración y voz.** La vibración se silencia también: con el teléfono en la mano interfiere con tocar la pantalla y con los retos de precisión. Al reactivar, todo vuelve a la vez (tono con la ganancia de la rampa en curso, vibración con su patrón y voz en su siguiente ciclo).
2. **Cómo se silencia: ganancia 0, no volumen del sistema.** `AlarmSoundEngine.setMuted(true)` pone la ganancia del `TonePlayer` a 0 (el bucle sigue en marcha, reactivar es instantáneo) y detiene vibrador y voz. No se toca el volumen del stream de alarma, así que `VolumeLock` sigue forzándolo al máximo y sus intentos de manipulación se cuentan igual. La rampa sigue avanzando en segundo plano.
3. **Interacción = toques o progreso.** `RingChallenges` observa los punteros en pasada `Initial` (sin consumirlos, de modo que no altera los juegos ni el anti-trampas) y `ChallengeSessionScreen` notifica cada respuesta enviada (cubre los retos por sensor, que no generan toques). Abrir la pantalla sin tocar NO silencia. Limitación: un reto de movimiento largo sin respuesta ni toques puede superar la inactividad y volver a sonar; es aceptable porque el valor por defecto es holgado y es el lado seguro.
4. **Máquina de estados pura `MuteStateMachine`.** Estados `RINGING` y `MUTED`. Entradas: `onInteraction()` (→ `MUTED`, refresca el instante) y `poll()` (si `MUTED` y han pasado ≥ `timeoutMillis` desde la última interacción → `RINGING`). `millisUntilRing()` indica cuánto falta. Recibe `Clock` por constructor; las entradas devuelven si cambió el estado. No conoce Android ni corrutinas.
5. **`ChallengeMuteController` (singleton)** une máquina, motor y tiempo. Mantiene una máquina por `sessionId` con la inactividad de su alarma; las interacciones de sesiones sin la opción activa se ignoran. Un único bucle de espera por sesión duerme hasta `millisUntilRing()` y consulta `poll()`; las interacciones intermedias solo actualizan el instante (sin reprogramar un temporizador por cada evento táctil). Es un `RingingLifecycleListener`: al terminar la sesión (resuelta, pospuesta o tiempo agotado) cancela el bucle y descarta la máquina. `AlarmSoundEngine.start` siempre arranca sin silenciar.
6. **Persistencia en columnas propias**, no en `soundJson`: `muteDuringChallenges INTEGER NOT NULL DEFAULT 0` y `muteInactivitySeconds INTEGER NOT NULL DEFAULT 30`. `Alarm.muteDuringChallenges: Boolean = false` y `Alarm.muteInactivitySeconds: Int = 30`, rango 10..300 validado en `init`. Migración explícita `MIGRATION_5_6` (dos `ALTER TABLE ... ADD COLUMN`). Los nombres específicos evitan colisiones con las columnas de v7/v8.
7. **Editor:** sección propia (`MuteSection`, `EditorSection` por multibinding) con un interruptor y un selector de inactividad (15 s, 30 s, 1 min, 2 min) visible solo si la opción está activa. Sigue el diseño de `design/` y la skill `design-taste-compose`.

## Risks / Trade-offs

- Conflicto previsible con los changes v7/v8: `ApexDatabase` (versión, lista de migraciones y `DataModule`), `AlarmEntity`, `Alarm`, `Mappers` y el editor. Se resuelve conservando todas las columnas y encadenando `MIGRATION_5_6`, `6_7`, `7_8`; hay que renumerar la mía si otro change llega antes a develop.
- Silenciar la alarma es la excepción buscada a «una alarma nunca suena en silencio»; es opt-in, acotada por la inactividad y por el tiempo máximo de sonado.
