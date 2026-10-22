# alarm-scheduling Specification

## Purpose
Define cómo la app hace que una alarma activa suene de forma fiable a su hora exacta, incluidos los permisos necesarios y la recuperación tras reiniciar el dispositivo.

## Requirements

### Requirement: Programación de alarmas activas
El sistema SHALL programar una alarma exacta para la próxima ocurrencia de cada alarma activa y SHALL cancelar la programación cuando la alarma se desactiva o se elimina.

#### Scenario: Activar una alarma
- **WHEN** se activa una alarma
- **THEN** queda programada para su próxima ocurrencia según hora y días de repetición

#### Scenario: Desactivar o borrar
- **WHEN** se desactiva o elimina una alarma programada
- **THEN** su programación se cancela y no se dispara

### Requirement: Disparo de la alarma
El sistema SHALL notificar al usuario cuando llega la hora de una alarma activa, incluso con la app cerrada, y SHALL reprogramar las alarmas repetitivas para su siguiente ocurrencia.

#### Scenario: Alarma puntual
- **WHEN** llega la hora de una alarma sin repetición
- **THEN** se notifica al usuario y la alarma pasa a inactiva

#### Scenario: Alarma repetitiva
- **WHEN** llega la hora de una alarma con días de repetición
- **THEN** se notifica al usuario y queda programada la siguiente ocurrencia

### Requirement: Recuperación tras reinicio
El sistema SHALL reprogramar todas las alarmas activas cuando el dispositivo se reinicia.

#### Scenario: Reinicio del dispositivo
- **WHEN** el dispositivo arranca
- **THEN** todas las alarmas activas vuelven a estar programadas

### Requirement: Permisos de alarmas y notificaciones
El sistema SHALL comprobar los permisos de alarmas exactas y de notificaciones, y SHALL informar al usuario y guiarle a concederlos cuando falten.

#### Scenario: Permiso de alarmas exactas denegado
- **WHEN** el usuario activa una alarma sin permiso de alarmas exactas
- **THEN** la app indica que falta el permiso y ofrece abrir el ajuste correspondiente

#### Scenario: Permiso de notificaciones denegado
- **WHEN** el permiso de notificaciones no está concedido
- **THEN** la app lo solicita o avisa de que la alarma podría no mostrarse

### Requirement: Registro del disparo
El sistema SHALL registrar un evento «sonó» con el instante programado y el instante real cada vez que se dispara una alarma activa, sin retrasar ni impedir la notificación si el registro falla.

#### Scenario: Disparo registrado
- **WHEN** llega la hora de una alarma activa
- **THEN** se notifica al usuario y se registra un evento «sonó»

#### Scenario: Fallo al registrar
- **WHEN** el registro del evento falla
- **THEN** la alarma se notifica y se reprograma igualmente
