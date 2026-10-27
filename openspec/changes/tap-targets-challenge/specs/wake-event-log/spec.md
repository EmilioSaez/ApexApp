## ADDED Requirements

### Requirement: Sesión sospechosa por entrada sintética
El sistema SHALL registrar el evento «sospechosa» de una sesión marcada por entrada táctil sintética, de modo que se excluya de las consultas que piden omitir sesiones sospechosas, igual que cualquier otro motivo.

#### Scenario: Excluir sesión marcada por toques sintéticos
- **WHEN** una sesión terminó marcada por entrada sintética y se piden las últimas sesiones excluyendo sospechosas
- **THEN** esa sesión no se devuelve
