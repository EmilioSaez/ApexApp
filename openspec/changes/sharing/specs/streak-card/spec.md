## ADDED Requirements

### Requirement: Generación de la tarjeta de racha
El sistema SHALL generar una imagen de tarjeta de racha con la racha actual y la mejor racha, obtenidas de las estadísticas de despertar (capability `wake-stats`). La imagen SHALL usar el tema visual de la app y tener un tamaño fijo adecuado para chats y redes. La tarjeta SHALL NOT incluir datos personales distintos de las estadísticas mostradas (sin etiquetas de alarma ni horas exactas).

#### Scenario: Tarjeta con racha
- **WHEN** el usuario pide la tarjeta y tiene una racha activa
- **THEN** se genera una imagen con racha actual y mejor racha

#### Scenario: Sin racha todavía
- **WHEN** el usuario no tiene despertares registrados
- **THEN** se informa de que aún no hay logros que compartir y no se genera imagen

#### Scenario: Datos actualizados
- **WHEN** las estadísticas cambian mientras se muestra la vista previa
- **THEN** la vista previa refleja los valores actualizados

### Requirement: Compartir la tarjeta
El sistema SHALL permitir compartir la tarjeta generada mediante la share sheet del sistema, entregando la imagen con un acceso temporal de solo lectura y sin exponer otros archivos de la app.

#### Scenario: Compartir por chat
- **WHEN** el usuario pulsa compartir en la tarjeta
- **THEN** se abre la share sheet con la imagen adjunta y la aplicación elegida puede leerla

#### Scenario: Acceso limitado
- **WHEN** una aplicación receptora intenta leer otros archivos de la app
- **THEN** el acceso es denegado

#### Scenario: Cancelar compartir
- **WHEN** el usuario cierra la share sheet sin elegir destino
- **THEN** no se envía nada y la tarjeta sigue disponible
