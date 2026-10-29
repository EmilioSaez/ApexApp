# Spec Delta

## Purpose

Define el reto del laberinto: el usuario debe llevar una ficha con el icono de alarma desde la entrada hasta la salida de un laberinto haciendo swipes en las cuatro direcciones, como forma de comprobar que está despierto y atento.

## ADDED Requirements

### Requirement: Reto de recorrer un laberinto
El sistema SHALL ofrecer el tipo de reto "laberinto", que muestra un laberinto con una única solución, una ficha con el icono de alarma en la entrada y una salida, y se supera cuando la ficha llega a la salida mediante swipes del usuario.

#### Scenario: Recorrido correcto
- **WHEN** el usuario hace swipes que llevan la ficha hasta la salida
- **THEN** el reto se supera

#### Scenario: Deslizar hasta un muro
- **WHEN** el usuario hace un swipe norte, sur, oeste o este
- **THEN** la ficha se mueve animada, celda a celda, en esa dirección hasta tener un muro delante o llegar a la salida

#### Scenario: Swipe contra un muro
- **WHEN** hay un muro inmediatamente delante de la ficha en la dirección del swipe
- **THEN** la ficha no se mueve y no cuenta como fallo

#### Scenario: Swipe durante la animación
- **WHEN** el usuario hace un swipe mientras la ficha aún se está moviendo
- **THEN** el swipe se ignora

### Requirement: Laberinto determinista con solución única
El sistema SHALL generar el laberinto a partir de una semilla de modo que la misma semilla y tamaño produzcan siempre el mismo laberinto, sin ciclos y con un único camino entre la entrada y la salida, y SHALL generar uno nuevo e independiente cada vez que se crea una instancia del reto.

#### Scenario: Misma semilla
- **WHEN** se genera dos veces un laberinto con la misma semilla y tamaño
- **THEN** ambos tienen las mismas paredes y la misma solución

#### Scenario: Solución única
- **WHEN** se genera un laberinto
- **THEN** todas las celdas son alcanzables desde la entrada y solo existe un camino sin repeticiones hasta la salida

### Requirement: Dificultad y tamaño del laberinto
El sistema SHALL determinar el tamaño del laberinto únicamente a partir del nivel de dificultad (1..5) efectivo de la sesión mediante una tabla única, de modo que un nivel mayor nunca produzca un laberinto con menos celdas.

#### Scenario: Nivel mínimo y máximo
- **WHEN** se genera el reto en el nivel 1 y en el nivel 5
- **THEN** el laberinto del nivel 5 tiene estrictamente más celdas que el del nivel 1

#### Scenario: Parámetros guardados
- **WHEN** la alarma guarda `params` con un tamaño propio
- **THEN** se ignoran y manda el nivel de dificultad

#### Scenario: Nivel fuera de rango
- **WHEN** se solicita un nivel inferior a 1 o superior a 5
- **THEN** se acota al rango válido

### Requirement: Laberinto tocable en pantallas pequeñas
El sistema SHALL dibujar el laberinto con celdas de al menos 40 dp y paredes de 4 dp (pasillo libre de al menos 36 dp) y SHALL limitar el tamaño máximo a 7 columnas por 9 filas, de modo que el laberinto del nivel más alto quepa en una pantalla de 320 dp de ancho con márgenes de 20 dp.

#### Scenario: Pantalla estrecha
- **WHEN** el ancho disponible es menor que el necesario para celdas de 40 dp
- **THEN** la celda no se reduce por debajo de 40 dp

#### Scenario: Pantalla ancha
- **WHEN** el espacio disponible permite celdas mayores
- **THEN** la celda crece hasta un máximo de 64 dp

### Requirement: Disponibilidad sin hardware
El sistema SHALL tratar el reto del laberinto como siempre disponible, sin requerir sensores ni permisos.

#### Scenario: Dispositivo sin sensores
- **WHEN** el dispositivo no tiene acelerómetro, sensor de pasos ni cámara
- **THEN** el reto del laberinto sigue estando disponible y seleccionable en el editor

### Requirement: Entrada táctil fiable y anti-trampas
El sistema SHALL consumir los eventos del puntero del laberinto para que el desplazamiento de la pantalla no interrumpa el swipe, y SHALL aplicar la política anti-trampas táctil de la alarma al apoyo inicial del dedo.

#### Scenario: Swipe dentro de una pantalla desplazable
- **WHEN** el usuario desliza verticalmente sobre el laberinto
- **THEN** el gesto mueve la ficha y no desplaza la pantalla

#### Scenario: Toque rechazado por anti-trampas
- **WHEN** el nivel anti-trampas es estándar y el apoyo inicial no procede de la pantalla táctil o la ventana está tapada
- **THEN** el swipe no se procesa y se avisa al usuario

### Requirement: Selección y compartición del tipo
El sistema SHALL permitir añadir el reto del laberinto a una alarma desde el editor de retos y mostrarlo con nombre localizado en las listas y en la importación de alarmas compartidas.

#### Scenario: Añadir al editor
- **WHEN** el usuario abre el editor de retos de una alarma
- **THEN** el reto del laberinto aparece entre los tipos seleccionables

#### Scenario: Alarma importada
- **WHEN** se importa una alarma compartida que incluye el reto del laberinto
- **THEN** el reto se muestra con su nombre localizado
