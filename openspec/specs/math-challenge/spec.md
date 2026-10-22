# math-challenge Specification

## Purpose
Reto de operaciones aritméticas generadas por nivel de dificultad, con resultado entero único, validación de la respuesta y entrada numérica en pantalla.

## Requirements

### Requirement: Generación de operaciones por nivel
El sistema SHALL ofrecer un reto de matemáticas que presenta una operación aritmética cuya complejidad crece con el nivel de 1 a 5: sumas y restas de números pequeños en los niveles bajos, multiplicaciones en el nivel medio y operaciones combinadas de varios pasos con números mayores en los niveles altos.

#### Scenario: Nivel 1
- **WHEN** se genera una instancia en nivel 1
- **THEN** la operación es una suma o resta con resultado entero positivo

#### Scenario: Nivel 5
- **WHEN** se genera una instancia en nivel 5
- **THEN** la operación combina varios pasos o números mayores que en el nivel 3

### Requirement: Resultado entero y único
El sistema SHALL generar solo operaciones cuyo resultado correcto sea un número entero único.

#### Scenario: Sin divisiones inexactas
- **WHEN** se genera cualquier instancia de cualquier nivel
- **THEN** el resultado esperado es un entero y la operación no incluye divisiones inexactas

### Requirement: Validación de la respuesta
El sistema SHALL aceptar la respuesta numérica del usuario como correcta solo si coincide exactamente con el resultado, y SHALL rechazar entradas vacías o no numéricas sin contarlas como acierto.

#### Scenario: Respuesta correcta
- **WHEN** el usuario introduce el resultado exacto
- **THEN** el reto se da por superado

#### Scenario: Entrada no numérica
- **WHEN** el usuario envía una entrada vacía o con texto
- **THEN** se rechaza como respuesta no válida y el reto sigue pendiente

### Requirement: Entrada numérica en pantalla
El sistema SHALL presentar el enunciado y un campo de entrada numérica con la acción de confirmar, y SHALL indicar visualmente cuando la respuesta es incorrecta.

#### Scenario: Indicación de error
- **WHEN** el usuario confirma una respuesta incorrecta
- **THEN** se muestra un aviso de error y el campo se vacía para reintentar
