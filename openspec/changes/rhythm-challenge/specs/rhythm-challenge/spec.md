# Spec Delta

## Purpose

Define el reto de ritmo: notas que caen por carriles que el usuario debe tocar a tiempo, su configuración por alarma (número de rondas) y la regla de penalización por fallos.

## ADDED Requirements

### Requirement: Rondas de notas en carriles
El sistema SHALL presentar el reto de ritmo como una serie de rondas. En cada ronda SHALL caer un número fijo de notas por tres carriles hasta una línea de impacto, en orden y con separación temporal regular. El reto se supera al completar todas las rondas configuradas.

#### Scenario: Acierto
- **WHEN** el usuario toca el carril de una nota mientras ésta está dentro de la ventana de impacto
- **THEN** la nota se da por acertada y el progreso de la ronda avanza

#### Scenario: Superar el reto
- **WHEN** el usuario completa la última ronda
- **THEN** el reto se supera

#### Scenario: Duración orientativa
- **WHEN** la alarma usa el número de rondas por defecto y el usuario no falla
- **THEN** el reto dura aproximadamente un minuto

### Requirement: Fallos y penalización
El sistema SHALL contar como fallo una nota que cruza la línea de impacto sin ser tocada y un toque en un carril sin nota en la ventana de impacto. Al llegar a 3 fallos en una ronda, el sistema SHALL reiniciar solo esa ronda con un patrón nuevo, conservando las rondas ya superadas, y SHALL poner el contador de fallos a cero.

#### Scenario: Nota perdida
- **WHEN** una nota cruza la línea de impacto sin que el usuario toque su carril
- **THEN** cuenta un fallo y la ronda continúa

#### Scenario: Toque en vacío
- **WHEN** el usuario toca un carril que no tiene ninguna nota en la ventana de impacto
- **THEN** cuenta un fallo

#### Scenario: Reinicio de ronda
- **WHEN** el usuario acumula 3 fallos en la ronda 3
- **THEN** la ronda 3 empieza de nuevo con un patrón distinto y las rondas 1 y 2 siguen superadas

#### Scenario: Un fallo aislado
- **WHEN** el usuario falla 1 o 2 notas en una ronda y acierta el resto
- **THEN** la ronda se supera sin reiniciarse

### Requirement: Número de rondas por alarma
El sistema SHALL permitir configurar en cada alarma el número de rondas del reto de ritmo, entre 2 y 8, con 4 por defecto, y SHALL guardarlo en los parámetros del tipo de reto de la alarma. Un valor ausente, ilegible o fuera de rango SHALL tratarse como el valor por defecto o acotarse al rango.

#### Scenario: Configurar rondas
- **WHEN** el usuario habilita el reto de ritmo, fija 6 rondas y guarda la alarma
- **THEN** al reabrir la alarma el editor muestra 6 rondas y al sonar el reto tiene 6 rondas

#### Scenario: Alarma nueva
- **WHEN** el usuario habilita el reto de ritmo en una alarma sin parámetros guardados
- **THEN** el editor muestra 4 rondas

#### Scenario: Parámetros inválidos
- **WHEN** los parámetros guardados no son legibles o el valor está fuera de 2 a 8
- **THEN** el reto usa 4 rondas o el valor acotado al rango y no falla

### Requirement: Integración en el editor y en el modo de la alarma
El sistema SHALL ofrecer «Ritmo» entre los tipos de reto del editor, con el editor de rondas visible solo mientras el tipo está habilitado, y SHALL tratar una alarma con este reto como «Con retos» (no «Normal»). El reto SHALL estar siempre disponible, sin permisos ni sensores.

#### Scenario: Habilitar el tipo
- **WHEN** el usuario habilita «Ritmo» en una alarma «Con retos»
- **THEN** aparece el editor de rondas con el valor actual

#### Scenario: Convivencia con otros tipos
- **WHEN** la alarma tiene habilitados Ritmo y otros tipos
- **THEN** Ritmo cuenta como un reto más del número de retos y de los modos encadenado o aleatorio

### Requirement: Aleatoriedad por sesión
El sistema SHALL generar los patrones de notas de forma aleatoria en cada sesión y en cada reinicio de ronda.

#### Scenario: Dos sesiones
- **WHEN** la alarma suena dos días seguidos con la misma configuración
- **THEN** los patrones de notas son distintos

### Requirement: Anti-trampas de la alarma
El reto SHALL NOT definir un nivel anti-trampas propio: se ejecuta bajo el nivel efectivo de la alarma. El sistema SHALL pasar cada toque por carril al validador de plausibilidad de toques de ese nivel, que descarta los toques no táctiles, con la ventana tapada o más rápidos que un humano. Un toque descartado SHALL NOT acertar notas. Las salidas de pantalla se cuentan según `session-integrity`.

#### Scenario: Toques humanos
- **WHEN** el usuario toca los carriles con la irregularidad natural de una persona
- **THEN** los toques se aceptan y se evalúan contra las notas

#### Scenario: Plausibilidad en OFF
- **WHEN** el nivel efectivo de la alarma es `OFF`
- **THEN** el reto no descarta toques por plausibilidad

#### Scenario: Salir durante el reto
- **WHEN** el usuario abandona la pantalla de sonar durante el reto con la alarma en `STANDARD`
- **THEN** se aplica la recuperación de pantalla y el conteo de salidas habituales, sin reglas propias del reto

### Requirement: Textos localizados
El sistema SHALL ofrecer todos los textos del reto de ritmo (nombre del tipo, instrucciones, rondas, fallos) en los mismos idiomas que los demás retos.

#### Scenario: Idioma inglés
- **WHEN** el dispositivo está en inglés
- **THEN** los textos del reto aparecen en inglés
