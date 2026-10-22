# session-integrity Specification

## Purpose
Define cómo se detectan y marcan las sesiones de despertar sospechosas de manipulación, para que las estadísticas y la dificultad adaptativa no las traten como resultados fiables.

## Requirements

### Requirement: Detección de intentos de manipulación
El sistema SHALL contar durante el lockdown los intentos de salida (abandonar la pantalla de sonar), las bajadas de volumen reaplicadas y la interrupción del sonado por muerte del proceso o reinicio.

#### Scenario: Salir repetidamente
- **WHEN** el usuario abandona la pantalla de sonar con Home más veces que el umbral durante una sesión
- **THEN** la sesión acumula el motivo de salidas repetidas

#### Scenario: Muerte del proceso
- **WHEN** el proceso muere o el dispositivo se reinicia mientras el sonado no estaba resuelto
- **THEN** la sesión acumula el motivo de interrupción durante el sonado

### Requirement: Marcado de sesión sospechosa
El sistema SHALL marcar como sospechosa una sesión con al menos un motivo de manipulación, o que termine por tiempo máximo sin resolverse, y SHALL registrar los motivos.

#### Scenario: Sesión limpia
- **WHEN** una sesión termina resuelta sin motivos acumulados
- **THEN** no se marca como sospechosa

#### Scenario: Sesión con motivos
- **WHEN** una sesión termina con volumen manipulado
- **THEN** se marca como sospechosa con el motivo de volumen manipulado

### Requirement: Emisión de la marca
El sistema SHALL emitir la marca de sospechosa, con sesión, alarma y motivos, a través de un puerto de eventos que el módulo de estadísticas consume, y SHALL funcionar sin él.

#### Scenario: Sin módulo de estadísticas
- **WHEN** no hay consumidor registrado del puerto de eventos
- **THEN** la sesión termina con normalidad y la marca se descarta sin error

### Requirement: Degradación no es manipulación
El sistema SHALL NOT marcar como sospechosa una sesión solo porque el nivel pedido no fuera alcanzable.

#### Scenario: STRICT degradado
- **WHEN** una sesión `STRICT` se ejecuta como `STANDARD` por falta de device owner y se resuelve sin motivos
- **THEN** no se marca como sospechosa
