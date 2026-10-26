# Spec Delta

## Purpose

Define los retos de movimiento basados en sensores del dispositivo (agitar, contar pasos y levantarse) y la verificación que impide superarlos con movimientos falsos o sensores simulados.

## ADDED Requirements

### Requirement: Reto de agitar
El sistema SHALL superar el reto de agitar cuando el acelerómetro registre el número de sacudidas válidas configurado para la alarma, y SHALL mostrar el progreso al usuario.

#### Scenario: Sacudidas suficientes
- **WHEN** el usuario agita el dispositivo hasta alcanzar el número configurado de sacudidas válidas
- **THEN** el reto se supera

#### Scenario: Movimiento suave
- **WHEN** el dispositivo se mueve por debajo del umbral de intensidad de una sacudida
- **THEN** el movimiento no cuenta y el progreso no avanza

#### Scenario: Pausa prolongada
- **WHEN** el usuario deja de agitar durante el tiempo máximo de inactividad
- **THEN** el progreso se reinicia

### Requirement: Reto de contar pasos
El sistema SHALL superar el reto de contar pasos cuando el sensor de pasos registre el número configurado de pasos tras comenzar el reto, sin contar los pasos anteriores.

#### Scenario: Pasos suficientes
- **WHEN** el usuario camina hasta alcanzar el número configurado de pasos desde el inicio del reto
- **THEN** el reto se supera

#### Scenario: Pasos previos
- **WHEN** el contador acumulado del sensor ya tenía pasos antes de empezar el reto
- **THEN** esos pasos no cuentan

#### Scenario: Sensor o permiso no disponible
- **WHEN** el dispositivo no tiene sensor de pasos o falta el permiso de reconocimiento de actividad
- **THEN** la app no permite configurar este reto, o en una alarma ya configurada delega en el mecanismo de reserva del motor de retos

### Requirement: Reto de levantarse
El sistema SHALL superar el reto de levantarse cuando detecte una transición de posición tumbada o quieta a posición de pie con movimiento sostenido de caminar.

#### Scenario: Levantarse y caminar
- **WHEN** el usuario pasa de tumbado a de pie y camina durante el tiempo mínimo configurado
- **THEN** el reto se supera

#### Scenario: Incorporarse sin ponerse de pie
- **WHEN** el usuario cambia de orientación el dispositivo sin desplazarse
- **THEN** el reto no se supera

### Requirement: Detección de sensores falsos
El sistema SHALL rechazar la señal de los sensores cuando presente patrones sintéticos: periodicidad perfecta, ausencia total de ruido, incoherencia entre sensores o ausencia de datos, y SHALL reiniciar el progreso del reto afectado.

#### Scenario: Señal perfectamente periódica
- **WHEN** el acelerómetro entrega sacudidas con amplitud y periodo prácticamente idénticos y sin ruido
- **THEN** las sacudidas se descartan y se indica que el movimiento no es válido

#### Scenario: Pasos sin movimiento
- **WHEN** el sensor de pasos incrementa su cuenta mientras el acelerómetro no registra movimiento compatible
- **THEN** los pasos no se contabilizan

#### Scenario: Sensor sin datos
- **WHEN** un sensor necesario no emite datos durante el reto
- **THEN** el reto no avanza y se aplica el mecanismo de reserva del motor de retos tras el tiempo de espera

### Requirement: Intensidad según la dificultad
El sistema SHALL derivar el objetivo del reto (número de sacudidas, de pasos o segundos de marcha) del nivel de dificultad (1..5) que decide el proveedor de dificultad, mediante una tabla única, y NO SHALL permitir al usuario elegirlo. Sacudidas 10→60 (10, 20, 30, 45, 60), pasos 15→120 (15, 30, 55, 85, 120) y segundos de marcha 5→40 (5, 10, 20, 30, 40). Los parámetros `target` de alarmas ya guardadas se ignoran.

#### Scenario: Objetivo según nivel
- **WHEN** se genera un reto de agitar, pasos o levantarse con un nivel dado
- **THEN** su objetivo es el valor de la tabla para ese nivel

#### Scenario: Dificultad adaptativa
- **WHEN** el proveedor de dificultad sube el nivel por encima del configurado
- **THEN** el objetivo del reto de movimiento sube con él

#### Scenario: Alarma guardada con objetivo propio
- **WHEN** una alarma guardada trae `params` con `target`
- **THEN** el valor se ignora y gana el nivel de dificultad

#### Scenario: Valor informativo en el editor
- **WHEN** el usuario edita la alarma con un reto de movimiento habilitado
- **THEN** el editor muestra el objetivo resultante de la dificultad actual, sin control para cambiarlo
