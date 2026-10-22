# wake-event-log Specification

## Purpose
Registrar de forma persistente y fiable lo que ocurre cuando suena una alarma (sonó, snooze, fallo de reto, completado), agrupado en sesiones de despertar, como base del historial que consumen estadísticas, dificultad adaptativa y compartir.

## Requirements

### Requirement: Registro de eventos por sesión de despertar
El sistema SHALL registrar eventos de cada ocurrencia de una alarma, agrupados en una sesión de despertar. Los tipos de evento SHALL ser: sonó, snooze, fallo de reto, completado, perdida y sospechosa. Cada evento SHALL guardar su instante, la sesión a la que pertenece y la alarma de origen. Los eventos de reto SHALL guardar además el tipo de reto y su dificultad cuando se conozcan.

#### Scenario: Una alarma suena
- **WHEN** suena una alarma
- **THEN** se crea una sesión nueva con un evento «sonó» que guarda el instante programado y el instante real

#### Scenario: Snooze
- **WHEN** el usuario pospone la alarma durante una sesión
- **THEN** se añade un evento «snooze» a esa sesión

#### Scenario: Fallo de reto
- **WHEN** el usuario falla un reto durante una sesión
- **THEN** se añade un evento «fallo de reto» a esa sesión con el tipo de reto y la dificultad

#### Scenario: Completado
- **WHEN** el usuario completa el reto y la alarma se detiene
- **THEN** se añade un evento «completado» a esa sesión y la sesión queda cerrada

### Requirement: Alarma perdida
El sistema SHALL permitir registrar una alarma perdida (el dispositivo estuvo apagado a su hora) con un evento «perdida» en una sesión nueva que guarda el instante programado. Una sesión perdida SHALL figurar de inmediato como no completada, SHALL NOT contar como fallo de reto y SHALL romper la racha.

#### Scenario: Dispositivo apagado
- **WHEN** se registra una alarma perdida con su instante programado
- **THEN** existe una sesión no completada sin fallos de reto ni snoozes

### Requirement: Registro append-only y durable
El sistema SHALL conservar los eventos entre sesiones de la app y reinicios, SHALL NOT modificar eventos ya registrados y SHALL conservar el historial aunque la alarma de origen se elimine.

#### Scenario: Persistencia tras reinicio
- **WHEN** se cierra y se vuelve a abrir la app
- **THEN** los eventos registrados siguen disponibles sin cambios

#### Scenario: Alarma eliminada
- **WHEN** se elimina una alarma con eventos registrados
- **THEN** sus eventos se conservan sin referencia a la alarma

### Requirement: Eventos idempotentes
El sistema SHALL ignorar un evento «sonó» o «completado» duplicado para la misma sesión, y SHALL aceptar un evento de una sesión desconocida creando su sesión implícita sin fallar.

#### Scenario: Doble «completado»
- **WHEN** se registra «completado» dos veces para la misma sesión
- **THEN** solo existe un evento «completado» en esa sesión

### Requirement: Historial legible para otros módulos
El sistema SHALL exponer el historial de sesiones, en orden cronológico, como consulta puntual y como flujo reactivo, filtrable por rango de fechas y por tipo de reto. Cada sesión SHALL incluir instante programado, instante de inicio, número de snoozes, número de fallos, instante de completado si existe y tiempo hasta despertar.

#### Scenario: Consulta reactiva
- **WHEN** hay un observador del historial y se registra un evento
- **THEN** el observador recibe el historial actualizado

#### Scenario: Filtrar por tipo de reto
- **WHEN** se consulta el historial de un tipo de reto
- **THEN** solo se devuelven las sesiones que tuvieron eventos de ese tipo de reto

### Requirement: Consulta de sesiones recientes por alarma
El sistema SHALL permitir consultar las últimas N sesiones de una alarma concreta, de la más reciente a la más antigua, y SHALL permitir excluir las sesiones sospechosas. Un módulo de detección de trampas SHALL poder marcar una sesión como sospechosa registrando un evento «sospechosa»; una sesión con ese evento SHALL exponerse como sospechosa.

#### Scenario: Últimas sesiones de una alarma
- **WHEN** se piden las últimas 5 sesiones de una alarma con 8 sesiones registradas
- **THEN** se devuelven las 5 más recientes con su número de snoozes, de fallos y su tiempo hasta despertar

#### Scenario: Excluir sospechosas
- **WHEN** una de las últimas sesiones tiene un evento «sospechosa» y se pide excluirlas
- **THEN** esa sesión no se devuelve y el límite N se completa con sesiones anteriores no sospechosas

### Requirement: Sesión sin resolver
El sistema SHALL tratar como «no completada» una sesión sin evento «completado» pasadas 24 horas desde que sonó, y SHALL NOT contar su tiempo hasta despertar.

#### Scenario: Alarma ignorada
- **WHEN** pasan 24 horas desde que sonó una alarma sin evento «completado»
- **THEN** la sesión figura como no completada y rompe la racha

### Requirement: Privacidad local
El sistema SHALL guardar el historial únicamente en el dispositivo y SHALL NOT enviarlo a ningún servicio externo.

#### Scenario: Sin red
- **WHEN** se registra o consulta el historial
- **THEN** no se realiza ninguna comunicación de red
