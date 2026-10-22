## MODIFIED Requirements

### Requirement: Disparo de la alarma
El sistema SHALL iniciar el sonado de la alarma mediante el servicio en primer plano cuando llega la hora de una alarma activa, incluso con la app cerrada, y SHALL reprogramar las alarmas repetitivas para su siguiente ocurrencia.

#### Scenario: Alarma puntual
- **WHEN** llega la hora de una alarma sin repetición
- **THEN** se inicia el sonado y la alarma pasa a inactiva

#### Scenario: Alarma repetitiva
- **WHEN** llega la hora de una alarma con días de repetición
- **THEN** se inicia el sonado y queda programada la siguiente ocurrencia

#### Scenario: App cerrada
- **WHEN** llega la hora de una alarma y la app no está en ejecución
- **THEN** el sonado se inicia igualmente

### Requirement: Recuperación tras reinicio
El sistema SHALL reprogramar todas las alarmas activas cuando el dispositivo se reinicia, cuando cambian la hora o la zona horaria del sistema, cuando la app se actualiza y cuando se concede el permiso de alarmas exactas.

#### Scenario: Reinicio del dispositivo
- **WHEN** el dispositivo arranca
- **THEN** todas las alarmas activas vuelven a estar programadas

#### Scenario: Cambio de hora o zona
- **WHEN** el usuario cambia la hora del sistema o la zona horaria
- **THEN** las alarmas activas se reprograman según la nueva hora local

#### Scenario: Actualización de la app
- **WHEN** la app se actualiza a una nueva versión
- **THEN** las alarmas activas se reprograman

#### Scenario: Permiso concedido después
- **WHEN** el usuario concede el permiso de alarmas exactas tras haber activado alarmas
- **THEN** las alarmas activas se programan sin que el usuario las reactive

### Requirement: Permisos de alarmas y notificaciones
El sistema SHALL declarar y comprobar los permisos de alarmas exactas (`USE_EXACT_ALARM` desde API 33, `SCHEDULE_EXACT_ALARM` en versiones anteriores) y de notificaciones, y SHALL informar al usuario y guiarle a concederlos cuando falten.

#### Scenario: Permiso de alarmas exactas denegado
- **WHEN** el usuario activa una alarma sin permiso de alarmas exactas
- **THEN** la app indica que falta el permiso y ofrece abrir el ajuste correspondiente

#### Scenario: Permiso de notificaciones denegado
- **WHEN** el permiso de notificaciones no está concedido
- **THEN** la app lo solicita o avisa de que la alarma podría no mostrarse

#### Scenario: Permiso revocado
- **WHEN** el sistema revoca el permiso de alarmas exactas con alarmas activas
- **THEN** la app avisa al abrirse de que las alarmas pueden no sonar a su hora

## ADDED Requirements

### Requirement: Alarmas perdidas
El sistema SHALL detectar al recuperarse (arranque o reprogramación) las alarmas cuya hora pasó mientras no estaban programadas, y SHALL hacerlas sonar de inmediato si no ha pasado más de una ventana de tolerancia, o registrarlas como perdidas, notificándolo al registro de eventos de despertar, en caso contrario.

#### Scenario: Dentro de la ventana
- **WHEN** el dispositivo arranca y una alarma activa debió sonar hace menos de la ventana de tolerancia
- **THEN** la alarma suena de inmediato

#### Scenario: Fuera de la ventana
- **WHEN** una alarma activa debió sonar hace más de la ventana de tolerancia
- **THEN** no suena, se registra como perdida en el registro de eventos de despertar y, si es repetitiva, se reprograma su siguiente ocurrencia, o si es puntual pasa a inactiva
