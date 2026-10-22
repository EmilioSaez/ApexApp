# alarm-management Specification

## Purpose
Permite al usuario crear, editar, borrar y activar o desactivar sus alarmas desde la interfaz, manteniendo la programación sincronizada con lo guardado.

## Requirements

### Requirement: Crear una alarma
El sistema SHALL permitir crear una alarma indicando hora, días de repetición opcionales y etiqueta opcional, y SHALL programarla al guardarla.

#### Scenario: Crear desde la lista
- **WHEN** el usuario pulsa añadir, elige una hora y guarda
- **THEN** vuelve a la lista, donde aparece la nueva alarma activa y programada

#### Scenario: Alarma sin días
- **WHEN** el usuario guarda una alarma sin días de repetición
- **THEN** la alarma suena una sola vez en la próxima ocurrencia de esa hora

#### Scenario: Cancelar la creación
- **WHEN** el usuario sale del editor sin guardar
- **THEN** no se crea ninguna alarma

### Requirement: Editar una alarma
El sistema SHALL permitir abrir una alarma existente desde la lista, modificar sus datos y reprogramarla al guardar.

#### Scenario: Cambiar la hora
- **WHEN** el usuario abre una alarma, cambia la hora y guarda
- **THEN** la lista muestra la nueva hora y la alarma queda programada para ella

#### Scenario: Editar una alarma inactiva
- **WHEN** el usuario guarda cambios de una alarma inactiva
- **THEN** sigue inactiva y no se programa

### Requirement: Borrar una alarma
El sistema SHALL permitir borrar una alarma desde la lista y SHALL ofrecer deshacer el borrado durante unos segundos.

#### Scenario: Borrar
- **WHEN** el usuario borra una alarma
- **THEN** desaparece de la lista y se cancela su programación

#### Scenario: Deshacer
- **WHEN** el usuario pulsa deshacer tras borrar
- **THEN** la alarma vuelve a la lista con los mismos datos y se reprograma si estaba activa

### Requirement: Información de próxima ocurrencia
El sistema SHALL mostrar al guardar una alarma activa cuánto falta para que suene.

#### Scenario: Aviso al guardar
- **WHEN** el usuario guarda una alarma activa
- **THEN** la app muestra el tiempo restante hasta su próxima ocurrencia
