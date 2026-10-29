## 1. Dominio y datos

- [x] 1.1 `Alarm.awakeVerification`, `AlarmEntity.awakeVerificationEnabled`, mapeo (compartir queda fuera de alcance)
- [x] 1.2 `ApexDatabase` versión 7, `MIGRATION_6_7`, registro en `DataModule`, esquema 7.json
- [x] 1.2b Test instrumentado de migración 6→7 (escrito y compilado; sin ejecutar en emulador)
- [x] 1.3 `WakeEventType.VERIFIED` y `VERIFICATION_FAILED`

## 2. Lógica y planificación

- [x] 2.1 `AwakeVerificationPolicy` (plazos, restauración tras reinicio) con tests
- [x] 2.2 `AwakeVerifier` + puertos (gateway, notifier, store) con tests: resolver, aviso, confirmar, plazo, repetición, borrar/desactivar, notificaciones bloqueadas, restauración
- [x] 2.3 `AlarmFiringHandler.ringAgain` con tests
- [x] 2.4 Integración en `AlarmController` (cancelar), `AlarmReceiver` (disparo regular cancela) y `BootReceiver` (restaurar)

## 3. Android

- [x] 3.1 Gateway con `setAlarmClock`, `AwakeVerificationReceiver` (aviso/plazo/confirmar) y registro en el manifiesto
- [x] 3.2 Notificación (canal `awake_verification`, acción «Estoy despierto») y bindings Hilt

## 4. UI

- [x] 4.1 `AwakeVerificationEditorSection` según el diseño de `diseno-retos-mejoras`, strings es/en

## 5. Verificación

- [x] 5.1 `./gradlew assembleDebug` y tests unitarios en verde
- [x] 5.2 Arranque en emulador sin fallos (instala y abre)
- [ ] 5.3 Prueba manual del flujo completo (aviso, confirmar, nuevo sonado, reinicio)
