# alarm-lockdown Specification

## Purpose
Define el modo kiosko sobre la pantalla de sonar: mientras una alarma suena con nivel anti-trampas activo, el usuario no puede abandonarla con Atrás, Home o Recientes, y la pantalla se recupera si se interrumpe.

## Requirements

### Requirement: Inicio del lockdown con el sonado
El sistema SHALL activar el lockdown cuando empieza el sonado de una alarma cuyo nivel efectivo es `STANDARD` o `STRICT`, y SHALL no activarlo con nivel `OFF`.

#### Scenario: Alarma STANDARD empieza a sonar
- **WHEN** empieza a sonar una alarma con nivel efectivo `STANDARD`
- **THEN** la pantalla de sonar queda en modo kiosko y el volumen forzado se activa

#### Scenario: Alarma OFF
- **WHEN** empieza a sonar una alarma con nivel `OFF`
- **THEN** no se aplica ninguna medida y el usuario puede salir de la app libremente

### Requirement: Pantalla de sonar al primer plano
El sistema SHALL abrir la pantalla de sonar al empezar el lockdown aunque el dispositivo esté en uso, siempre que la app tenga permiso de superposición.

#### Scenario: Dispositivo en uso con superposición
- **WHEN** empieza el sonado de una alarma `STANDARD` con otra app en primer plano y permiso de superposición
- **THEN** la pantalla de sonar pasa al primer plano sin esperar a que el usuario toque la notificación

#### Scenario: Dispositivo en uso sin superposición
- **WHEN** empieza el sonado con otra app en primer plano y sin permiso de superposición
- **THEN** se muestra la notificación de alarma y el lockdown se aplica cuando el usuario abre la pantalla de sonar

### Requirement: Modo inmersivo sin salida
El sistema SHALL ocultar las barras del sistema en la pantalla de sonar durante el lockdown y SHALL ignorar el botón y el gesto de Atrás.

#### Scenario: Pulsar Atrás
- **WHEN** el usuario pulsa Atrás o hace el gesto de retroceso durante el lockdown
- **THEN** la pantalla de sonar permanece y el sonado continúa

#### Scenario: Gesto del borde del sistema
- **WHEN** el usuario desliza desde el borde para mostrar las barras del sistema
- **THEN** las barras aparecen de forma transitoria y vuelven a ocultarse sin cerrar la pantalla

### Requirement: Recuperación del primer plano
El sistema SHALL traer de nuevo la pantalla de sonar al primer plano si el usuario la abandona con Home o Recientes durante el lockdown, siempre que el sistema lo permita.

#### Scenario: Pulsar Home
- **WHEN** el usuario pulsa Home durante el lockdown y la app tiene permiso de superposición
- **THEN** la pantalla de sonar vuelve al primer plano en pocos segundos y el sonido no se interrumpe

#### Scenario: Sin permiso de superposición
- **WHEN** el usuario pulsa Home y la app no tiene permiso de superposición
- **THEN** el sonado continúa y la notificación de alarma permanece para reabrir la pantalla de sonar

### Requirement: Fin del lockdown
El sistema SHALL liberar el modo kiosko, las medidas de resistencia y el volumen forzado cuando el sonado termina por resolución, snooze o agotamiento del tiempo máximo.

#### Scenario: Alarma resuelta
- **WHEN** el sonado termina porque el usuario completó los retos
- **THEN** se libera el lockdown y la app vuelve a su navegación normal

#### Scenario: Tiempo máximo agotado
- **WHEN** el sonado termina por superar el tiempo máximo sin resolverse
- **THEN** se libera el lockdown completamente, incluido el lock task si estaba activo

#### Scenario: Snooze
- **WHEN** el sonado termina porque el usuario pospone la alarma
- **THEN** se libera el lockdown hasta que la alarma vuelva a sonar

### Requirement: Interrupción durante el sonado
El sistema SHALL tratar como interrupción la muerte del proceso o el reinicio del dispositivo durante un sonado sin resolver, y SHALL registrarla en la sesión. La reanudación del sonado tras la interrupción corresponde a la fiabilidad de la alarma; si el sonado se reanuda, el lockdown se aplica de nuevo.

#### Scenario: Muerte del proceso durante el sonado
- **WHEN** el proceso muere durante el sonado y la app vuelve a arrancar
- **THEN** la sesión interrumpida queda registrada como tal y el lockdown no permanece activo

#### Scenario: Sonado reanudado
- **WHEN** el sonado se reanuda tras una interrupción
- **THEN** la pantalla de sonar vuelve en modo kiosko con el volumen forzado
