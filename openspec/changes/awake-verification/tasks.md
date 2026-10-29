## 1. Dominio y datos

- [ ] 1.1 `Alarm.awakeVerification`, `AlarmEntity.awakeVerificationEnabled`, mapeo (compartir queda fuera de alcance)
- [ ] 1.2 `ApexDatabase` versión 8, `MIGRATION_7_8`, registro en `DataModule`, esquema 8.json y test de migración
- [ ] 1.3 `WakeEventType.VERIFIED` y `VERIFICATION_FAILED`

## 2. Lógica y planificación

- [ ] 2.1 `AwakeVerificationPolicy` (plazos, restauración tras reinicio) con tests
- [ ] 2.2 `AwakeVerifier` + puertos (gateway, notifier, store) con tests: resolver, aviso, confirmar, plazo, repetición, borrar/desactivar, notificaciones bloqueadas, restauración
- [ ] 2.3 `AlarmFiringHandler.ringAgain` con tests
- [ ] 2.4 Integración en `AlarmController` (cancelar), `AlarmReceiver` (disparo regular cancela) y `BootReceiver` (restaurar)

## 3. Android

- [ ] 3.1 Gateway con `setAlarmClock`, `AwakeVerificationReceiver` (aviso/plazo/confirmar) y registro en el manifiesto
- [ ] 3.2 Notificación (canal `awake_verification`, acción «Estoy despierto») y bindings Hilt

## 4. UI

- [ ] 4.1 `AwakeVerificationEditorSection` según el diseño de `diseno-retos-mejoras`, strings es/en

## 5. Verificación

- [ ] 5.1 `./gradlew assembleDebug` y tests unitarios en verde
- [ ] 5.2 Prueba en emulador del flujo completo
