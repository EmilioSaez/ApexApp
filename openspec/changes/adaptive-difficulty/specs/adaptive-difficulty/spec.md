# adaptive-difficulty Specification

## ADDED Requirements

### Requirement: Opt-in por alarma
El sistema SHALL ofrecer la dificultad adaptativa como ajuste independiente por alarma, desactivado por defecto. Con el ajuste desactivado, el sistema SHALL usar siempre la dificultad fija configurada para la alarma.

#### Scenario: Alarma nueva
- **WHEN** se crea una alarma
- **THEN** la dificultad adaptativa está desactivada y el reto usa la dificultad configurada

#### Scenario: Ajuste desactivado
- **WHEN** suena una alarma con la dificultad adaptativa desactivada
- **THEN** el nivel del reto es la dificultad fija de la alarma, sea cual sea su historial

### Requirement: Nivel base y límites
El sistema SHALL calcular el nivel adaptativo tomando como nivel inicial la dificultad configurada de la alarma, y SHALL mantenerlo siempre entre un nivel mínimo y un nivel máximo por alarma, ambos dentro de los niveles que ofrece `challenge-engine`. El mínimo SHALL ser menor o igual que el máximo.

#### Scenario: Primera activación
- **WHEN** el usuario activa la dificultad adaptativa en una alarma
- **THEN** el nivel actual es la dificultad configurada, ajustada al rango mínimo-máximo

#### Scenario: Límite superior
- **WHEN** el algoritmo decide subir y el nivel actual ya es el máximo
- **THEN** el nivel se mantiene en el máximo

#### Scenario: Límite inferior
- **WHEN** el algoritmo decide bajar y el nivel actual ya es el mínimo
- **THEN** el nivel se mantiene en el mínimo

#### Scenario: Rango inválido
- **WHEN** el usuario intenta fijar un mínimo mayor que el máximo
- **THEN** el sistema rechaza el cambio y conserva el rango anterior

### Requirement: Evaluación por historial reciente
El sistema SHALL evaluar el ajuste tras cada sesión de alarma finalizada usando los eventos de `wake-stats` de las últimas 5 sesiones de esa alarma, y SHALL NOT ajustar el nivel hasta disponer de al menos 3 sesiones. Cada sesión se resume en tres señales: número de snoozes, número de intentos fallidos y tiempo hasta despertar (desde que suena hasta completar el reto). Como ese tiempo incluye los snoozes, SHALL usarse solo en sesiones sin snoozes.

#### Scenario: Historial insuficiente
- **WHEN** la alarma tiene menos de 3 sesiones registradas
- **THEN** el nivel no cambia

#### Scenario: Sesiones antiguas
- **WHEN** hay más de 5 sesiones registradas
- **THEN** solo las 5 más recientes influyen en el cálculo

### Requirement: Reglas de subida y bajada
El sistema SHALL cambiar el nivel como máximo un nivel por sesión finalizada, subiéndolo ante somnolencia o un reto demasiado fácil y bajándolo ante bloqueo. Si se cumplen a la vez condiciones de subida y de bajada, SHALL prevalecer la bajada.

#### Scenario: Subida por snoozes
- **WHEN** 2 de las últimas 3 sesiones tuvieron al menos un snooze y no se cumple la condición de bajada
- **THEN** el nivel sube un nivel, hasta el máximo

#### Scenario: Subida por reto fácil
- **WHEN** las últimas 3 sesiones se resolvieron sin snoozes ni fallos y con un tiempo hasta despertar inferior a 30 s cada una
- **THEN** el nivel sube un nivel, hasta el máximo

#### Scenario: Bajada por fallos
- **WHEN** 2 de las últimas 3 sesiones tuvieron 3 o más intentos fallidos, o, sin snoozes, un tiempo hasta despertar superior a 120 s
- **THEN** el nivel baja un nivel, hasta el mínimo

#### Scenario: Señales contradictorias
- **WHEN** se cumplen a la vez una condición de subida y una de bajada
- **THEN** el nivel baja un nivel

#### Scenario: Estabilidad tras un cambio
- **WHEN** el nivel acaba de cambiar
- **THEN** las sesiones anteriores al cambio no cuentan para el siguiente ajuste, de modo que el siguiente cambio requiere nuevas sesiones

### Requirement: Sesiones que no cuentan
El sistema SHALL ignorar para el ajuste las sesiones sin resultado fiable: alarmas ignoradas o descartadas sin reto, sesiones marcadas como sospechosas por `anti-cheat`, y alarmas desactivadas durante la sesión.

#### Scenario: Alarma no atendida
- **WHEN** una sesión termina sin que el usuario llegue a intentar el reto
- **THEN** esa sesión no se incluye en el historial evaluado

### Requirement: Aplicación del nivel al reto
El sistema SHALL proporcionar el nivel calculado a `challenge-engine` antes de crear el reto de cada disparo. El nivel SHALL fijarse al inicio de la sesión y no cambiar durante ella.

#### Scenario: Disparo con adaptativa activa
- **WHEN** suena una alarma con la dificultad adaptativa activa
- **THEN** el reto se crea con el nivel adaptativo actual

#### Scenario: Datos no disponibles
- **WHEN** no se puede leer el historial de `wake-stats`
- **THEN** se usa el último nivel guardado, o la dificultad configurada si no hay ninguno, y la alarma suena igualmente

### Requirement: Persistencia local del estado
El sistema SHALL guardar por alarma el ajuste activo/inactivo, el rango mínimo-máximo, el nivel actual y el motivo y fecha del último cambio, y SHALL eliminar ese estado cuando se elimina la alarma. Los datos SHALL permanecer solo en el dispositivo.

#### Scenario: Persistencia tras reinicio
- **WHEN** se cierra y reabre la app
- **THEN** el estado adaptativo de cada alarma se conserva

#### Scenario: Alarma eliminada
- **WHEN** se elimina una alarma
- **THEN** su estado adaptativo también se elimina

### Requirement: Explicación al usuario
El sistema SHALL explicar el comportamiento en el editor de la alarma con un texto breve junto al ajuste, SHALL mostrar el nivel actual y el motivo del último cambio, y SHALL avisar en la pantalla del reto cuando el nivel ha cambiado respecto a la sesión anterior.

#### Scenario: Texto del ajuste
- **WHEN** el usuario ve el ajuste en el editor
- **THEN** se explica que la dificultad sube si pospone la alarma o resuelve todo muy fácil, y baja si falla mucho, dentro de los límites elegidos

#### Scenario: Motivo del cambio
- **WHEN** el nivel ha cambiado
- **THEN** el editor muestra el nivel actual y una frase con el motivo, por ejemplo "Subida a 4: pospusiste 2 de las últimas 3 veces"

#### Scenario: Aviso en el reto
- **WHEN** el reto se muestra con un nivel distinto al de la sesión anterior
- **THEN** una línea breve indica el cambio y su motivo

### Requirement: Control del usuario
El sistema SHALL permitir al usuario desactivar la dificultad adaptativa en cualquier momento y reiniciar el aprendizaje de una alarma. Desactivarla SHALL restaurar la dificultad fija configurada; reiniciar SHALL volver al nivel base y descartar el historial previo para el cálculo.

#### Scenario: Desactivar
- **WHEN** el usuario desactiva el ajuste
- **THEN** los siguientes disparos usan la dificultad fija configurada

#### Scenario: Reiniciar
- **WHEN** el usuario reinicia el aprendizaje
- **THEN** el nivel vuelve al base y las sesiones anteriores no influyen en ajustes futuros
