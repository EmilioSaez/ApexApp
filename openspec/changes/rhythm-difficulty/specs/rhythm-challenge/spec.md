## ADDED Requirements

### Requirement: Velocidad de las notas según la dificultad
El sistema SHALL acelerar las notas al subir la dificultad de la alarma (tempo y velocidad de caída mayores) y SHALL NOT cambiar con ella la duración de la ronda ni el número de rondas. A mayor tempo, la ronda SHALL contener más notas para mantener su duración.

#### Scenario: Más dificultad, más rápido
- **WHEN** se comparan los niveles 1 y 5
- **THEN** en el nivel 5 el tempo es mayor y las notas tardan menos en caer hasta la línea de impacto

#### Scenario: Misma duración
- **WHEN** se comparan las rondas de cualquier nivel
- **THEN** su duración es aproximadamente la misma

#### Scenario: Rondas independientes de la dificultad
- **WHEN** se genera el reto con los mismos parámetros en dos niveles distintos
- **THEN** ambos tienen el mismo número de rondas
