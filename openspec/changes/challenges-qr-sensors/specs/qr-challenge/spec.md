# Spec Delta

## Purpose

Define el reto de código QR o de barras: el usuario registra un código físico para una alarma y solo puede apagarla escaneando ese mismo código en vivo con la cámara.

## ADDED Requirements

### Requirement: Registro del código de la alarma
El sistema SHALL permitir registrar, al configurar una alarma con reto QR, un código QR o de barras escaneado con la cámara, y SHALL asociarlo a esa alarma. La alarma SHALL poder tener un único código registrado.

#### Scenario: Registrar un código
- **WHEN** el usuario escanea un código válido en la configuración del reto
- **THEN** el código queda asociado a la alarma y se muestra una confirmación con el tipo de código

#### Scenario: Sustituir el código
- **WHEN** el usuario registra un código nuevo en una alarma que ya tenía uno
- **THEN** el código anterior deja de ser válido para esa alarma

#### Scenario: Alarma sin código registrado
- **WHEN** el usuario intenta guardar una alarma con reto QR sin código registrado
- **THEN** la app impide guardarla y le pide registrar un código

### Requirement: Resolución del reto por escaneo
El sistema SHALL dar el reto QR por superado solo cuando la cámara lea en vivo un código idéntico al registrado en la alarma que está sonando.

#### Scenario: Código correcto
- **WHEN** suena la alarma y el usuario escanea el código registrado
- **THEN** el reto se supera

#### Scenario: Código distinto
- **WHEN** el usuario escanea un código diferente al registrado
- **THEN** el reto no se supera y se indica que el código no coincide

### Requirement: Verificación anti foto-de-foto
El sistema SHALL rechazar lecturas que procedan de una imagen estática del código, como una foto impresa o una pantalla, y SHALL exigir que el código se lea de forma continua durante varios fotogramas con variación natural de la escena.

#### Scenario: Escaneo en vivo del objeto real
- **WHEN** el usuario apunta la cámara al código físico y lo mantiene en el encuadre el tiempo mínimo exigido
- **THEN** la lectura se acepta

#### Scenario: Imagen estática del código
- **WHEN** la cámara lee un código desde una foto o pantalla cuyos fotogramas no presentan variación de escena
- **THEN** la lectura se rechaza y se pide escanear el objeto real

#### Scenario: Código perdido durante la lectura
- **WHEN** el código sale del encuadre antes de completar el tiempo mínimo
- **THEN** la verificación se reinicia

### Requirement: Permiso de cámara
El sistema SHALL solicitar el permiso de cámara solo al registrar o escanear un código, y SHALL guiar al usuario cuando esté denegado. Si falta el permiso al sonar la alarma, el reto SHALL poder resolverse por el mecanismo de reserva que defina `challenge-engine`.

#### Scenario: Permiso pendiente al registrar
- **WHEN** el usuario abre el registro del código sin permiso de cámara
- **THEN** la app solicita el permiso antes de mostrar la cámara

#### Scenario: Permiso denegado de forma permanente
- **WHEN** el usuario ha denegado el permiso con "no volver a preguntar"
- **THEN** la app explica el motivo y ofrece abrir los ajustes de la aplicación

#### Scenario: Cámara no disponible con la alarma sonando
- **WHEN** suena una alarma con reto QR y la cámara no está disponible o sin permiso
- **THEN** el reto delega en el mecanismo de reserva del motor de retos en lugar de bloquear la alarma indefinidamente
