# Proposal

## Why

Hoy la alarma solo publica una notificación: no suena ni vibra. ApexApp necesita que despertar sea inevitable, así que hace falta un sistema de audio propio, fiable y configurable por alarma (tono o música, volumen progresivo, vibración y voz).

## What Changes

- Reproducción del sonido de la alarma por el stream de alarma (`USAGE_ALARM`) con audio focus, en bucle, que sobrevive al apagado de pantalla y a otras apps de audio.
- Fuentes de sonido: tonos del sistema (`RingtoneManager`), archivos locales elegidos con SAF (con permiso persistente) y un tono por defecto incluido en la app como respaldo.
- Volumen progresivo (rampa) configurable por alarma.
- Patrones de vibración predefinidos y personalizados por alarma.
- Voz/TTS: leer la hora y un recordatorio de texto libre al sonar, con repetición opcional.
- Ajustes de sonido, vibración y voz en el editor de alarmas, con previsualización.
- Se amplía el `SoundConfig` de `alarm-core` (volumen, vibración, TTS) con rampa, patrón de vibración, lectura de hora y repetición de voz; se persiste como JSON en su columna `soundJson` (Room v2): sin columnas ni migración propias.
- El motor de audio se engancha a `AlarmRinger` (propiedad de `alarm-reliability`, dueño único del servicio en primer plano); este change no crea servicio propio.
- Fuera de alcance: volumen forzado/anti-trampas (`anti-cheat`), pantalla de retos y servicio de alarma sonando (`alarm-core`/`challenge-engine`), fiabilidad del disparo (`alarm-reliability`), tema visual (`design-system-stitch`).

## Capabilities

### New Capabilities
- `alarm-sound-playback`: reproducción fiable del audio de la alarma (fuentes, stream de alarma, audio focus, bucle, rampa de volumen, respaldo ante fallo).
- `alarm-vibration`: patrones de vibración predefinidos y personalizados al sonar.
- `alarm-voice`: lectura por TTS de la hora y de un recordatorio al sonar.
- `alarm-sound-settings`: configuración por alarma de sonido, rampa, vibración y voz en el editor, incluida la selección SAF/sistema y la previsualización.

### Modified Capabilities
- `alarm-storage`: `SoundConfig` se amplía con rampa, patrón de vibración y ajustes de voz; los sonidos admiten tonos del sistema; el esquema se migra sin perder datos.

## Impact

- Código: `domain/Alarm.kt`, `domain/Sound.kt`, `data/local/*` (mapper de `soundJson` y `SoundEntity`), `Mappers.kt`, binding Hilt de `AlarmRinger`, nuevo paquete `alarm/sound` (reproductor, vibración, TTS), `ui/editor/AlarmEditorScreen.kt`, `AndroidManifest.xml` (`VIBRATE`, consulta de motor TTS).
- Sin dependencias de red; todo local. Sin librerías nuevas previstas (MediaPlayer/`Vibrator`/`TextToSpeech` de la plataforma).
- Dependencias de merge (orden aprobado: … alarm-core → alarm-reliability → challenge-engine → **alarm-sound** → anti-cheat …): requiere `SoundConfig` y Room v2 (con `soundJson`) de `alarm-core` y la interfaz `AlarmRinger` de `alarm-reliability`. Al integrar se rebasa.
- El volumen forzado queda en `anti-cheat`; aquí `volume` solo limita la ganancia de reproducción.
