package app.luxion.apexapp.domain.challenge

import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject

/** Crea sesiones con los ganchos inyectados (registro, proveedor de dificultad y receptor de eventos). */
class ChallengeSessionFactory @Inject constructor(
    private val registry: ChallengeRegistry,
    private val difficultyProvider: DifficultyProvider,
    private val eventSink: ChallengeEventSink,
    private val passedAlarms: PassedAlarms,
    private val finishedListeners: Set<@JvmSuppressWildcards SessionFinishedListener> = emptySet(),
) {
    val registeredTypes: Set<ChallengeType> get() = registry.types

    fun create(config: SessionConfig, sessionId: String, alarmId: Long? = null): ChallengeSession {
        // Se envuelve el receptor para registrar las alarmas superadas sin depender del binding de eventos.
        val sink = ChallengeEventSink { event ->
            if (event is ChallengeEvent.SessionFinished && event.alarmId != null) {
                passedAlarms.markPassed(event.alarmId)
            }
            eventSink.onEvent(event)
            if (event is ChallengeEvent.SessionFinished) notifyFinished(event)
        }
        return ChallengeSession(sessionId, alarmId, config, registry, difficultyProvider, sink)
    }

    /** Cada oyente va aislado: si uno falla no se corta a los demás ni a la sesión. */
    private fun notifyFinished(event: ChallengeEvent.SessionFinished) {
        finishedListeners.forEach { listener ->
            try {
                listener.onFinished(event.sessionId, event.alarmId)
            } catch (e: Exception) {
                logger.log(Level.WARNING, "SessionFinishedListener failed", e)
            }
        }
    }

    private companion object {
        val logger: Logger = Logger.getLogger("ChallengeSessionFactory")
    }
}
