# Spec Delta

## Purpose

Define el reto «Mantener y soltar»: el usuario mantiene pulsado un control hasta que una señal cambia y suelta dentro de la ventana correcta, durante un número configurable de rondas.

## ADDED Requirements

### Requirement: Superación del reto por rondas
El sistema SHALL superar el reto de mantener y soltar cuando el usuario complete correctamente el número de rondas configurado para la alarma, y SHALL mostrar el progreso (rondas hechas y totales).

#### Scenario: Todas las rondas acertadas
- **WHEN** el usuario suelta dentro de la ventana correcta en cada una de las rondas configuradas
- **THEN** el reto se supera

#### Scenario: Progreso visible
- **WHEN** el usuario completa una ronda
- **THEN** el progreso muestra una ronda más hecha sobre el total

### Requirement: Señal en instante aleatorio
El sistema SHALL cambiar la señal de cada ronda tras un tiempo de mantenimiento aleatorio dentro de un rango fijo, sorteado en cada sesión y en cada ronda, y SHALL NOT mostrar cuenta atrás ni otra pista del instante del cambio.

#### Scenario: Instantes distintos
- **WHEN** se generan dos sesiones del reto con semillas aleatorias distintas
- **THEN** los instantes de señal de sus rondas no coinciden en todas las rondas

#### Scenario: Instante dentro del rango
- **WHEN** se sortea el instante de la señal de una ronda
- **THEN** está entre el mínimo y el máximo definidos para el reto

### Requirement: Ventana de acierto
El sistema SHALL considerar acierto soltar desde el cambio de la señal hasta el fin de una ventana de tiempo cuya duración se estrecha al subir la dificultad, y SHALL medir los tiempos con un reloj monotónico.

#### Scenario: Soltar dentro de la ventana
- **WHEN** el usuario suelta tras el cambio de señal y antes de que termine la ventana
- **THEN** la ronda se da por acertada

#### Scenario: Soltar antes de la señal
- **WHEN** el usuario suelta antes de que cambie la señal
- **THEN** la ronda se da por fallada

#### Scenario: Soltar tarde
- **WHEN** el usuario suelta después de que termine la ventana
- **THEN** la ronda se da por fallada

#### Scenario: Cambio del reloj de pared
- **WHEN** la hora del dispositivo cambia durante una ronda
- **THEN** el resultado de la ronda no varía

### Requirement: Penalización por fallo sin reinicio total
El sistema SHALL, ante una ronda fallada, repetirla con un nuevo instante de señal y añadir una ronda extra al total hasta un máximo de rondas extra, y SHALL reiniciar solo la ronda en curso, sin ronda extra y conservando las superadas, al tercer fallo consecutivo en ella.

#### Scenario: Un fallo
- **WHEN** el usuario falla una ronda
- **THEN** el total de rondas sube en una, las rondas ya superadas se conservan y la ronda se repite con otro instante de señal

#### Scenario: Tope de rondas extra
- **WHEN** las rondas extra acumuladas alcanzan el máximo y el usuario vuelve a fallar
- **THEN** el total no aumenta más y la ronda se repite

#### Scenario: Tres fallos seguidos
- **WHEN** el usuario falla tres veces seguidas la misma ronda
- **THEN** se reinicia esa ronda con un nuevo sorteo, el tercer fallo no suma ronda extra y las rondas superadas se conservan

### Requirement: Rondas configurables por alarma
El sistema SHALL permitir configurar, en el editor de la alarma y para el tipo habilitado, el número de rondas (de 2 a 6, 3 por defecto), SHALL guardarlo con la alarma y SHALL usar el valor por defecto o acotado si falta o no es válido.

#### Scenario: Configurar rondas
- **WHEN** el usuario habilita el reto, fija 5 rondas y guarda la alarma
- **THEN** al reabrirla el editor muestra 5 rondas y el reto exige 5

#### Scenario: Parámetros ausentes o corruptos
- **WHEN** la alarma guarda parámetros vacíos o ilegibles
- **THEN** el reto usa 3 rondas sin error

#### Scenario: Parámetros fuera de rango
- **WHEN** la alarma guarda un número de rondas fuera de 2 a 6
- **THEN** el reto lo acota al límite más cercano

#### Scenario: Duración orientativa
- **WHEN** se usan los valores por defecto
- **THEN** la duración esperada del reto es de aproximadamente un minuto

### Requirement: Política anti-trampas heredada
El sistema SHALL NOT definir un nivel anti-trampas propio para este reto, SHALL aplicarle el lockdown y la integridad de sesión de la alarma según su nivel efectivo, y SHALL dar por fallada la ronda si la pantalla del reto pierde el foco o se cancela la pulsación.

#### Scenario: Sesión con salidas repetidas
- **WHEN** el usuario sale repetidamente de la pantalla de sonar durante el reto
- **THEN** la sesión se marca como sospechosa por los mismos motivos que con cualquier otro reto

#### Scenario: Pérdida de foco durante una ronda
- **WHEN** la pantalla del reto pierde el foco mientras el usuario mantiene pulsado
- **THEN** la ronda en curso se da por fallada

### Requirement: Validación de pulsaciones según el nivel anti-trampas
El sistema SHALL validar cada pulsación y suelta del reto con el validador táctil compartido según el nivel anti-trampas de la alarma, descartando las pulsaciones con la ventana superpuesta, de origen no táctil real o más rápidas que un humano.

#### Scenario: Pulsación con ventana superpuesta
- **WHEN** el toque llega con la ventana del reto cubierta por otra superposición
- **THEN** la pulsación no cuenta y no inicia ronda

#### Scenario: Evento de origen no táctil
- **WHEN** el evento de pulsación no procede de la pantalla táctil real
- **THEN** la pulsación se descarta

#### Scenario: Nivel OFF
- **WHEN** el nivel efectivo es `OFF`
- **THEN** no se descarta ninguna pulsación por plausibilidad

### Requirement: Disponibilidad y presencia en la configuración
El sistema SHALL ofrecer el reto como tipo seleccionable en el modo «Con retos» del editor sin requerir permisos ni sensores, y SHALL mostrar su nombre en los idiomas de la app.

#### Scenario: Selección en el editor
- **WHEN** el usuario cambia a «Con retos» y habilita «Mantener y soltar»
- **THEN** aparece su editor de rondas y la alarma puede guardarse

#### Scenario: Dispositivo sin sensores
- **WHEN** el dispositivo carece de acelerómetro, sensor de pasos o permisos opcionales
- **THEN** el reto sigue disponible
