package app.luxion.apexapp.domain.challenge

import javax.inject.Inject

/** Contexto que recibe el [DifficultyProvider] para decidir el nivel de un reto. */
data class ChallengeContext(
    val sessionId: String,
    val alarmId: Long?,
    val type: ChallengeType,
    /** Posición del reto en la sesión, desde 0. */
    val index: Int,
    /** Fallos previos en este reto. */
    val failures: Int,
)

/** Decide el nivel (1..5) de cada reto. `adaptive-difficulty` sustituye la implementación por defecto. */
interface DifficultyProvider {
    fun levelFor(config: SessionConfig, context: ChallengeContext): Int
}

class FixedDifficultyProvider @Inject constructor() : DifficultyProvider {
    override fun levelFor(config: SessionConfig, context: ChallengeContext): Int = config.difficulty
}

sealed interface ChallengeEvent {
    val sessionId: String
    val alarmId: Long?
    val timestampMillis: Long

    data class SessionStarted(
        override val sessionId: String,
        override val alarmId: Long?,
        override val timestampMillis: Long,
        val total: Int,
        val difficulty: Int,
    ) : ChallengeEvent

    data class ChallengeStarted(
        override val sessionId: String,
        override val alarmId: Long?,
        override val timestampMillis: Long,
        val type: ChallengeType,
        val level: Int,
        val index: Int,
    ) : ChallengeEvent

    data class ChallengeFailed(
        override val sessionId: String,
        override val alarmId: Long?,
        override val timestampMillis: Long,
        val type: ChallengeType,
        val level: Int,
        val index: Int,
    ) : ChallengeEvent

    data class ChallengeSolved(
        override val sessionId: String,
        override val alarmId: Long?,
        override val timestampMillis: Long,
        val type: ChallengeType,
        val level: Int,
        val index: Int,
    ) : ChallengeEvent

    /** Un reto no posible se sustituyó por [to]. */
    data class ChallengeFellBack(
        override val sessionId: String,
        override val alarmId: Long?,
        override val timestampMillis: Long,
        val from: ChallengeType,
        val to: ChallengeType,
        val reason: String,
        val index: Int,
    ) : ChallengeEvent

    data class SessionFinished(
        override val sessionId: String,
        override val alarmId: Long?,
        override val timestampMillis: Long,
        val total: Int,
    ) : ChallengeEvent
}

/** Receptor de eventos de la sesión. `wake-stats` sustituye la implementación por defecto. */
fun interface ChallengeEventSink {
    fun onEvent(event: ChallengeEvent)
}

class NoOpChallengeEventSink @Inject constructor() : ChallengeEventSink {
    override fun onEvent(event: ChallengeEvent) = Unit
}
