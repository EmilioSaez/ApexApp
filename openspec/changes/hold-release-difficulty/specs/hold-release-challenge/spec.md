## MODIFIED Requirements

### Requirement: Señal en instante aleatorio
El sistema SHALL cambiar la señal de cada ronda tras un tiempo de mantenimiento aleatorio dentro de un rango que depende de la dificultad y que se alarga al subirla, sorteado en cada sesión y en cada ronda, y SHALL NOT mostrar cuenta atrás ni otra pista del instante del cambio.

#### Scenario: Instantes distintos
- **WHEN** se generan dos sesiones del reto con semillas aleatorias distintas
- **THEN** los instantes de señal de sus rondas no coinciden en todas las rondas

#### Scenario: Instante dentro del rango del nivel
- **WHEN** se sortea el instante de la señal de una ronda
- **THEN** está entre el mínimo y el máximo definidos para la dificultad de la alarma

#### Scenario: Más dificultad, más espera
- **WHEN** se comparan los niveles 1 y 5
- **THEN** el máximo de mantenimiento del nivel 5 es mayor que el del nivel 1

### Requirement: Penalización por fallo sin reinicio total
El sistema SHALL, ante una ronda fallada, repetirla con un nuevo instante de señal y añadir una ronda extra al total hasta un máximo de rondas extra que disminuye al subir la dificultad, y SHALL reiniciar solo la ronda en curso, sin ronda extra y conservando las superadas, al tercer fallo consecutivo en ella.

#### Scenario: Un fallo
- **WHEN** el usuario falla una ronda
- **THEN** el total de rondas sube en una, las rondas ya superadas se conservan y la ronda se repite con otro instante de señal

#### Scenario: Tope de rondas extra
- **WHEN** las rondas extra acumuladas alcanzan el máximo de la dificultad y el usuario vuelve a fallar
- **THEN** el total no aumenta más y la ronda se repite

#### Scenario: Menos margen en dificultad alta
- **WHEN** se comparan los niveles 1 y 5
- **THEN** el máximo de rondas extra del nivel 5 es menor que el del nivel 1

#### Scenario: Tres fallos seguidos
- **WHEN** el usuario falla tres veces seguidas la misma ronda
- **THEN** se reinicia esa ronda con un nuevo sorteo, el tercer fallo no suma ronda extra y las rondas superadas se conservan
