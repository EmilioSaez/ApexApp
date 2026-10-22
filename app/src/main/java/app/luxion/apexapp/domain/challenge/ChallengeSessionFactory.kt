package app.luxion.apexapp.domain.challenge

import javax.inject.Inject

/** Crea sesiones con los ganchos inyectados (registro, proveedor de dificultad y receptor de eventos). */
class ChallengeSessionFactory @Inject constructor(
    private val registry: ChallengeRegistry,
    private val difficultyProvider: DifficultyProvider,
    private val eventSink: ChallengeEventSink,
    private val passedAlarms: PassedAlarms,
) {
    val registeredTypes: Set<ChallengeType> get() = registry.types

    fun create(config: SessionConfig, sessionId: String, alarmId: Long? = null): ChallengeSession {
        // Se envuelve el receptor para registrar las alarmas superadas sin depender del binding de eventos.
        val sink = ChallengeEventSink { event ->
            if (event is ChallengeEvent.SessionFinished && event.alarmId != null) {
                passedAlarms.markPassed(event.alarmId)
            }
            eventSink.onEvent(event)
        }
        return ChallengeSession(sessionId, alarmId, config, registry, difficultyProvider, sink)
    }
}
