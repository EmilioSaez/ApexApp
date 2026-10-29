# Spec Delta

## Purpose

Define el reto de memoria de cuadrícula: el usuario debe recordar qué cuadrados de un tablero se iluminaron y tocarlos, sin importar el orden, como forma de comprobar que está despierto y atento.

## ADDED Requirements

### Requirement: Reto de recordar un conjunto de cuadrados
El sistema SHALL ofrecer el tipo de reto "memoria de cuadrícula", que ilumina a la vez un conjunto de cuadrados de un tablero y se supera cuando el usuario toca exactamente esos cuadrados, en cualquier orden.

#### Scenario: Conjunto correcto
- **WHEN** el usuario toca todos los cuadrados iluminados, en cualquier orden
- **THEN** el reto se supera

#### Scenario: Cuadrado equivocado
- **WHEN** el usuario toca un cuadrado que no estaba iluminado
- **THEN** el intento falla, se avisa al usuario y se vuelve a mostrar el mismo patrón

#### Scenario: Entrada durante la exposición
- **WHEN** el patrón se está mostrando
- **THEN** los toques sobre el tablero se ignoran hasta que termine

#### Scenario: Cuadrado repetido
- **WHEN** el usuario toca dos veces el mismo cuadrado correcto
- **THEN** no cuenta como un cuadrado adicional y el reto no se supera hasta tocar todos los distintos

### Requirement: Dificultad del tablero
El sistema SHALL determinar el tamaño del tablero (de 3×3 a 6×6), el número de cuadrados a recordar y el tiempo de exposición únicamente a partir del nivel de dificultad (1..5) efectivo de la sesión, de modo que un nivel mayor nunca produzca un tablero menor ni menos cuadrados, y que siempre haya más casillas que cuadrados iluminados.

#### Scenario: Nivel mínimo y máximo
- **WHEN** se genera el reto en el nivel 1 y en el nivel 5
- **THEN** el nivel 5 tiene un tablero mayor y más cuadrados que el nivel 1

#### Scenario: Parámetros guardados
- **WHEN** la alarma guarda `params` con valores propios
- **THEN** se ignoran y manda el nivel de dificultad

#### Scenario: Nivel fuera de rango
- **WHEN** se solicita un nivel inferior a 1 o superior a 5
- **THEN** se acota al rango válido

### Requirement: Patrón estable y determinista
El sistema SHALL generar el conjunto de cuadrados a partir de la semilla inyectada, sin repetir cuadrados, mantener el mismo conjunto entre intentos fallidos de una misma instancia y generar uno nuevo con cada instancia.

#### Scenario: Misma semilla
- **WHEN** se genera el reto dos veces con el mismo nivel y la misma semilla
- **THEN** el conjunto de cuadrados es idéntico

#### Scenario: Reintento tras fallo
- **WHEN** el usuario falla y se vuelve a mostrar
- **THEN** el patrón es idéntico al del primer intento

### Requirement: Disponibilidad sin hardware
El sistema SHALL tratar el reto como siempre disponible, sin requerir sensores ni permisos.

#### Scenario: Dispositivo sin sensores
- **WHEN** el dispositivo no tiene acelerómetro, sensor de pasos ni cámara
- **THEN** el reto sigue disponible y seleccionable en el editor

### Requirement: Selección y compartición del tipo
El sistema SHALL permitir añadir el reto al editor de retos de una alarma y mostrarlo con nombre localizado (es/en) en las listas y en la importación de alarmas compartidas, sin migración de datos.

#### Scenario: Añadir al editor
- **WHEN** el usuario abre el editor de retos de una alarma
- **THEN** el reto de memoria de cuadrícula aparece entre los tipos seleccionables

#### Scenario: Alarma importada
- **WHEN** se importa una alarma compartida que incluye el reto
- **THEN** el reto se muestra con su nombre localizado
