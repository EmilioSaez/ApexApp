# anti-cheat-policy Specification

## Purpose
Define la política anti-trampas configurable por alarma, el cálculo del nivel efectivo según las capacidades del dispositivo y la pantalla donde se explican y conceden los permisos delicados.

## Requirements

### Requirement: Nivel anti-trampas por alarma
El sistema SHALL permitir elegir para cada alarma un nivel anti-trampas entre `OFF`, `STANDARD` y `STRICT`, y SHALL guardarlo dentro de la política anti-trampas de la alarma.

#### Scenario: Alarma nueva en el editor
- **WHEN** se crea una alarma desde el editor sin cambiar el nivel
- **THEN** su nivel anti-trampas es `STANDARD`

#### Scenario: Cambiar el nivel
- **WHEN** el usuario cambia una alarma a `STRICT` y la guarda
- **THEN** la alarma conserva el nivel `STRICT` al volver a abrirla

### Requirement: Política ausente permisiva
El sistema SHALL tratar una alarma sin política anti-trampas guardada como nivel `OFF` y sin snooze, y SHALL ignorar las claves desconocidas de la política.

#### Scenario: Alarma existente
- **WHEN** se carga una alarma guardada antes de este cambio
- **THEN** conserva sus datos y se comporta con nivel `OFF`

#### Scenario: Clave desconocida
- **WHEN** la política guardada contiene una clave que este cambio no define
- **THEN** la política se lee sin error

### Requirement: Nivel efectivo
El sistema SHALL calcular el nivel efectivo de cada alarma a partir del nivel pedido y de las capacidades disponibles, y SHALL aplicar el efectivo al sonar.

#### Scenario: Capacidad insuficiente
- **WHEN** una alarma pide `STRICT` y no hay propietario del dispositivo
- **THEN** el nivel efectivo es el máximo alcanzable y se muestra junto a la alarma

#### Scenario: Capacidad suficiente
- **WHEN** una alarma pide `STANDARD` y las capacidades necesarias están concedidas
- **THEN** el nivel efectivo es `STANDARD`

### Requirement: Aviso de capacidades que faltan
El sistema SHALL avisar en el editor, mientras el nivel de la alarma sea `STANDARD` o `STRICT`, de los permisos o capacidades que faltan, y SHALL ofrecer abrir la pantalla de estado para concederlos.

#### Scenario: Falta el permiso de superposición
- **WHEN** el usuario elige el nivel `STANDARD` sin permiso de superposición
- **THEN** el editor indica el permiso que falta y ofrece abrir la pantalla de estado

### Requirement: Pantalla de estado de capacidades
El sistema SHALL ofrecer una pantalla que lista cada capacidad delicada (superposición, administrador de dispositivo, propietario del dispositivo), para qué se usa, si está concedida y cómo concederla, y SHALL mostrar también el estado de pantalla completa y notificaciones que ya gestiona la fiabilidad de la alarma.

#### Scenario: Consultar estado
- **WHEN** el usuario abre la pantalla de estado
- **THEN** ve el estado de cada capacidad con su explicación y los límites declarados

#### Scenario: Conceder un permiso
- **WHEN** el usuario pulsa conceder en una capacidad pendiente
- **THEN** la app abre el ajuste del sistema correspondiente y actualiza el estado al volver
