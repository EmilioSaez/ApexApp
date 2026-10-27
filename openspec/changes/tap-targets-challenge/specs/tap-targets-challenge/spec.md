## Purpose
Reto de reflejos sobre la pantalla: tocar círculos que aparecen en posiciones aleatorias antes de que desaparezcan, configurable por número de rondas y sujeto al nivel anti-trampas de la alarma.

## ADDED Requirements

### Requirement: Rondas de círculos
El sistema SHALL ofrecer un reto «Tocar objetivos» formado por rondas; en cada ronda aparecen varios círculos a la vez (según el nivel) en posiciones aleatorias, sin solaparse ni salirse de la zona de juego, y la ronda se supera al tocar todos antes de que venza su tiempo.

#### Scenario: Ronda superada
- **WHEN** el usuario toca todos los círculos antes de que venza el tiempo de la ronda
- **THEN** la ronda cuenta como superada y comienza la siguiente tras una breve pausa

#### Scenario: Ronda fallida por tiempo
- **WHEN** vence el tiempo de la ronda con algún círculo sin tocar
- **THEN** la ronda cuenta como fallida

#### Scenario: Toque fuera de un círculo
- **WHEN** el usuario toca fuera de todos los círculos
- **THEN** el toque se ignora y la ronda sigue

### Requirement: Número de rondas configurable
El sistema SHALL permitir configurar por alarma el número de rondas del reto (3 a 15, 8 por defecto) como único parámetro propio, y SHALL usar el valor por defecto si el parámetro falta o es inválido y acotar al rango si está fuera de él.

#### Scenario: Valor por defecto
- **WHEN** la alarma habilita el reto sin parámetros
- **THEN** el reto tiene 8 rondas

#### Scenario: Valor fuera de rango
- **WHEN** el parámetro guardado es 40
- **THEN** el reto usa 15 rondas

#### Scenario: Duración orientativa
- **WHEN** el usuario juega con el valor por defecto sin fallar
- **THEN** el reto dura en torno a un minuto

### Requirement: Dificultad por nivel
El sistema SHALL escalar la exigencia del reto con el nivel de dificultad de 1 a 5, igual que el reto de matemáticas, y SHALL obtener el nivel del proveedor de dificultad de la sesión (incluido el adaptativo). A mayor nivel, más círculos por ronda, más pequeños y con menos tiempo; el número de rondas no depende del nivel.

#### Scenario: Nivel 1 frente a nivel 5
- **WHEN** se generan rondas en nivel 1 y en nivel 5
- **THEN** las de nivel 5 tienen más círculos, de menor tamaño y con menos tiempo que las de nivel 1

#### Scenario: Nivel fuera de rango
- **WHEN** se pide una instancia con un nivel fuera de 1 a 5
- **THEN** se acota al rango

### Requirement: El reto no permite apagar la alarma sin superarlo
El sistema SHALL mantener la alarma sonando hasta que el reto se supere: fallar, descartar toques o agotar el tiempo de una ronda SHALL NOT dar la alarma por resuelta, y la ronda fallida se repite hasta superarla.

#### Scenario: Fallos repetidos
- **WHEN** el usuario falla una misma ronda varias veces
- **THEN** la alarma sigue sonando y la ronda se repite con posiciones nuevas

### Requirement: Penalización por fallo
El sistema SHALL tratar una ronda fallida como no superada y añadir una ronda extra, hasta un máximo de tantas rondas extra como rondas configuradas, y SHALL NOT reiniciar las rondas ya superadas. Alcanzado el tope, una ronda fallida se repite sin añadir más.

#### Scenario: Fallo con rondas extra disponibles
- **WHEN** el usuario falla una ronda con 3 de 8 superadas
- **THEN** conserva las 3 superadas y le faltan 6 rondas por superar (5 + 1 extra)

#### Scenario: Tope de rondas extra
- **WHEN** el usuario ya acumuló el máximo de rondas extra y falla otra ronda
- **THEN** la ronda se repite y el total no aumenta

#### Scenario: Reto superado
- **WHEN** el usuario supera todas las rondas pendientes, extra incluidas
- **THEN** el reto se da por superado

### Requirement: Aleatoriedad por sesión
El sistema SHALL generar posiciones y tiempos de espera de cada ronda con el generador aleatorio de la sesión, de modo que dos sesiones o dos rondas no repitan una secuencia previsible.

#### Scenario: Reintento tras fallo
- **WHEN** el usuario falla una ronda y se repite
- **THEN** los círculos aparecen en posiciones nuevas

### Requirement: Plausibilidad de toques según el nivel anti-trampas
El sistema SHALL aplicar a los toques del reto el nivel anti-trampas efectivo de la alarma, sin definir niveles propios: con `OFF` no valida los toques; con `STANDARD` descarta los toques que no proceden de la pantalla táctil, los recibidos con la ventana tapada por otra superficie y los sucesivos separados menos del mínimo humano. Un toque descartado SHALL NOT contar como acierto.

#### Scenario: OFF no valida
- **WHEN** la alarma tiene nivel `OFF` y se reciben toques muy rápidos
- **THEN** todos se aceptan

#### Scenario: Toque con ventana tapada
- **WHEN** el nivel es `STANDARD` y un toque llega con la ventana parcialmente tapada
- **THEN** el toque se descarta y no cuenta como acierto

### Requirement: Accesibilidad y textos
El sistema SHALL presentar el reto con texto en español e inglés, mostrar el progreso de rondas y describir los círculos a los servicios de accesibilidad.

#### Scenario: Idioma
- **WHEN** el dispositivo está en inglés
- **THEN** el nombre del reto y sus textos aparecen en inglés
