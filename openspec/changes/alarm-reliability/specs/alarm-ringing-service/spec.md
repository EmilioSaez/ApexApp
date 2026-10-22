## ADDED Requirements

### Requirement: Servicio en primer plano al sonar
El sistema SHALL arrancar un servicio en primer plano con notificación persistente cuando se dispara una alarma, y SHALL mantenerlo activo hasta que la alarma se resuelva o se agote su tiempo máximo.

#### Scenario: Disparo con la app cerrada
- **WHEN** llega la hora de una alarma activa y la app no está en ejecución
- **THEN** se inicia el servicio en primer plano y la notificación de alarma queda visible mientras la alarma suena

#### Scenario: Proceso presionado por el sistema
- **WHEN** el sistema libera memoria mientras suena una alarma
- **THEN** el proceso no se termina por ser un servicio en primer plano

#### Scenario: Tiempo máximo agotado
- **WHEN** la alarma lleva sonando más del tiempo máximo sin resolverse
- **THEN** el servicio se detiene y la alarma queda registrada como no atendida

### Requirement: Notificación de pantalla completa
El sistema SHALL mostrar la notificación de alarma con intent de pantalla completa para abrir la pantalla de sonar sobre el bloqueo, y SHALL degradar a notificación heads-up si el permiso de pantalla completa no está concedido.

#### Scenario: Dispositivo bloqueado
- **WHEN** suena una alarma con el dispositivo bloqueado y el permiso de pantalla completa concedido
- **THEN** se abre la pantalla de sonar sobre el bloqueo y se enciende la pantalla

#### Scenario: Sin permiso de pantalla completa
- **WHEN** suena una alarma sin permiso de pantalla completa
- **THEN** se muestra una notificación heads-up de máxima prioridad que abre la pantalla de sonar al tocarla

### Requirement: Contrato de enganche del sonado
El sistema SHALL tener un único servicio de sonado y SHALL exponer puntos de extensión estables para el audio (`AlarmRinger`), la pantalla de sonar y la resolución de la alarma, de modo que otros módulos los usen sin crear servicios propios ni modificar su ciclo de vida.

#### Scenario: Módulo de sonido conectado
- **WHEN** existe una implementación de reproducción de sonido registrada
- **THEN** el servicio la inicia al comenzar el sonado y la detiene al terminar

#### Scenario: Sin módulo de sonido
- **WHEN** no hay implementación de sonido registrada
- **THEN** el servicio usa el tono de alarma por defecto del sistema para que la alarma nunca sea silenciosa

### Requirement: Resolución de la alarma
El sistema SHALL ofrecer una operación de resolución con motivo (resuelta, pospuesta o tiempo agotado) que detenga el servicio y retire la notificación cuando la alarma se resuelve, y SHALL aplicar las reglas de reprogramación o desactivación de la alarma una sola vez por disparo.

#### Scenario: Alarma resuelta
- **WHEN** se notifica al servicio que la alarma ha sido completada
- **THEN** se detiene el audio, se retira la notificación y el servicio termina

#### Scenario: Observadores del ciclo de vida
- **WHEN** el sonado comienza o termina
- **THEN** se notifica a los observadores registrados con la sesión y, al terminar, el motivo (resuelta, pospuesta o tiempo agotado), y un fallo en un observador no interrumpe el sonado

#### Scenario: Sesión de sonado
- **WHEN** se inicia el sonado de una alarma
- **THEN** el identificador de sesión creado al disparar se propaga al audio, a la pantalla de sonar y a la resolución

#### Scenario: Sesión reutilizada
- **WHEN** una alarma se vuelve a disparar con un identificador de sesión existente
- **THEN** se reutiliza esa sesión en lugar de crear una nueva

#### Scenario: Disparo duplicado
- **WHEN** llega un segundo disparo de la misma alarma mientras ya está sonando
- **THEN** no se inicia un segundo sonado
