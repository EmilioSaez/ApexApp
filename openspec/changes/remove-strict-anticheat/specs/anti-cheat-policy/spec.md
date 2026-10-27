## MODIFIED Requirements

### Requirement: Nivel anti-trampas por alarma
El sistema SHALL permitir elegir para cada alarma un nivel anti-trampas entre `OFF` y `STANDARD`, y SHALL guardarlo dentro de la política anti-trampas de la alarma. Un nivel `STRICT` guardado o compartido por versiones anteriores SHALL leerse como `STANDARD`.

#### Scenario: Alarma nueva en el editor
- **WHEN** se crea una alarma desde el editor sin cambiar el nivel
- **THEN** su nivel anti-trampas es `STANDARD`

#### Scenario: Cambiar el nivel
- **WHEN** el usuario cambia una alarma a `OFF` y la guarda
- **THEN** la alarma conserva el nivel `OFF` al volver a abrirla

#### Scenario: Nivel heredado
- **WHEN** se lee una política guardada o compartida con el nivel `STRICT`
- **THEN** el nivel resultante es `STANDARD`

### Requirement: Nivel efectivo
El sistema SHALL calcular el nivel efectivo de cada alarma a partir del nivel pedido y de las capacidades disponibles, y SHALL aplicar el efectivo al sonar.

#### Scenario: Capacidad suficiente
- **WHEN** una alarma pide `STANDARD` y las capacidades necesarias están concedidas
- **THEN** el nivel efectivo es `STANDARD`

### Requirement: Aviso de capacidades que faltan
El sistema SHALL avisar en el editor, mientras el nivel de la alarma sea `STANDARD`, de los permisos o capacidades que faltan, y SHALL ofrecer abrir la pantalla de estado para concederlos.

#### Scenario: Falta el permiso de superposición
- **WHEN** el usuario elige el nivel `STANDARD` sin permiso de superposición
- **THEN** el editor indica el permiso que falta y ofrece abrir la pantalla de estado
