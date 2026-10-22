## ADDED Requirements

### Requirement: Estado de fiabilidad
El sistema SHALL calcular un estado de fiabilidad que incluya: alarmas exactas permitidas, notificaciones habilitadas, canal de alarma no silenciado, permiso de pantalla completa (API 34+) y app excluida de la optimización de batería, distinguiendo comprobaciones críticas de recomendadas.

#### Scenario: Todo correcto
- **WHEN** todas las comprobaciones se cumplen
- **THEN** el estado se considera fiable y no se muestra ningún aviso

#### Scenario: Falta una comprobación crítica
- **WHEN** falta el permiso de alarmas exactas o las notificaciones están deshabilitadas
- **THEN** el estado se marca como no fiable e identifica qué falta

#### Scenario: Comprobación recomendada
- **WHEN** solo falta la exclusión de batería o el permiso de pantalla completa
- **THEN** el estado se marca como mejorable, distinto del crítico

### Requirement: Avisos guiados
El sistema SHALL mostrar una pantalla de configuración que lista cada comprobación pendiente con su explicación y una acción que abre el ajuste del sistema correspondiente, y SHALL actualizar el estado al volver a la app.

#### Scenario: Abrir el ajuste
- **WHEN** el usuario pulsa la acción de una comprobación pendiente
- **THEN** se abre el ajuste del sistema correspondiente o el más cercano disponible

#### Scenario: Regreso tras conceder
- **WHEN** el usuario vuelve a la app tras conceder un permiso
- **THEN** la comprobación se muestra como cumplida sin acción adicional

### Requirement: Aviso persistente en Inicio
El sistema SHALL mostrar en la pantalla de inicio un aviso visible mientras el estado no sea fiable, con acceso directo a la configuración, y SHALL comprobar el estado al abrir la app y al activar una alarma.

#### Scenario: Aviso crítico
- **WHEN** el estado es no fiable
- **THEN** Inicio muestra un aviso destacado que lleva a la configuración

#### Scenario: Activar con permisos incompletos
- **WHEN** el usuario activa una alarma con el estado no fiable
- **THEN** la alarma se guarda, se indica qué falta y la activación no se bloquea

### Requirement: Guías por fabricante
El sistema SHALL detectar el fabricante del dispositivo y mostrar instrucciones de autoarranque y ahorro de batería para los fabricantes con restricciones conocidas, abriendo la pantalla de ajustes del fabricante cuando exista y los ajustes de la aplicación en caso contrario.

#### Scenario: Fabricante con restricciones
- **WHEN** el fabricante es uno con optimización agresiva conocida
- **THEN** se muestran los pasos específicos de ese fabricante

#### Scenario: Pantalla del fabricante no disponible
- **WHEN** no se puede resolver la actividad de ajustes del fabricante
- **THEN** se abren los ajustes generales de la aplicación sin que la app falle

#### Scenario: Confirmación del usuario
- **WHEN** el usuario indica que ya completó los pasos del fabricante
- **THEN** la guía se marca como hecha y deja de avisar, dado que el sistema no permite verificarlo

### Requirement: Prueba de alarma
El sistema SHALL ofrecer una alarma de prueba que sigue el camino real de disparo para que el usuario verifique que suena en su dispositivo.

#### Scenario: Lanzar prueba
- **WHEN** el usuario pulsa probar alarma en la configuración
- **THEN** se programa una alarma exacta a pocos segundos que dispara el servicio de sonado igual que una real
