## Context

`Alarm` solo tiene `soundId`; `Sound` admite `Resource` y `Uri`. `AlarmFiringHandler.onFire` llama a `AlarmNotifier` (solo notificación, sin audio). Room v1 con `exportSchema = true`. Otros changes en paralelo definen el host de "alarma sonando" (`alarm-core`, `alarm-reliability`) y el volumen forzado (`anti-cheat`); este change aporta el motor de audio, no el host. Decisiones aprobadas: `alarm-reliability` es dueño único del servicio en primer plano y de `AlarmRinger`; `alarm-core` define `SoundConfig` (`volume`, `vibrate`, `ttsEnabled`, `ttsText`) y fija Room v2; orden de merge: …alarm-core, alarm-reliability, challenge-engine, alarm-sound, anti-cheat….

## Goals / Non-Goals

**Goals:** audio fiable por stream de alarma; rampa; vibración; TTS; todo configurable por alarma; migración sin pérdida.
**Non-Goals:** volumen forzado/bloqueo de ajustes (anti-cheat), pantalla de retos, servicio en primer plano y fiabilidad del disparo, biblioteca/streaming de música, sonidos remotos.

## Decisions

1. **Motor enganchado a `AlarmRinger`.** `alarm-reliability` define `AlarmRinger { start(session: RingingSession); stop() }` con `RingingSession(sessionId: String, alarm: Alarm)`; el servicio llama `start` al empezar a sonar y `stop` al terminar y su servicio en primer plano, con `SystemToneRinger` por defecto. Este change aporta `SoundAlarmRinger`, que sustituye el binding Hilt y orquesta `ToneAudio`, `AlarmVibrator` y `AlarmSpeaker`. Sin servicio propio ni cambios en `AlarmFiringHandler`. La previsualización del editor usa una API `previewStart/Stop` del mismo motor.
2. **Reproducción con `MediaPlayer`**, `AudioAttributes(USAGE_ALARM, CONTENT_TYPE_MUSIC)`, `isLooping = true`, `setVolume` para la rampa y wake lock parcial. Rápido de preparar y soporta `content://` y raw. Se descarta ExoPlayer para no añadir dependencia.
3. **Audio focus:** `AUDIOFOCUS_GAIN` con `USAGE_ALARM`, sin delayed focus. Si se pierde, la alarma NO se pausa (el callback solo registra). Si se deniega, suena igualmente.
4. **Rampa:** `rampSeconds` (0 = sin rampa). Curva lineal de 0.05 a 1.0 sobre el volumen del `MediaPlayer` (el techo real lo marca el volumen del stream de alarma, que no se toca aquí). Se implementa con una corrutina ligada a la sesión, independiente del bucle del `MediaPlayer`, para no reiniciarse en cada vuelta.
5. **Respaldo:** cualquier excepción en `setDataSource/prepare` u `OnErrorListener` conmuta a `R.raw.default_alarm` (tono incluido). `Alarm.soundId == null` usa el tono por defecto.
6. **Fuentes:** `SoundSource` gana `SystemTone(uri)` (tonos de `RingtoneManager`); `Uri` sigue para SAF. El archivo se elige con `ActivityResultContracts.OpenDocument` (`audio/*`) y se llama a `takePersistableUriPermission(READ)`. Se verifica legibilidad al guardar y al sonar. El nombre visible sale de `OpenableColumns.DISPLAY_NAME`.
7. **Modelo: `SoundConfig` de alarm-core (esquema cerrado con alarm-core-43).** `Alarm.soundConfig = SoundConfig(volume: Int = 100, vibrate: Boolean = true, ttsEnabled: Boolean = false, ttsText: String? = null, rampSeconds: Int = 30, vibrationPattern: String = "steady", speakTime: Boolean = false, speakRepeatSeconds: Int = 0)`. alarm-core lo persiste entero en UNA columna `soundJson TEXT NOT NULL DEFAULT '{}'` (kotlinx.serialization, `ignoreUnknownKeys`, defaults al decodificar) que crea en Room v2. Este change **no añade columnas ni sube la versión de Room**: solo añade propiedades con default a `SoundConfig` y valida rangos.
   - `volume` (0..100) se usa solo como techo de ganancia del reproductor; el forzado es de anti-cheat. `ttsText` (máx. 140 caracteres) es el recordatorio hablado.
   - `rampSeconds` 0..120 (0 = sin rampa); `speakRepeatSeconds` 0 = una vez, si > 0 mínimo 15.
   - `vibrationPattern`: id de preset (`"steady"`, `"pulse"`, `"sos"`…) o personalizado `"c:200,100,400"` (ms, alterna pulso/pausa; máx. 20 elementos, 10 s por ciclo).
   - **Clave estable de sonido:** no va en `SoundConfig`. Vive en el sonido: `SoundSource.Bundled(key)` (p. ej. `"default"`) y los tipos `system`/`bundled` en `SoundEntity.sourceType` (`String`, sin cambio de esquema). `BundledSounds.resolve(key)` devuelve el sonido, o el por defecto si la clave es desconocida. Las URIs externas (SAF/sistema) no son portables y no se exportan.

   Validación de rangos en `init` de `SoundConfig` (lado de este change).
8. **Vibración:** `VibrationEffect.createWaveform(timings, 0)` con atributos de alarma; `VibratorManager` en API 31+ y `Vibrator` antes; permiso `VIBRATE`; sin vibrador, no-op.
9. **TTS:** `TextToSpeech` con `AudioAttributes USAGE_ALARM`; la hora se formatea según `DateFormat.is24HourFormat`. Mientras habla se baja el `MediaPlayer` a ~0.3 y se restaura en `onDone`. Si `onInit` falla o el idioma no está soportado, se omite sin error. La repetición usa un `delay` en la corrutina de sesión.
10. **Editor:** nueva sección "Sonido" en `AlarmEditorScreen` con selector de fuente, slider de rampa, vibración (preset/personalizado) y voz; botón de probar que usa `previewStart/Stop` y se detiene en `onDispose`. Se aplica la skill `design-taste-compose`.

## Risks / Trade-offs

- **Doze/OEM:** el audio depende de que el proceso esté vivo al disparar; lo mitiga el host de alarm-reliability/alarm-core. Aquí se toma wake lock parcial durante la reproducción.
- **Volumen de alarma a 0:** no se corrige aquí (anti-cheat). Decisión abierta: avisar en el editor si el volumen de alarma es 0.
- **URIs SAF revocadas** (archivo movido o app de origen desinstalada): respaldo al tono por defecto; límite práctico de permisos persistentes por app.
- **Conflicto de migración/modelo** con alarm-core y los changes que se fusionan antes: al rebasar se usa la siguiente versión de Room libre y se reconcilia `SoundConfig`.
- **TTS** depende del motor instalado y puede tardar en iniciar: el sonido arranca sin esperar a la voz.

## Migration Plan

Sin migración propia: los ajustes viven en `soundJson`, que crea alarm-core en Room v2; los campos nuevos se leen con defaults.

## Open Questions

- ¿Rampa propuesta por el editor de 30 s o desactivada? ¿Se avisa en el editor si el volumen de alarma es 0 (el forzado es de anti-cheat)?
- ¿Varios sonidos por alarma (playlist/aleatorio) o solo uno?
- ¿`ttsText` por alarma basta, o se necesitan plantillas (p. ej. incluir la etiqueta)?
