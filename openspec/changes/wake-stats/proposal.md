# Proposal

## Why

ApexApp obliga a despertar mediante retos, pero hoy no guarda nada de lo que ocurre cuando suena una alarma: el usuario no puede ver si mejora, cuántas veces pospone o falla, ni mantener una racha. Además, `adaptive-difficulty` necesita un historial por reto y `sharing` necesita una racha que mostrar. Ambos dependen de un registro de eventos fiable que debe existir antes y con un modelo pensado para ellos.

## What Changes

- Nuevo registro persistente (Room) de eventos por cada ocurrencia de alarma: sonó, snooze, fallo de reto, reto completado/alarma descartada. Cada evento pertenece a una *sesión de despertar* (una ocurrencia que suena) e incluye el tipo de reto y la dificultad cuando aplica.
- API de dominio `WakeEventRecorder` para que otros changes (alarm-core, challenge-engine, anti-cheat) registren eventos sin conocer Room, y `WakeHistoryRepository` de solo lectura para consumidores (adaptive-difficulty, sharing).
- Derivación pura de sesiones y métricas a partir de los eventos: tiempo hasta despertar, snoozes y fallos por sesión, puntualidad y rachas.
- Registro de horas de sueño: hora de acostarse manual por noche, o estimada a partir de una hora habitual configurable cuando no hay entrada manual; horas dormidas = hora de despertar − hora de acostarse.
- Pantallas de estadísticas con gráficas (dibujadas con Canvas de Compose, sin dependencia nueva): tiempo en despertar, snoozes y fallos, puntualidad y rachas, horas de sueño; con selector de rango (7 / 30 / 90 días) y entrada de hora de acostarse.
- Nueva ruta de navegación a Estadísticas desde la pantalla inicial.
- `AlarmFiringHandler` registra el evento «sonó» al disparar (único punto de código existente que se toca; el resto de eventos los emiten los changes propietarios del flujo de despertar mediante la API).
- Migración de Room aditiva desde la versión vigente en el momento del merge (solo tablas nuevas, sin tocar `alarms` ni `sounds`); este change no fija el número de versión (lo fija `alarm-core`).

Fuera de alcance: exportar/compartir estadísticas (change `sharing`), lógica de dificultad (`adaptive-difficulty`), UI de la pantalla de alarma sonando y reto (`alarm-core`, `challenge-engine`), detección de trampas (`anti-cheat`), sistema de diseño (`design-system-stitch`).

## Capabilities

### New Capabilities
- `wake-event-log`: persistencia y API del registro de eventos por sesión de despertar (sonó, snooze, fallo, completado) y lectura reactiva del historial para otros módulos.
- `wake-statistics`: cálculo de métricas (tiempo hasta despertar, snoozes, fallos, puntualidad, rachas) y las pantallas con gráficas que las muestran.
- `sleep-tracking`: hora de acostarse manual/estimada, horas dormidas y su visualización.

### Modified Capabilities
- `alarm-storage`: la base de datos incorpora las tablas de eventos y sueño; los eventos sobreviven al borrado de la alarma.
- `alarm-scheduling`: al dispararse una alarma se registra el evento «sonó».
- `app-shell`: el grafo de navegación incluye las pantallas de estadísticas, accesibles desde la pantalla inicial.

## Impact

- Código: `data/local` (nuevas entidades/DAOs, `ApexDatabase` con migración aditiva a la versión siguiente + schema JSON), `domain` (modelos, recorder, repositorio, calculadores puros), `di/DataModule`, `alarm/AlarmFiringHandler`, `ui/navigation/AppNavigation`, `ui/home` (acceso a estadísticas), nuevo paquete `ui/stats`.
- Contrato hacia otros changes: `WakeEventRecorder`, `WakeHistoryRepository`, `StreakCalculator`/`WakeSession` (estables; ver design.md).
- Sin dependencias nuevas ni permisos nuevos. Todo local.
