# Spec Delta

## Purpose

Define el reto de secuencia de formas: el usuario debe elegir, en el mismo orden, las formas que se le han mostrado una tras otra, como forma de comprobar que está despierto y atento.

## ADDED Requirements

### Requirement: Reto de repetir una secuencia de formas
El sistema SHALL ofrecer el tipo de reto "secuencia de formas", que muestra varias formas una tras otra y se supera cuando el usuario elige las mismas formas en el mismo orden.

#### Scenario: Secuencia correcta
- **WHEN** el usuario elige todas las formas de la secuencia en el orden mostrado
- **THEN** el reto se supera

#### Scenario: Forma equivocada
- **WHEN** el usuario elige una forma distinta de la que corresponde en esa posición
- **THEN** el intento falla, se avisa al usuario y se muestra de nuevo la misma secuencia desde el principio

#### Scenario: Entrada durante la muestra
- **WHEN** la secuencia se está mostrando
- **THEN** los toques sobre la paleta se ignoran hasta que termine la muestra

### Requirement: Dificultad de la secuencia
El sistema SHALL determinar la longitud de la secuencia, el número de formas distintas de la paleta y el tiempo de muestra por forma únicamente a partir del nivel de dificultad (1..5) efectivo de la sesión, de modo que un nivel mayor nunca produzca una secuencia más corta, una paleta más pequeña ni una muestra más lenta.

#### Scenario: Nivel mínimo y máximo
- **WHEN** se genera el reto en el nivel 1 y en el nivel 5
- **THEN** la secuencia del nivel 5 es estrictamente más larga que la del nivel 1

#### Scenario: Parámetros guardados
- **WHEN** la alarma guarda `params` con valores propios
- **THEN** se ignoran y manda el nivel de dificultad

#### Scenario: Nivel fuera de rango
- **WHEN** se solicita un nivel inferior a 1 o superior a 5
- **THEN** se acota al rango válido

### Requirement: Secuencia estable durante la sesión
El sistema SHALL mantener la misma secuencia y paleta entre intentos fallidos de una misma instancia del reto y SHALL generar una secuencia nueva cada vez que se crea una instancia. Un fallo SHALL contarse como intento fallido y no ofrecer pistas.

#### Scenario: Reintento tras fallo
- **WHEN** el usuario falla y se muestra de nuevo
- **THEN** la secuencia mostrada es idéntica a la del primer intento y el fallo queda registrado

#### Scenario: Nueva alarma
- **WHEN** suena la alarma otro día
- **THEN** la secuencia generada es independiente de la anterior

### Requirement: Disponibilidad sin hardware
El sistema SHALL tratar el reto como siempre disponible, sin requerir sensores ni permisos.

#### Scenario: Dispositivo sin sensores
- **WHEN** el dispositivo no tiene acelerómetro, sensor de pasos ni cámara
- **THEN** el reto de secuencia de formas sigue estando disponible y seleccionable en el editor

### Requirement: Selección y compartición del tipo
El sistema SHALL permitir añadir el reto al editor de retos de una alarma y mostrarlo con nombre localizado (es/en) en las listas y en la importación de alarmas compartidas.

#### Scenario: Añadir al editor
- **WHEN** el usuario abre el editor de retos de una alarma
- **THEN** el reto de secuencia de formas aparece entre los tipos seleccionables

#### Scenario: Alarma importada
- **WHEN** se importa una alarma compartida que incluye este reto
- **THEN** el reto se muestra con su nombre localizado
