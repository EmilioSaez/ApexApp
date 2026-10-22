## ADDED Requirements

### Requirement: Selección de sonido
El sistema SHALL permitir elegir en el editor de alarma entre el tono por defecto, un tono del sistema y un archivo de audio local seleccionado con el selector de documentos del sistema (SAF).

#### Scenario: Elegir tono del sistema
- **WHEN** el usuario elige un tono del sistema
- **THEN** la alarma queda asociada a ese tono

#### Scenario: Elegir archivo local
- **WHEN** el usuario selecciona un archivo de audio
- **THEN** la app conserva el permiso de lectura persistente y asocia el archivo a la alarma

#### Scenario: Archivo no audio
- **WHEN** el usuario selecciona un archivo que no es audio
- **THEN** se rechaza y se informa al usuario

### Requirement: Ajustes de rampa, vibración y voz
El sistema SHALL permitir configurar en el editor, por alarma, la duración de la rampa (o desactivarla), la vibración (activar y patrón) y la voz (leer hora, recordatorio, repetición).

#### Scenario: Configurar y guardar
- **WHEN** el usuario cambia estos ajustes y guarda la alarma
- **THEN** la alarma conserva los ajustes elegidos

#### Scenario: Valores por defecto
- **WHEN** el usuario crea una alarma nueva
- **THEN** usa tono por defecto, rampa moderada, vibración activada y voz desactivada

### Requirement: Previsualización
El sistema SHALL permitir probar el sonido, la vibración y la voz configurados desde el editor, y SHALL detener la prueba al salir de la pantalla o al terminar.

#### Scenario: Probar sonido
- **WHEN** el usuario pulsa probar
- **THEN** se reproduce la configuración actual y puede detenerse

#### Scenario: Salir del editor
- **WHEN** el usuario sale del editor durante una prueba
- **THEN** la prueba se detiene
