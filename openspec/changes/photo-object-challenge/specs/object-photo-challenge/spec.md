# Spec Delta

## Purpose

Define el reto de foto a un objeto: el usuario registra un objeto de su entorno y, para apagar la alarma, debe enfocarlo en vivo con la cámara, con reconocimiento local y verificación contra imágenes falsas.

## ADDED Requirements

### Requirement: Registro del objeto
El sistema SHALL permitir al usuario elegir, desde una vista previa de cámara, una de las etiquetas que el reconocedor detecta en ese momento, y SHALL guardar solo esa etiqueta como objeto de la alarma.

#### Scenario: Elegir etiqueta
- **WHEN** el usuario apunta la cámara a un objeto y selecciona una de las etiquetas detectadas
- **THEN** la etiqueta queda asociada al reto de la alarma

#### Scenario: Sin etiqueta elegida
- **WHEN** el usuario intenta guardar la alarma con este reto sin haber elegido etiqueta
- **THEN** no se guarda la alarma y se indica que falta el objeto

### Requirement: Superación del reto
El sistema SHALL superar el reto cuando la etiqueta registrada se detecte con una confianza mínima durante una ventana continua de fotogramas, y SHALL reiniciar el progreso si la etiqueta desaparece.

#### Scenario: Objeto correcto enfocado
- **WHEN** el usuario enfoca el objeto registrado de forma continua durante el tiempo requerido
- **THEN** el reto se supera

#### Scenario: Objeto distinto
- **WHEN** la cámara enfoca un objeto cuya etiqueta no coincide
- **THEN** el progreso no avanza

#### Scenario: Varios retos de foto
- **WHEN** la sesión incluye N retos de foto
- **THEN** el usuario debe superar N verificaciones, una foto en vivo por reto, del objeto registrado

#### Scenario: Objeto fuera de encuadre
- **WHEN** el objeto sale del encuadre antes de completar la ventana
- **THEN** el progreso se reinicia

### Requirement: Verificación en vivo
El sistema SHALL rechazar la detección cuando la escena sea una imagen estática o una pantalla, aplicando la misma verificación de vida que el reto de código QR, y SHALL informar al usuario del rechazo.

#### Scenario: Foto de una foto
- **WHEN** el usuario enfoca una fotografía impresa del objeto sin variación natural de la escena
- **THEN** el reto no se supera y se muestra un aviso

#### Scenario: Imagen en pantalla
- **WHEN** el usuario enfoca la imagen del objeto en otra pantalla
- **THEN** el reto no se supera y se muestra un aviso

### Requirement: Privacidad
El sistema SHALL procesar las imágenes únicamente en el dispositivo y SHALL NOT almacenar ni transmitir fotogramas ni fotografías.

#### Scenario: Reconocimiento sin red
- **WHEN** el dispositivo no tiene conexión
- **THEN** el registro y la verificación del objeto funcionan igual y no se envía ninguna imagen

### Requirement: Disponibilidad
El sistema SHALL considerar el reto no disponible cuando falte la cámara o su permiso, y SHALL solicitar el permiso con una explicación antes de abrir la cámara.

#### Scenario: Permiso denegado
- **WHEN** el permiso de cámara está denegado al sonar la alarma
- **THEN** el reto se declara no disponible y la sesión recurre a la reserva

#### Scenario: Explicación del permiso
- **WHEN** el usuario configura el reto sin permiso de cámara
- **THEN** se muestra por qué se necesita antes de pedirlo
