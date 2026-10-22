# wake-statistics Specification

## Purpose
Calcular y mostrar métricas de despertar (tiempo hasta despertar, snoozes, fallos, puntualidad y racha) sobre rangos de 7, 30 y 90 días, con estado vacío y descripciones accesibles.

## Requirements

### Requirement: Métricas de despertar
El sistema SHALL calcular, para un rango de fechas, el tiempo medio hasta despertar, el total y la media de snoozes por sesión, el total y la media de fallos de reto por sesión, y el número de sesiones completadas y no completadas.

#### Scenario: Tiempo hasta despertar
- **WHEN** una sesión sonó a las 07:00:00 y se completó a las 07:02:30
- **THEN** su tiempo hasta despertar es 2 minutos y 30 segundos y entra en la media del rango

#### Scenario: Rango sin sesiones
- **WHEN** el rango seleccionado no contiene sesiones
- **THEN** las métricas se muestran como vacías y no como cero ni error

### Requirement: Puntualidad
El sistema SHALL considerar una sesión puntual cuando se completa dentro de 10 minutos desde su instante programado, y SHALL mostrar el porcentaje de sesiones puntuales del rango.

#### Scenario: Sesión puntual
- **WHEN** una alarma programada a las 07:00 se completa a las 07:08
- **THEN** la sesión cuenta como puntual

#### Scenario: Sesión tardía
- **WHEN** una alarma programada a las 07:00 se completa a las 07:15
- **THEN** la sesión no cuenta como puntual

### Requirement: Rachas
El sistema SHALL calcular la racha actual y la mejor racha en días. Un día SHALL contar para la racha cuando todas sus sesiones son puntuales; un día con alguna sesión no puntual o no completada SHALL romperla; un día sin sesiones SHALL NOT sumar ni romperla.

#### Scenario: Racha continúa
- **WHEN** hay tres días consecutivos con sesiones puntuales
- **THEN** la racha actual es 3

#### Scenario: Día fallido
- **WHEN** un día tiene una sesión no completada
- **THEN** la racha actual vuelve a 0 y se conserva la mejor racha

#### Scenario: Día sin alarma
- **WHEN** un día no tiene ninguna sesión entre dos días puntuales
- **THEN** la racha no se rompe ni suma ese día

### Requirement: Pantallas de estadísticas con gráficas
El sistema SHALL ofrecer una pantalla de estadísticas con gráficas de tiempo hasta despertar, snoozes y fallos, y puntualidad y rachas, con selector de rango de 7, 30 y 90 días. La pantalla SHALL mostrar un estado vacío cuando no hay datos, SHALL funcionar en modo claro y oscuro y SHALL ofrecer descripción accesible del valor de cada gráfica.

#### Scenario: Cambiar de rango
- **WHEN** el usuario selecciona 30 días
- **THEN** las métricas y gráficas se recalculan para los últimos 30 días

#### Scenario: Sin datos
- **WHEN** no existe ninguna sesión
- **THEN** se muestra un estado vacío que explica que las estadísticas aparecerán tras la primera alarma

### Requirement: Datos de racha reutilizables
El sistema SHALL exponer la racha actual y la mejor racha como flujo consultable por otros módulos, calculadas con las mismas reglas que la pantalla.

#### Scenario: Consumidor externo
- **WHEN** otro módulo observa la racha y se completa una sesión puntual
- **THEN** recibe el valor actualizado, igual al mostrado en la pantalla
