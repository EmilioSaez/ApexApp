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

### Requirement: Configuración de intensidad
El sistema SHALL permitir configurar por alarma el objetivo del reto (número de sacudidas, de pasos o duración de la marcha) dentro de límites mínimos y máximos definidos.

#### Scenario: Valor dentro de límites
- **WHEN** el usuario fija un objetivo dentro del rango permitido
- **THEN** el objetivo se guarda con la alarma

#### Scenario: Valor fuera de límites
- **WHEN** el usuario fija un objetivo fuera del rango permitido
- **THEN** la app lo ajusta al límite más cercano y se lo indica
