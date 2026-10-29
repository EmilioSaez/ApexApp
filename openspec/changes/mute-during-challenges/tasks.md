# Tasks

## 1. Datos

- [ ] 1.1 `Alarm.muteDuringChallenges` y `muteInactivitySeconds` (10..300) y columnas en `AlarmEntity`; mapeo en `Mappers`; verificar con test de ida y vuelta.
- [ ] 1.2 `ApexDatabase` v6 con `MIGRATION_5_6` registrada en `DataModule`, esquema `6.json` y test de migración.

## 2. Motor y lógica

- [ ] 2.1 `MuteStateMachine` pura con reloj inyectado; verificar con tests (silencio, reinicio de cuenta, reactivación, reciclado).
- [ ] 2.2 `AlarmSoundEngine.setMuted` (tono, vibración y voz); verificar con tests de motor.
- [ ] 2.3 `ChallengeMuteController` (bucle de inactividad, descarte al terminar la sesión); verificar con tests y reloj de corrutinas.

## 3. Pantalla de sonar

- [ ] 3.1 Señal de interacción: punteros en `RingChallenges` y respuestas en `ChallengeSessionScreen`, vía `RingViewModel`.

## 4. Editor

- [ ] 4.1 `MuteSection` con interruptor y selector de inactividad, strings es/en, siguiendo `design/` y `design-taste-compose`.

## 5. Verificación

- [ ] 5.1 `./gradlew assembleDebug` y `./gradlew test` en verde; prueba manual en emulador.
