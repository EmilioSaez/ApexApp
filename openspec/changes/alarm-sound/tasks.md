## 1. Modelo y persistencia

- [x] 1.1 Añadir a `SoundConfig` (alarm-core) `speakTime` y los campos `rampSeconds`, `vibrationPattern`, `speakRepeatSeconds` con defaults si aún no están, y la validación de rangos en `init`
- [x] 1.2 Añadir `SoundSource.SystemTone`/`Bundled(key)`, `BundledSounds.resolve(key)` (desconocida → por defecto) y los tipos `system`/`bundled` en `SoundEntity`/`Mappers.kt`
- [x] 1.3 Tras rebasar sobre alarm-core, comprobar que `soundJson` decodifica los campos nuevos (sin columnas ni migración propias)
- [x] 1.4 Test de decodificación de `soundJson` con claves ausentes o desconocidas (defaults) y de alarmas existentes de v2
- [ ] 1.5 Tests de mappers y repositorio para los campos nuevos y los valores por defecto

## 2. Motor de audio

- [ ] 2.1 Crear `SoundAlarmRinger` (implementa `AlarmRinger.start(session: RingingSession)/stop()` de alarm-reliability, reemplaza el binding Hilt de `SystemToneRinger`) con `MediaPlayer` (`USAGE_ALARM`, bucle, wake lock parcial)
- [x] 2.2 Añadir tono por defecto en `res/raw` y respaldo ante fallo de fuente o error del reproductor
- [x] 2.3 Gestionar audio focus (solicitar, ignorar pérdida, liberar)
- [x] 2.4 Implementar la rampa de volumen sin reinicio con el bucle
- [x] 2.5 Resolver fuentes: recurso, tono del sistema y URI SAF con comprobación de legibilidad
- [x] 2.6 Tests unitarios de la curva de rampa y de la lógica de respaldo con fakes

## 3. Vibración

- [x] 3.1 Definir presets y parser/validador de patrón personalizado (límites de longitud y duración)
- [x] 3.2 Implementar `AlarmVibrator` (API 29+, atributos de alarma, no-op sin vibrador) y permiso `VIBRATE`
- [x] 3.3 Tests unitarios del parser y validador

## 4. Voz (TTS)

- [x] 4.1 Implementar `AlarmSpeaker` con `TextToSpeech` en `USAGE_ALARM` y formato de hora 12/24 h
- [x] 4.2 Añadir ducking del tono mientras habla y repetición por intervalo
- [x] 4.3 Manejar fallo de init o de idioma sin afectar al resto
- [x] 4.4 Tests con fake del motor TTS

## 5. Integración en el disparo

- [ ] 5.1 Verificar que el servicio de alarm-reliability arranca/para `SoundAlarmRinger` por `AlarmRinger.start/stop` y cubrirlo con tests (sin servicio propio)
- [ ] 5.2 Detener audio, vibración y voz al parar o posponer la alarma

## 6. Editor

- [x] 6.1 Selector de sonido: por defecto, incluidos en la app, tonos del sistema y `OpenDocument` con `takePersistableUriPermission`
- [ ] 6.2 Controles de rampa, vibración (preset/personalizado) y voz en `AlarmEditorScreen` siguiendo `design-taste-compose`
- [x] 6.3 Botón de previsualización con parada al salir de la pantalla
- [x] 6.4 Strings en `strings.xml` y tests de UI/ViewModel del editor

## 7. Verificación

- [ ] 7.1 Prueba instrumentada: la alarma suena por el stream de alarma con la pantalla apagada
- [ ] 7.2 Prueba manual: audio de otra app en curso, archivo SAF borrado y TTS ausente
- [ ] 7.3 `openspec validate alarm-sound`
