package app.luxion.apexapp.domain.challenge.flappy

import app.luxion.apexapp.domain.challenge.Challenge
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.SessionConfig
import javax.inject.Inject
import kotlin.random.Random

/**
 * Puntos (tuberías superadas) que hay que sumar de una tirada, por nivel de dificultad (índice 0 = nivel 1).
 * Tabla única: ajustar aquí al calibrar.
 */
object FlappyTargets {
    val BY_LEVEL: List<Int> = listOf(3, 5, 8, 12, 16)

    /** Puntos para el nivel [level] (1..5, se acota). Los `params` guardados se ignoran. */
    fun targetFor(level: Int): Int =
        BY_LEVEL[level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY) - SessionConfig.MIN_DIFFICULTY]
}

/** Instancia del reto Flappy: se supera con [ChallengeAnswer.GameCompleted] de su tipo. */
class FlappyInstance(
    override val level: Int,
    /** Puntos que hay que sumar sin chocar. */
    val target: Int,
) : ChallengeInstance {
    override val type: ChallengeType = FlappyChallenge.TYPE
    override val prompt: String = "flappy:$target"

    override fun check(answer: ChallengeAnswer): Boolean =
        answer is ChallengeAnswer.GameCompleted && answer.type == type
}

class FlappyChallenge @Inject constructor() : Challenge {
    override val type: ChallengeType = TYPE

    override fun generate(level: Int, params: String, random: Random): ChallengeInstance =
        FlappyInstance(level, FlappyTargets.targetFor(level))

    companion object {
        val TYPE = ChallengeType("flappy")
    }
}
