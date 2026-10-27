## REMOVED Requirements

### Requirement: Modo de tarea bloqueada
**Reason**: el nivel `STRICT`, único que lo usaba, se elimina.
**Migration**: ninguna; las alarmas con `STRICT` pasan a `STANDARD`.

### Requirement: Degradación sin privilegios
**Reason**: sin `STRICT` no hay nivel inalcanzable por falta de propietario del dispositivo.
**Migration**: ninguna.
