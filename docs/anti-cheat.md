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

Ejecutadas en emuladores API 36 (debug). Las alarmas se crean con `DebugAlarmReceiver`:
`--es level OFF|STANDARD|STRICT --ei snoozeMinutes N --ei snoozeMax M`; `--ez removeAdmin true` desactiva el
administrador de dispositivo (limpieza). El volumen de alarma de partida se baja con `adb shell cmd audio set-volume 4 3`.

Verificadas:

| Caso | Resultado |
| --- | --- |
| `OFF`, dispositivo en uso | Suena sin abrir la pantalla; volumen intacto; Atrás y Home libres. |
| `STANDARD` con otra app delante y superposición | La pantalla de sonar pasa sola al primer plano en modo inmersivo; Atrás ignorado; Home vuelve en ~1 s. |
| `STANDARD`, pantalla apagada | La pantalla se enciende y muestra la pantalla de sonar. |
| `STANDARD`, pantalla apagada con PIN | La pantalla de sonar queda sobre el bloqueo (`isKeyguardShowing=true`); Atrás ignorado. |
| `STANDARD` sin superposición | La pantalla no se abre sola; abierta desde la notificación, Atrás se ignora y Home sale y no vuelve; el volumen se fuerza igualmente. |
| Volumen | Con volumen de partida 3: sube a 7 al empezar, las teclas de bajar no lo mueven y vuelve a 3 al terminar. |
| Snooze | Libera el lockdown, re-dispara al minuto con la misma sesión y respeta `maxCount`. |
| Matar el proceso durante el sonado | El sonado se detiene (nada lo reanuda); la marca de sesión viva se consume al arrancar la app (`SUSPICIOUS`). |
| `STRICT` sin propietario | Se comporta como `STANDARD` (sin lock task). |
| `STRICT` con propietario (`dpm set-device-owner`) | Lock task `LOCKED`: Home, Recientes, panel y encendido no responden; se libera al resolver, al posponer y por tiempo máximo (10 min); el volumen se restaura. |
| `alarm-sound` | Con el lockdown activo el `MediaPlayer` suena en `USAGE_ALARM` con el stream al máximo (7/7). |
| Administrador de dispositivo | Con el administrador activo, `pm uninstall` falla con `DELETE_FAILED_DEVICE_POLICY_MANAGER`. |
| Botón «Renunciar a propietario» | En el emulador con propietario real (`dpm`), deja `dpm list-owners` vacío y la pantalla de estado pasa a «Pendiente». |

Hallazgos:

- Con el dispositivo en uso, el full-screen intent solo muestra un aviso; por eso, con superposición, el lockdown abre
  la pantalla de sonar directamente. Sin superposición el lockdown solo se aplica cuando el usuario abre la pantalla.
- Matar el proceso (por ejemplo, Forzar detención en Ajustes) silencia la alarma: el servicio no se reanuda. Mitigarlo
  exige que `alarm-reliability` reanude el sonado o, con propietario del dispositivo, impedir la detención forzada
  (`setUserControlDisabledPackages`); no está implementado.
- `adb shell dpm remove-active-admin` falla con esta app; usa el botón de la pantalla de estado.

Pendiente para el usuario: probar el lock task y el botón de renuncia en un dispositivo físico, y activar el
administrador con el diálogo real del sistema.
