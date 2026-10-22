package app.luxion.apexapp.domain.challenge

import javax.inject.Inject

/** Crea sesiones con los ganchos inyectados (registro, proveedor de dificultad y receptor de eventos). */
class ChallengeSessionFactory @Inject constructor(
    private val registry: ChallengeRegistry,
    private val difficultyProvider: DifficultyProvider,
    private val eventSink: ChallengeEventSink,
) {
    val registeredTypes: Set<ChallengeType> get() = registry.types

    fun create(config: SessionConfig, sessionId: String, alarmId: Long? = null): ChallengeSession =
        ChallengeSession(sessionId, alarmId, config, registry, difficultyProvider, eventSink)
}
