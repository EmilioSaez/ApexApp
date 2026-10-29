## MODIFIED Requirements

### Requirement: Registro de eventos por sesión de despertar
El sistema SHALL registrar eventos de cada ocurrencia de una alarma, agrupados en una sesión de despertar. Los tipos de evento SHALL ser: sonó, snooze, fallo de reto, completado, perdida, sospechosa, verificada y verificación fallida. Cada evento SHALL guardar su instante, la sesión a la que pertenece y la alarma de origen. Los eventos de reto SHALL guardar además el tipo de reto y su dificultad cuando se conozcan. Los eventos «verificada» y «verificación fallida» SHALL añadirse a la sesión que se había resuelto, SHALL NOT cambiar su estado ni sus contadores y SHALL NOT crear sesiones nuevas.

#### Scenario: Una alarma suena
- **WHEN** suena una alarma
- **THEN** se crea una sesión nueva con un evento «sonó» que guarda el instante programado y el instante real

#### Scenario: Snooze
- **WHEN** el usuario pospone la alarma durante una sesión
- **THEN** se añade un evento «snooze» a esa sesión

#### Scenario: Fallo de reto
- **WHEN** el usuario falla un reto durante una sesión
- **THEN** se añade un evento «fallo de reto» a esa sesión con el tipo de reto y la dificultad

#### Scenario: Completado
- **WHEN** el usuario completa el reto y la alarma se detiene
- **THEN** se añade un evento «completado» a esa sesión y la sesión queda cerrada

#### Scenario: Verificación confirmada
- **WHEN** el usuario confirma que está despierto
- **THEN** se añade un evento «verificada» a la sesión resuelta y su estado no cambia

#### Scenario: Verificación fallida
- **WHEN** vence el plazo de confirmación sin respuesta
- **THEN** se añade un evento «verificación fallida» a la sesión resuelta y el nuevo sonado es una sesión aparte
