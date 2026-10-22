# Anti-trampas

Medidas para que una alarma no se pueda abandonar sin resolverla. Se configuran por alarma en el
editor (nivel y snooze) y se aplican mientras la alarma suena.

## Niveles

| Nivel | Qué hace |
| --- | --- |
| `OFF` | Sin restricciones: la alarma se descarta como siempre. |
| `STANDARD` | Pantalla de sonar en modo inmersivo, Atrás bloqueado, volumen de alarma forzado al máximo y recuperación de la pantalla si se pulsa Home (necesita permiso de superposición). |
| `STRICT` | Lo de `STANDARD` y, si la app es propietaria del dispositivo, lock task: Home, Recientes, panel de notificaciones y menú de apagado dejan de responder. Sin propietario funciona como `STANDARD` y la pantalla de estado lo indica. |

Una alarma sin política guardada se comporta como `OFF`. Las alarmas nuevas se crean en `STANDARD`.

## Permisos y capacidades

- **Superposición** (`SYSTEM_ALERT_WINDOW`): permite relanzar la pantalla de sonar desde segundo plano.
  Se concede desde la pantalla de estado (Ajustes del sistema).
- **Administrador de dispositivo**: impide desinstalar la app directamente mientras esté activo.
  Se activa desde la pantalla de estado. Para desinstalar, desactívalo antes en Ajustes > Seguridad.
- **Propietario del dispositivo**: necesario para el lock task de `STRICT`. Solo se concede por ADB.

La pantalla de estado (editor de alarma > Ver estado de permisos) muestra cada capacidad y sus límites.
Estos permisos no son aptos para Google Play; la app se distribuye por APK directo.

## Provisionar el propietario del dispositivo

Requisitos: dispositivo recién restablecido o **sin cuentas** (Google, de fabricante, etc.) y depuración USB.

```
adb shell dpm set-device-owner app.luxion.apexapp/.anticheat.ApexDeviceAdminReceiver
```

Si falla con `Not allowed to set the device owner because there are already some accounts`, elimina las
cuentas del dispositivo y repite.

## Salir si algo falla (rescate)

El lock task se libera siempre al terminar el sonado (resuelta, snooze o tiempo máximo de 10 minutos).
Si el dispositivo se queda atrapado:

```
adb shell am force-stop app.luxion.apexapp
```

Para dejar de ser propietario del dispositivo, usa el botón **Renunciar a propietario del dispositivo** de la
pantalla de estado. Nota: `adb shell dpm remove-active-admin` **no** funciona con esta app (no es `testOnly`)
y lanza una `SecurityException`; sin el botón solo queda restablecer de fábrica.

## Snooze

Desactivado por defecto. Por alarma: permitido, minutos (1 a 30) y máximo de veces (1 a 10). Posponer
detiene el sonado y lo reanuda pasados los minutos **dentro de la misma sesión de despertar** (mismo
`sessionId`), emite el evento `SNOOZED` y libera el lockdown hasta el siguiente sonado.

## Sesiones sospechosas

Se emite `SUSPICIOUS` a `wake-stats` (y `adaptive-difficulty` las excluye del ajuste) cuando una sesión:

- abandona la pantalla de sonar 3 o más veces durante el lockdown;
- baja el volumen 3 o más veces;
- se interrumpe por muerte del proceso o reinicio;
- termina por tiempo máximo sin resolverse.

Un `STRICT` degradado a `STANDARD` por falta de propietario **no** cuenta como sospechoso. Los motivos se
calculan pero no se persisten hasta que `wake-stats` ofrezca un campo para ellos.

## Límites que no se pueden superar

- Apagado forzado por hardware, modo seguro, recuperación, ADB y retirar la batería.
- Sin propietario del dispositivo, Home y Recientes no se pueden bloquear; solo se recupera la pantalla.
- El volumen forzado no controla salidas externas con volumen propio (algunos auriculares Bluetooth).
- Android 12+ restringe los toques sobre overlays; por eso no se usa el overlay como pantalla de bloqueo.
- Android 14+ puede revocar el permiso de pantalla completa; entonces queda la notificación del sonado.

## Pruebas manuales en emulador

Ejecutadas en un emulador API 36 (debug, alarmas creadas con `DebugAlarmReceiver`, que acepta
`--es level OFF|STANDARD|STRICT --ei snoozeMinutes N --ei snoozeMax M`).

Verificadas:

- `STANDARD` con otra app en primer plano y superposición concedida: la pantalla de sonar pasa sola al primer plano en modo inmersivo.
- Atrás no la cierra; Home vuelve a la pantalla de sonar en cerca de 1 segundo.
- Tres pulsaciones de bajar volumen no cambian el volumen de alarma (7/7).
- Snooze: aparece el botón; al pulsarlo se libera el lockdown y se detiene el servicio; la alarma se re-dispara al minuto con la misma sesión; con `maxCount = 1` ya no se ofrece de nuevo.
- Matar el proceso durante el sonado deja la marca de sesión viva; al arrancar la app se consume (se emite `SUSPICIOUS`).
- `STRICT` con propietario del dispositivo (`dpm set-device-owner`): lock task en estado `LOCKED`; Home, Recientes, panel de notificaciones y pulsación larga de encendido no responden; al posponer pasa a `NONE` y Home vuelve a funcionar.

Pendientes de verificar:

1. Pantalla bloqueada y apagada: la pantalla de sonar se enciende sobre el bloqueo.
2. `OFF`: la alarma se descarta sin restricciones.
3. Sin superposición: Atrás no cierra; Home sale y solo la notificación reabre la pantalla.
4. Restauración del volumen previo al terminar (el volumen de partida no se pudo bajar en el emulador).
5. Reaplicación del lockdown al recrear la actividad tras matar el proceso.
6. Liberación del lock task al resolver y por tiempo máximo (solo se probó por snooze).
7. `STRICT` sin propietario: se comporta como `STANDARD` y la pantalla de estado lo indica.
8. Botón «Renunciar a propietario del dispositivo» (solo compilado).
9. Integración con `alarm-sound` (rampa hasta el máximo con lockdown).

Hallazgo: con el dispositivo en uso, el full-screen intent solo muestra un aviso; por eso, con superposición, el
lockdown abre la pantalla de sonar directamente. Sin superposición el lockdown solo se aplica cuando el usuario
abre la pantalla.
