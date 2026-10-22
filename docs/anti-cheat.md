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
Si el dispositivo se queda atrapado, por ADB:

```
adb shell am force-stop app.luxion.apexapp
adb shell dpm remove-active-admin app.luxion.apexapp/.anticheat.ApexDeviceAdminReceiver
```

El segundo comando quita también el propietario del dispositivo en apps de depuración (`debuggable`);
en una release hay que restablecer de fábrica para retirarlo.

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

## Pruebas manuales en emulador (pendientes de ejecutar)

Estado: pendientes de ejecutar, no se pudo reservar emulador (todos en uso). Checklist:

1. `OFF`: la alarma suena, Atrás y Home salen sin restricción.
2. `STANDARD` sin superposición: Atrás no cierra; Home sale pero la notificación reabre la pantalla.
3. `STANDARD` con superposición: Home vuelve a la pantalla en menos de un segundo.
4. Volumen: bajar con la tecla no cambia el volumen; al terminar se restaura el anterior.
5. `STRICT` sin propietario: se comporta como `STANDARD` y la pantalla de estado lo indica.
6. `STRICT` con propietario (`dpm set-device-owner`): Home, Recientes y menú de apagado no responden; se liberan al resolver, posponer o agotar el tiempo.
7. Snooze: aparece el botón solo si la política lo permite; la alarma vuelve a sonar a los minutos configurados con el mismo `sessionId`; tras el máximo no se ofrece.
8. Matar el proceso durante el sonado (`adb shell am force-stop app.luxion.apexapp`) y comprobar que se emite `SUSPICIOUS` al arrancar de nuevo.
