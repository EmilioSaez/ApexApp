## ADDED Requirements

### Requirement: Opción de silencio por alarma
El sistema SHALL ofrecer por alarma la opción «silenciar mientras resuelvo los retos», desactivada por defecto, junto con un tiempo de inactividad de 10 a 300 segundos (30 por defecto).

#### Scenario: Alarma nueva
- **WHEN** se crea una alarma
- **THEN** la opción está desactivada y la inactividad es de 30 segundos

#### Scenario: Inactividad fuera de rango
- **WHEN** se intenta crear una alarma con una inactividad menor de 10 s o mayor de 300 s
- **THEN** el sistema la rechaza

### Requirement: Silencio al interactuar con los retos
El sistema SHALL silenciar el sonido, la vibración y la voz de la alarma al registrarse la primera interacción del usuario con los retos, si la opción está activa, y SHALL NOT silenciar por el solo hecho de mostrar la pantalla de retos.

#### Scenario: Primer toque
- **WHEN** suena una alarma con la opción activa y el usuario toca la pantalla de retos
- **THEN** el sonido, la vibración y la voz se silencian

#### Scenario: Pantalla abierta sin interacción
- **WHEN** la pantalla de retos está abierta y el usuario no la toca
- **THEN** la alarma sigue sonando

#### Scenario: Progreso sin toques
- **WHEN** el usuario completa un reto por sensor sin tocar la pantalla
- **THEN** cuenta como interacción

#### Scenario: Opción desactivada
- **WHEN** la alarma tiene la opción desactivada y el usuario interactúa con los retos
- **THEN** la alarma sigue sonando

### Requirement: Reactivación por inactividad
El sistema SHALL volver a hacer sonar la alarma (sonido, vibración y voz) cuando transcurra el tiempo de inactividad sin interacción, y SHALL volver a silenciarla con la siguiente interacción.

#### Scenario: Inactividad
- **WHEN** la alarma está silenciada y pasan 30 s sin interacción
- **THEN** la alarma vuelve a sonar

#### Scenario: Interacción que reinicia la cuenta
- **WHEN** la alarma está silenciada y el usuario interactúa antes de agotarse la inactividad
- **THEN** la alarma sigue silenciada y la cuenta empieza de nuevo

#### Scenario: Nueva interacción tras reactivarse
- **WHEN** la alarma ha vuelto a sonar por inactividad y el usuario interactúa
- **THEN** la alarma se silencia de nuevo

### Requirement: Fin de la alarma y límites
El sistema SHALL detener la alarma al superar todos los retos como hasta ahora, SHALL conservar el tiempo máximo de sonado, el bloqueo de volumen y el anti-trampas, y SHALL NOT modificar el volumen del stream de alarma para silenciar.

#### Scenario: Retos superados estando silenciada
- **WHEN** el usuario supera todos los retos con la alarma silenciada
- **THEN** la alarma se detiene y no queda temporizador de inactividad

#### Scenario: Posponer estando silenciada
- **WHEN** la alarma está silenciada y se pospone
- **THEN** se descarta el estado de silencio y, al volver a sonar, lo hace sin silenciar

#### Scenario: Bloqueo de volumen
- **WHEN** la alarma está silenciada con el anti-trampas activo y el usuario baja el volumen
- **THEN** el volumen del stream de alarma se restablece al máximo y cuenta como intento de manipulación

#### Scenario: Tiempo máximo de sonado
- **WHEN** la alarma lleva silenciada hasta agotarse el tiempo máximo de sonado
- **THEN** la sesión termina por tiempo agotado

### Requirement: Máquina de inactividad pura
El sistema SHALL implementar la lógica de silencio e inactividad sin dependencias de Android y con reloj inyectado.

#### Scenario: Prueba con reloj simulado
- **WHEN** una prueba unitaria avanza el reloj inyectado
- **THEN** la máquina cambia de estado sin esperas reales
