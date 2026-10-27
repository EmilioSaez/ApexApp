# Spec Delta

## Purpose

Define el reto de memoria: el usuario debe repetir una secuencia de casillas que se le muestra, como forma de comprobar que está despierto y atento.

## ADDED Requirements

### Requirement: Reto de repetir un patrón
El sistema SHALL ofrecer el tipo de reto "memoria", que muestra una secuencia de casillas iluminadas una a una y se supera cuando el usuario toca las mismas casillas en el mismo orden.

#### Scenario: Secuencia correcta
- **WHEN** el usuario toca todas las casillas de la secuencia en el orden mostrado
- **THEN** el reto se supera

#### Scenario: Casilla equivocada
- **WHEN** el usuario toca una casilla distinta de la que corresponde en esa posición
- **THEN** el intento falla, se avisa al usuario y se reproduce de nuevo la misma secuencia desde el principio

#### Scenario: Entrada durante la reproducción
- **WHEN** la secuencia se está mostrando
- **THEN** los toques sobre las casillas se ignoran hasta que termine la reproducción

### Requirement: Dificultad del patrón
El sistema SHALL determinar la longitud de la secuencia y el ritmo de reproducción únicamente a partir del nivel de dificultad (1..5) efectivo de la sesión, de modo que un nivel mayor nunca produzca una secuencia más corta.

#### Scenario: Nivel mínimo y máximo
- **WHEN** se genera el reto en el nivel 1 y en el nivel 5
- **THEN** la secuencia del nivel 5 es estrictamente más larga que la del nivel 1

#### Scenario: Parámetros guardados antiguos
- **WHEN** la alarma guarda `params` con una longitud propia
- **THEN** se ignoran y manda el nivel de dificultad

#### Scenario: Nivel fuera de rango
- **WHEN** se solicita un nivel inferior a 1 o superior a 5
- **THEN** se acota al rango válido

### Requirement: Secuencia estable durante la sesión
El sistema SHALL mantener la misma secuencia entre intentos fallidos de una misma instancia del reto y SHALL generar una secuencia nueva cada vez que se crea una instancia.

#### Scenario: Reintento tras fallo
- **WHEN** el usuario falla y se reproduce de nuevo
- **THEN** la secuencia mostrada es idéntica a la del primer intento

#### Scenario: Nueva alarma
- **WHEN** suena la alarma otro día
- **THEN** la secuencia generada es independiente de la anterior

### Requirement: Disponibilidad sin hardware
El sistema SHALL tratar el reto de memoria como siempre disponible, sin requerir sensores ni permisos.

#### Scenario: Dispositivo sin sensores
- **WHEN** el dispositivo no tiene acelerómetro, sensor de pasos ni cámara
- **THEN** el reto de memoria sigue estando disponible y seleccionable en el editor

### Requirement: Selección y compartición del tipo
El sistema SHALL permitir añadir el reto de memoria a una alarma desde el editor de retos y mostrarlo con nombre localizado en las listas y en la importación de alarmas compartidas.

#### Scenario: Añadir al editor
- **WHEN** el usuario abre el editor de retos de una alarma
- **THEN** el reto de memoria aparece entre los tipos seleccionables

#### Scenario: Alarma importada
- **WHEN** se importa una alarma compartida que incluye el reto de memoria
- **THEN** el reto se muestra con su nombre localizado
