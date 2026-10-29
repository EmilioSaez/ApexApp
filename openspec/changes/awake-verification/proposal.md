# Proposal

## Why

Apagar la alarma no garantiza que el usuario siga despierto: es habitual resolver los retos y volver a dormirse. Una opción por alarma que compruebe, pasados unos minutos, que el usuario sigue despierto cierra ese hueco con el mismo mecanismo de retos que ya usa la app.

## What Changes

- Nueva opción por alarma **«Verificar que estoy despierto»** (desactivada por defecto).
- Al resolver una alarma con la opción activa (retos completados o descartada), se programa con `AlarmManager.setAlarmClock` una verificación a los **5 min**: una notificación de importancia alta con la acción **«Estoy despierto»**.
- Si el usuario pulsa la acción dentro de **2 min**, la verificación termina y se cancela la notificación. Si no, la alarma **vuelve a sonar con todos sus retos** como una sesión nueva (disparo normal, con evento «sonó»).
- La verificación **se repite** tras cada resolución hasta que el usuario confirma.
- La verificación sobrevive a reinicio, cambio de hora y actualización de la app (se reprograma desde su estado persistido) y a Doze (`setAlarmClock`).
- Si la alarma se borra, se desactiva explícitamente o se le quita la opción entre medias, la verificación se cancela.
- Se registran los eventos «verificada» y «verificación fallida» en la sesión original del log de despertares.
- Columna nueva en `alarms` (migración Room explícita) y sección nueva en el editor.
- **Fuera de alcance**: plazos configurables por alarma, verificación en la alarma de prueba, cambios en la pantalla de sonar.

## Capabilities

### New Capabilities
- `awake-verification`: opción, planificación, notificación, confirmación y nuevo sonado de la verificación.

### Modified Capabilities
- `wake-event-log`: dos tipos de evento nuevos.
- `alarm-scheduling`: la reprogramación tras reinicio restaura también las verificaciones pendientes.
- `alarm-storage`: el esquema añade la columna de la opción.

## Impact

- Código: `domain/Alarm`, `data/local/AlarmEntity`, `ApexDatabase` (versión 7 + `MIGRATION_6_7`), `data/Mappers`, `DataModule`, `alarm/` (nuevo `AwakeVerifier`, receiver, gateway y notifier de verificación; `AlarmFiringHandler.ringAgain`; `AlarmController`, `AlarmReceiver`, `BootReceiver`), `domain/WakeEvents`, editor (nueva `EditorSection`), `AndroidManifest.xml`, strings es/en.
- **Numeración de BD**: el silencio durante los retos usa la versión 6 y el límite de posposiciones no añade migración, así que este change usa la **7** y `MIGRATION_6_7`.
