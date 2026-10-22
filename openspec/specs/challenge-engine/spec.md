# challenge-engine Specification

## Purpose
Definir el contrato de los retos, su registro, las sesiones (encadenadas o aleatorias), la dificultad configurable y los eventos que se registran en el historial, de modo que cada tipo de reto se añada sin tocar el motor.

## Requirements

### Requirement: Contrato de reto
El sistema SHALL definir un contrato común de reto: cada reto tiene un tipo identificable, genera una instancia a partir de un nivel de dificultad (entero de 1 a 5) y de parámetros opacos opcionales propios del tipo, y valida la respuesta del usuario como correcta o incorrecta.

#### Scenario: Instancia según dificultad
- **WHEN** se pide a un reto una instancia para un nivel de dificultad
- **THEN** se obtiene una instancia con su enunciado y un modo de validar la respuesta acorde a ese nivel

#### Scenario: Respuesta incorrecta
- **WHEN** el usuario entrega una respuesta incorrecta a una instancia
- **THEN** la validación la rechaza y la instancia sigue sin superarse

### Requirement: Registro de tipos de reto
El sistema SHALL mantener un registro de los tipos de reto disponibles, de modo que se puedan añadir nuevos tipos sin modificar el motor de sesiones.

#### Scenario: Tipo registrado disponible
- **WHEN** un tipo de reto está en el registro
- **THEN** puede activarse en la configuración de una alarma y ser elegido por una sesión

#### Scenario: Tipo desconocido en la configuración
- **WHEN** la configuración de una alarma activa un tipo que no está en el registro
- **THEN** la sesión ignora ese tipo y usa los restantes

### Requirement: Sesión de retos
El sistema SHALL ejecutar una sesión de retos a partir de una configuración (dificultad, número de retos, modo, tipos activados en orden y parámetros por tipo) y de un identificador de sesión recibido de quien la lanza y SHALL darla por superada solo cuando se hayan resuelto correctamente todos los retos solicitados.

#### Scenario: Sesión superada
- **WHEN** el usuario resuelve correctamente el último reto de la sesión
- **THEN** la sesión pasa a estado superado

#### Scenario: Avance al siguiente reto
- **WHEN** el usuario resuelve correctamente un reto y quedan más
- **THEN** se presenta el siguiente reto y el progreso refleja los retos resueltos

#### Scenario: Reto fallido
- **WHEN** el usuario falla un reto
- **THEN** la sesión no avanza y se genera una nueva instancia del mismo reto

#### Scenario: Sin tipos disponibles
- **WHEN** la configuración no tiene ningún tipo de reto utilizable
- **THEN** la sesión usa el reto de matemáticas como tipo por defecto

### Requirement: Modos de ejecución encadenada y aleatoria
El sistema SHALL soportar dos modos de selección: encadenado, que recorre en orden los tipos activados y repite el ciclo si hay más retos que tipos, y aleatorio, que elige cada reto al azar entre los tipos activados.

#### Scenario: Modo encadenado
- **WHEN** se inicia una sesión encadenada con los tipos A y B y tres retos
- **THEN** los retos se presentan en el orden A, B, A

#### Scenario: Modo aleatorio
- **WHEN** se inicia una sesión aleatoria con varios tipos activados
- **THEN** cada reto es de uno de los tipos activados

### Requirement: Dificultad configurable
El sistema SHALL aceptar una dificultad entera de 1 a 5 y un número de retos entre 1 y 10 para una sesión. Una dificultad fuera de rango SHALL acotarse al rango, una dificultad sin definir (0) SHALL interpretarse como 3 y un número de retos fuera de rango SHALL rechazarse.

#### Scenario: Número de retos fuera de rango
- **WHEN** se solicita una sesión con 0 o más de 10 retos
- **THEN** la configuración se rechaza como inválida

#### Scenario: Dificultad fuera de rango
- **WHEN** se solicita una sesión con dificultad 9
- **THEN** se usa la dificultad 5

#### Scenario: Dificultad sin definir
- **WHEN** la alarma no define dificultad (0)
- **THEN** se usa la dificultad 3

#### Scenario: Mayor nivel, mayor exigencia
- **WHEN** se generan instancias del mismo reto en nivel 1 y en nivel 5
- **THEN** la instancia de nivel 5 es más exigente según el criterio propio del reto

### Requirement: Configuración de sesión a partir de la alarma
El sistema SHALL construir la configuración de sesión desde la configuración de retos de la alarma (tipos con sus parámetros, dificultad y, si existen, número de retos y modo) y SHALL aplicar valores por defecto (1 reto, modo encadenado, sin parámetros) a lo que la alarma no defina.

#### Scenario: Alarma con solo tipos y dificultad
- **WHEN** se crea una sesión desde una alarma que solo define tipos y dificultad
- **THEN** la sesión tiene 1 reto en modo encadenado

### Requirement: Parámetros opacos por tipo
El sistema SHALL transportar sin interpretar los parámetros propios de cada tipo de reto, asociados a la alarma, y SHALL entregarlos únicamente a ese tipo al generar sus instancias.

#### Scenario: Parámetros entregados al tipo
- **WHEN** una sesión genera un reto de un tipo que tiene parámetros
- **THEN** ese tipo recibe exactamente sus parámetros y los demás tipos no los reciben

### Requirement: Reserva cuando un reto no es posible
El sistema SHALL sustituir un reto que se declare no disponible o que no pueda completarse (por ejemplo, por falta de cámara, sensor o permiso) por otro tipo habilitado y disponible o, si no hay ninguno, por el reto de matemáticas al mismo nivel, y SHALL emitir un evento de reserva.

#### Scenario: Reto no disponible con otro tipo habilitado
- **WHEN** el siguiente reto declara que no está disponible y hay otro tipo habilitado y disponible
- **THEN** se presenta un reto de ese otro tipo y se emite el evento de reserva

#### Scenario: Reto no disponible sin alternativa
- **WHEN** el siguiente reto declara que no está disponible y no hay otro tipo utilizable
- **THEN** se presenta un reto de matemáticas y se emite el evento de reserva

### Requirement: Proveedor de dificultad sustituible
El sistema SHALL obtener el nivel (1 a 5) de cada reto de un proveedor de dificultad, con una implementación por defecto que devuelve el nivel base configurado en la alarma, de modo que pueda sustituirse por una estrategia adaptativa.

#### Scenario: Proveedor por defecto
- **WHEN** no hay ningún proveedor alternativo
- **THEN** cada reto usa el nivel configurado en la alarma

### Requirement: Eventos de sesión y de reto
El sistema SHALL emitir eventos al iniciar y finalizar una sesión y al iniciar, resolver, fallar o sustituir cada reto, incluyendo el identificador de sesión recibido, tipo, nivel, marca de tiempo e identificador de alarma cuando exista, hacia un receptor sustituible que por defecto no hace nada.

#### Scenario: Eventos emitidos
- **WHEN** se completa una sesión con un fallo y un acierto
- **THEN** el receptor recibe inicio de sesión, inicio de reto, fallo, acierto y fin de sesión, en ese orden

#### Scenario: Sin receptor configurado
- **WHEN** no hay un receptor de eventos propio
- **THEN** la sesión funciona con normalidad sin registrar nada

### Requirement: Registro de fallos y de fin de sesión en el historial de despertar
El sistema SHALL registrar en el historial de despertar cada fallo de reto, con tipo y nivel, y el fin de una sesión superada, con identificador de sesión, de alarma y marca de tiempo. Las sesiones sin alarma, como las de la pantalla de prueba, y las de la alarma de prueba SHALL NOT registrarse. Un fallo al registrar SHALL NOT afectar a la sesión ni a los registros siguientes, que se entregan en orden.

#### Scenario: Fallo y acierto registrados
- **WHEN** una sesión de una alarma tiene dos fallos y se supera
- **THEN** el historial recibe, en orden, dos fallos de reto con su tipo y nivel y un completado, todos con el identificador de sesión y de alarma

#### Scenario: Sesión de prueba
- **WHEN** se ejecuta una sesión desde la pantalla de prueba, sin alarma, o con la alarma de prueba
- **THEN** no se registra ningún evento

#### Scenario: Fallo del historial
- **WHEN** el historial falla al guardar un evento
- **THEN** la sesión continúa y los eventos siguientes se guardan

### Requirement: Completado de una alarma sin retos
El sistema SHALL registrar el completado de una alarma que suena cuando se resuelve, también si no tenía retos, y SHALL NOT registrarlo cuando termina por posponer o por tiempo. El registro es idempotente por sesión.

#### Scenario: Alarma sin retos descartada
- **WHEN** una alarma sin retos se resuelve
- **THEN** se registra su completado

#### Scenario: Pospuesta o por tiempo
- **WHEN** una alarma termina por posponer o por agotar el tiempo
- **THEN** no se registra completado

### Requirement: Notificación de fin de sesión a oyentes adicionales
El sistema SHALL permitir que varios módulos se registren como oyentes del fin de una sesión, y SHALL notificarlos una vez por sesión superada, después de entregar el evento de fin de sesión al receptor de eventos, con el identificador de sesión y el de alarma cuando exista. El fallo de un oyente no SHALL impedir que se notifique a los demás ni afectar a la sesión.

#### Scenario: Orden de notificación
- **WHEN** se supera una sesión y hay un receptor de eventos y un oyente registrados
- **THEN** el receptor recibe primero el evento de fin de sesión y después el oyente recibe el identificador de sesión y de alarma

#### Scenario: Oyente que falla
- **WHEN** un oyente falla al ser notificado
- **THEN** los demás oyentes se notifican igualmente y la sesión sigue superada

#### Scenario: Sesión sin superar
- **WHEN** una sesión no llega a superarse
- **THEN** no se notifica a ningún oyente

### Requirement: Pantalla de prueba de retos
El sistema SHALL ofrecer una pantalla accesible desde la app que permita configurar dificultad (1 a 5), número de retos, modo y tipos y ejecutar una sesión completa sin que exista ni se dispare ninguna alarma.

#### Scenario: Probar una sesión
- **WHEN** el usuario configura una sesión en la pantalla de prueba y la inicia
- **THEN** se ejecutan los retos y, al superarlos, se muestra el resultado y se puede volver a la pantalla anterior

#### Scenario: Sin efectos sobre alarmas
- **WHEN** se ejecuta una sesión desde la pantalla de prueba
- **THEN** no se crea, modifica ni programa ninguna alarma
