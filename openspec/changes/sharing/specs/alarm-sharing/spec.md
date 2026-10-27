## ADDED Requirements

### Requirement: Exportar la configuración de una alarma
El sistema SHALL permitir compartir la configuración de una alarma existente como enlace (deep link), código QR o archivo, usando en los tres canales el mismo contenido. La configuración SHALL incluir hora, minutos, días de repetición y etiqueta, y, cuando existan, el reto, el sonido (solo si es un sonido incluido en la app) y la política anti-trampa. La configuración SHALL NOT incluir el identificador interno de la alarma, su estado activo ni URIs externas del dispositivo.

#### Scenario: Compartir por enlace
- **WHEN** el usuario elige compartir una alarma como enlace
- **THEN** se abre la share sheet del sistema con un enlace que codifica la configuración

#### Scenario: Compartir por QR
- **WHEN** el usuario elige compartir una alarma como QR
- **THEN** se muestra un código QR que, al escanearse, entrega la misma configuración que el enlace

#### Scenario: Compartir por archivo
- **WHEN** el usuario elige compartir una alarma como archivo
- **THEN** se comparte un archivo `.apexalarm` con la misma configuración mediante la share sheet

#### Scenario: Sonido externo no exportado
- **WHEN** la alarma usa un sonido procedente de una URI externa
- **THEN** la configuración exportada no contiene esa URI y el receptor usará el sonido por defecto

### Requirement: Esquema versionado del contenido compartido
El contenido compartido SHALL incluir un número de versión de esquema. El sistema SHALL aceptar versiones que conoce, SHALL ignorar sin error los campos opcionales desconocidos de una versión compatible y SHALL rechazar con un mensaje claro una versión mayor que la soportada.

#### Scenario: Versión soportada
- **WHEN** se importa contenido con una versión de esquema soportada
- **THEN** se procesa y se muestra su vista previa

#### Scenario: Versión posterior no soportada
- **WHEN** se importa contenido con una versión de esquema mayor que la soportada
- **THEN** se rechaza e informa de que hay que actualizar la app

#### Scenario: Bloques opcionales ausentes
- **WHEN** el contenido no incluye reto, sonido o política anti-trampa
- **THEN** se importa con los valores por defecto de la app para esos aspectos

### Requirement: Recepción de contenido compartido
El sistema SHALL iniciar la importación al abrir un enlace de la app, un archivo `.apexalarm` o al escanear un QR de la app desde la propia app.

#### Scenario: Abrir un enlace
- **WHEN** el usuario abre un enlace de ApexApp desde otra aplicación
- **THEN** la app se abre en la pantalla de importación con ese contenido

#### Scenario: Abrir un archivo
- **WHEN** el usuario abre un archivo `.apexalarm` con la app
- **THEN** la app se abre en la pantalla de importación con ese contenido

### Requirement: Validación del contenido recibido
El sistema SHALL tratar el contenido recibido como no confiable y SHALL validarlo por completo antes de mostrarlo: tamaño máximo, estructura, rangos de hora (0–23) y minutos (0–59), días válidos sin duplicados, longitud máxima de etiqueta y valores de reto, sonido y anti-trampa dentro de los tipos y rangos conocidos. Si la validación falla, el sistema SHALL rechazar el contenido completo sin crear ni modificar ninguna alarma.

#### Scenario: Contenido malformado
- **WHEN** el contenido no es una configuración válida o está corrupto
- **THEN** se muestra un error y no se crea ninguna alarma

#### Scenario: Valores fuera de rango
- **WHEN** la hora es 25 o la etiqueta supera la longitud máxima
- **THEN** se rechaza el contenido completo

#### Scenario: Contenido demasiado grande
- **WHEN** el contenido supera el tamaño máximo permitido
- **THEN** se rechaza sin procesarlo

#### Scenario: Tipo de reto desconocido
- **WHEN** el contenido referencia un tipo de reto que la app no conoce
- **THEN** se rechaza o se sustituye por el reto por defecto indicándolo en la vista previa, sin ejecutar nada derivado del contenido

### Requirement: Vista previa y confirmación antes de importar
El sistema SHALL mostrar una vista previa legible de la alarma recibida (hora, días, etiqueta, reto, sonido y política anti-trampa) y SHALL crear la alarma únicamente cuando el usuario la acepte. La alarma importada SHALL crearse como nueva, con un identificador propio, y SHALL quedar desactivada hasta que el usuario la active. Cancelar SHALL descartar el contenido sin efectos.

#### Scenario: Aceptar importación
- **WHEN** el usuario acepta la vista previa
- **THEN** se crea una alarma nueva desactivada con esa configuración y aparece en la lista

#### Scenario: Rechazar importación
- **WHEN** el usuario cancela la vista previa
- **THEN** no se crea ninguna alarma

#### Scenario: Importar dos veces
- **WHEN** el usuario importa dos veces el mismo contenido
- **THEN** se crean dos alarmas independientes, solo si acepta ambas

#### Scenario: Sin programación automática
- **WHEN** se importa una alarma
- **THEN** no se programa ninguna alarma exacta hasta que el usuario la active
