## ADDED Requirements

### Requirement: Lectura de la hora
El sistema SHALL, si la alarma tiene la lectura de hora activada, leer en voz alta la hora actual en el idioma del dispositivo cuando la alarma empieza a sonar.

#### Scenario: Leer la hora
- **WHEN** suena una alarma con lectura de hora activada
- **THEN** se anuncia la hora actual por voz

### Requirement: Recordatorio hablado
El sistema SHALL leer el texto de recordatorio configurado en la alarma, tras la hora si ambas están activas.

#### Scenario: Recordatorio configurado
- **WHEN** suena una alarma con un recordatorio de texto
- **THEN** el texto se lee en voz alta

### Requirement: Repetición de la voz
El sistema SHALL repetir el anuncio cada intervalo configurado mientras la alarma siga sonando, si la repetición está activada.

#### Scenario: Alarma sin atender
- **WHEN** la alarma sigue sonando pasado el intervalo de repetición
- **THEN** el anuncio se vuelve a leer

### Requirement: Convivencia de voz y sonido
El sistema SHALL reproducir la voz por el stream de alarma y SHALL bajar temporalmente el volumen del sonido mientras habla, para que se entienda.

#### Scenario: Voz sobre el sonido
- **WHEN** se lee un anuncio mientras suena el tono
- **THEN** el tono baja de volumen durante el anuncio y lo recupera después

### Requirement: Fallo de TTS
El sistema SHALL continuar con sonido y vibración sin error si el motor TTS no está disponible, falla o el idioma no está soportado.

#### Scenario: Sin motor TTS
- **WHEN** suena una alarma con voz y no hay motor TTS operativo
- **THEN** la alarma suena y vibra normalmente sin voz
