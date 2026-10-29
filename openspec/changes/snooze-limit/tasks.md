## 1. Dominio y tests

- [x] 1.1 `SnoozePolicy.UNLIMITED`, `remaining` y `canSnooze`; `SnoozeController.remaining`
- [x] 1.2 Tests: cuenta atrás, sin límite, 0, reinicio por sesión
- [x] 1.3 `RingUiState.snoozesLeft` y test del ViewModel
- [x] 1.4 Validador de alarmas compartidas admite `-1`

## 2. UI

- [ ] 2.1 Editor: control del máximo con «Sin límite» (0..10)
- [ ] 2.2 Pantalla de sonar: restantes junto al botón y estado «agotadas»
- [ ] 2.3 Resumen de importación con «sin límite»
- [ ] 2.4 Strings es/en

## 3. Verificación

- [ ] 3.1 `./gradlew assembleDebug` y tests unitarios
