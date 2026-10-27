# tamper-resistance Specification

## Purpose
Define las medidas best-effort contra apagar el móvil, cerrar la app, desinstalarla o entrar a ajustes durante una alarma, según las capacidades que el dispositivo conceda, y los límites que no se pueden superar.

## Requirements

### Requirement: Detección de capacidades
El sistema SHALL determinar qué capacidades de resistencia están disponibles: superposición y administrador de dispositivo.

#### Scenario: Sin privilegios
- **WHEN** la app no tiene administrador de dispositivo
- **THEN** el sistema registra la capacidad como básica y solo aplica las medidas que no requieren privilegios

### Requirement: Modo de tarea bloqueada
El sistema SHALL entrar en modo de tarea bloqueada durante el lockdown con nivel `STRICT` cuando la app es propietaria del dispositivo, de modo que Home, Recientes, la barra de estado y el menú de apagado queden suprimidos.

#### Scenario: Nivel STRICT con propietario
- **WHEN** empieza el lockdown con nivel `STRICT` y la app es propietaria del dispositivo
- **THEN** Home, Recientes, el panel de notificaciones y el menú de apagado no responden hasta que termina el lockdown

#### Scenario: Salida garantizada
- **WHEN** el lockdown termina por cualquier causa
- **THEN** el modo de tarea bloqueada se desactiva y el dispositivo vuelve a su comportamiento normal

### Requirement: Degradación sin privilegios
El sistema SHALL aplicar solo las medidas disponibles cuando el nivel `STRICT` no es alcanzable, y SHALL informar al usuario de qué medidas faltan.

#### Scenario: STRICT sin propietario del dispositivo
- **WHEN** una alarma tiene nivel `STRICT` y la app no es propietaria del dispositivo
- **THEN** el lockdown se comporta como `STANDARD` más las medidas disponibles y la pantalla de estado muestra el nivel efectivo

### Requirement: Protección contra desinstalación
El sistema SHALL ofrecer activar el administrador de dispositivo para impedir la desinstalación directa mientras esté activo.

#### Scenario: Administrador activo
- **WHEN** el administrador de dispositivo está activo y el usuario intenta desinstalar la app
- **THEN** el sistema exige desactivar antes el administrador

### Requirement: Límites declarados
El sistema SHALL declarar que no puede impedir el apagado forzado por hardware, el modo seguro, la recuperación, ADB ni la retirada de la batería.

#### Scenario: Consulta de límites
- **WHEN** el usuario abre la pantalla de estado de anti-trampas
- **THEN** se muestra qué medidas están activas y qué acciones siguen siendo posibles
