## ADDED Requirements

### Requirement: Opción por alarma
El sistema SHALL ofrecer en cada alarma la opción «Verificar que estoy despierto», desactivada por defecto, que se guarda con el resto de la configuración de la alarma.

#### Scenario: Valor por defecto
- **WHEN** se crea una alarma nueva
- **THEN** la verificación está desactivada

### Requirement: Verificación tras resolver la alarma
El sistema SHALL, cuando una alarma con la opción activa se resuelve (retos completados o descartada), programar con `AlarmManager.setAlarmClock` una verificación 5 minutos después, y SHALL NOT hacerlo si la sesión terminó por snooze o por tiempo agotado, ni para la alarma de prueba.

#### Scenario: Alarma resuelta
- **WHEN** el usuario completa los retos de una alarma con la opción activa
- **THEN** se programa una verificación 5 minutos después

#### Scenario: Opción desactivada
- **WHEN** se resuelve una alarma sin la opción
- **THEN** no se programa ninguna verificación

#### Scenario: Snooze
- **WHEN** el usuario pospone la alarma
- **THEN** no se programa verificación

### Requirement: Notificación y confirmación
Al vencer la verificación el sistema SHALL mostrar una notificación de importancia alta con la acción «Estoy despierto» y SHALL conceder 2 minutos para pulsarla. Al confirmar, el sistema SHALL cancelar el plazo y la notificación y terminar la verificación.

#### Scenario: Confirmación a tiempo
- **WHEN** el usuario pulsa «Estoy despierto» dentro del plazo
- **THEN** la notificación desaparece, la alarma no vuelve a sonar y la verificación termina

#### Scenario: Notificaciones bloqueadas
- **WHEN** vence la verificación y las notificaciones están bloqueadas
- **THEN** la verificación se omite sin hacer sonar la alarma

### Requirement: Nuevo sonado
Si el plazo vence sin confirmar, el sistema SHALL cancelar la notificación y hacer sonar la alarma de nuevo en una sesión nueva con todos los retos configurados, sin reprogramar ni desactivar la alarma. La verificación SHALL repetirse al resolver esa sesión, hasta que el usuario confirme.

#### Scenario: Plazo agotado
- **WHEN** pasan 2 minutos sin pulsar «Estoy despierto»
- **THEN** la alarma suena de nuevo con todos sus retos como una sesión nueva

#### Scenario: Alarma puntual
- **WHEN** la alarma era puntual (ya desactivada al sonar) y vence el plazo
- **THEN** suena igualmente y sigue desactivada

#### Scenario: Repetición
- **WHEN** el usuario resuelve la sesión del nuevo sonado
- **THEN** se programa otra verificación a los 5 minutos

### Requirement: Cancelación y supervivencia
El sistema SHALL cancelar la verificación pendiente (programación y notificación) cuando la alarma se borra, se desactiva explícitamente, se le quita la opción o vuelve a dispararse por su horario, y SHALL restaurarla tras reinicio, cambio de hora o actualización de la app. Una verificación vencida hace más de 15 minutos al restaurar SHALL descartarse.

#### Scenario: Alarma borrada
- **WHEN** se borra la alarma con una verificación pendiente
- **THEN** no se muestra notificación ni vuelve a sonar

#### Scenario: Reinicio
- **WHEN** el dispositivo se reinicia con una verificación pendiente
- **THEN** la verificación se reprograma desde su estado guardado
