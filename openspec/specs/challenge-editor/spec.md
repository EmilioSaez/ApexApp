# challenge-editor Specification

## Purpose
Permitir configurar en el editor de alarmas los retos de cada alarma y sus parámetros por tipo, con secciones extensibles para otros módulos.

## Requirements

### Requirement: Sección de retos en el editor de alarmas
El sistema SHALL mostrar en el editor de alarmas una sección «Retos» que permita elegir los tipos de reto habilitados (entre los registrados), el número de retos (1 a 10), la dificultad (1 a 5) y el modo (encadenado o aleatorio), y SHALL guardar esa elección en la configuración de retos de la alarma.

#### Scenario: Configurar los retos de una alarma
- **WHEN** el usuario habilita un tipo, fija 3 retos, dificultad 4 y modo aleatorio, y guarda la alarma
- **THEN** al reabrir la alarma el editor muestra esos mismos valores

#### Scenario: Valores iniciales de una alarma nueva
- **WHEN** se abre el editor para una alarma nueva
- **THEN** la sección muestra 1 reto, dificultad 3, modo encadenado y ningún tipo habilitado

#### Scenario: Alarma sin tipos habilitados
- **WHEN** el usuario guarda una alarma sin ningún tipo habilitado
- **THEN** la alarma se guarda sin retos y se puede descartar de inmediato

#### Scenario: Orden de la cadena
- **WHEN** el usuario habilita primero el tipo B y después el tipo A
- **THEN** el modo encadenado los presenta en el orden B, A

### Requirement: Editor de parámetros por tipo de reto
El sistema SHALL permitir que cada tipo de reto aporte su propio editor de parámetros dentro de la sección de retos, que se muestra solo mientras el tipo está habilitado, y que valide sus parámetros antes de guardar.

#### Scenario: Editor del tipo habilitado
- **WHEN** el usuario habilita un tipo que aporta editor de parámetros
- **THEN** el editor de ese tipo aparece en la sección con sus parámetros actuales

#### Scenario: Parámetros inválidos bloquean el guardado
- **WHEN** un tipo habilitado declara inválidos sus parámetros (por ejemplo, un reto de QR sin código registrado)
- **THEN** la alarma no se puede guardar hasta que sean válidos

#### Scenario: Tipo sin editor
- **WHEN** el usuario habilita un tipo que no aporta editor de parámetros
- **THEN** no se muestra ningún campo extra y sus parámetros no bloquean el guardado

### Requirement: Secciones extensibles del editor
El sistema SHALL componer el editor de alarmas con secciones aportadas por distintos módulos, mostradas en un orden determinista, de modo que añadir una sección no requiera modificar el editor. Cada sección SHALL poder leer y modificar la alarma en edición y declararla válida o no antes de guardar.

#### Scenario: Orden de las secciones
- **WHEN** hay varias secciones registradas
- **THEN** se muestran ordenadas por su prioridad, y a igual prioridad por un orden estable

#### Scenario: Sección inválida
- **WHEN** alguna sección declara inválida la alarma en edición
- **THEN** no se puede guardar la alarma

#### Scenario: Cambios de una sección
- **WHEN** una sección modifica la alarma en edición y el usuario guarda
- **THEN** la alarma guardada incluye esos cambios y conserva los campos que ninguna sección tocó
