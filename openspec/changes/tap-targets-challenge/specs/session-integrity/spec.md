## ADDED Requirements

### Requirement: Entrada táctil sintética en juegos
El sistema SHALL contar, con el nivel anti-trampas efectivo `STRICT`, los toques que un juego táctil descarta por no parecer humanos, y SHALL marcar la sesión como sospechosa con el motivo de entrada sintética al llegar a 3. Con `OFF` y `STANDARD` los toques descartados SHALL NOT marcar la sesión. La alarma SHALL poder apagarse al superar el reto aunque la sesión esté marcada.

#### Scenario: Tres toques descartados en STRICT
- **WHEN** en una sesión `STRICT` el juego descarta 3 toques por regularidad sintética
- **THEN** la sesión se marca como sospechosa con el motivo de entrada sintética

#### Scenario: Dos toques descartados
- **WHEN** en una sesión `STRICT` el juego descarta solo 2 toques
- **THEN** la sesión no se marca por este motivo

#### Scenario: STANDARD descarta sin marcar
- **WHEN** en una sesión `STANDARD` el juego descarta toques con la ventana tapada
- **THEN** los toques no cuentan como acierto y la sesión no se marca por este motivo

#### Scenario: La alarma se apaga igualmente
- **WHEN** una sesión marcada por entrada sintética supera el reto
- **THEN** la alarma se apaga y la sesión queda excluida de estadísticas y dificultad adaptativa
