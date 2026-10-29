# Tasks

## 1. Datos

- [x] 1.1 `Alarm.muteDuringChallenges` y `muteInactivitySeconds` (10..300) y columnas en `AlarmEntity`; mapeo en `Mappers`; verificar con test de ida y vuelta.
- [x] 1.2 `ApexDatabase` v6 con `MIGRATION_5_6` registrada en `DataModule`, esquema `6.json` y test de migración.

## 2. Motor y lógica

- [x] 2.1 `MuteStateMachine` pura con reloj inyectado; verificar con tests (silencio, reinicio de cuenta, reactivación, reciclado).
- [x] 2.2 `AlarmSoundEngine.setMuted` (tono, vibración y voz); verificar con tests de motor.
- [x] 2.3 `ChallengeMuteController` (bucle de inactividad, descarte al terminar la sesión); verificar con tests y reloj de corrutinas.

## 3. Pantalla de sonar

- [x] 3.1 Señal de interacción: punteros en `RingChallenges` y respuestas en `ChallengeSessionScreen`, vía `RingViewModel`.

## 4. Editor

- [x] 4.1 `MuteSection` con interruptor y selector de inactividad, strings es/en, siguiendo `design/` y `design-taste-compose`.

## 5. Verificación

- [ ] 5.1 `./gradlew assembleDebug` y `./gradlew test` en verde; prueba manual en emulador.
