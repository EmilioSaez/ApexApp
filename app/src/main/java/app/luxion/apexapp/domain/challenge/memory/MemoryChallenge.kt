package app.luxion.apexapp.domain.challenge.memory

import app.luxion.apexapp.domain.challenge.Challenge
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.SessionConfig
import javax.inject.Inject
import kotlin.random.Random

/**
 * Longitud de la secuencia y milisegundos por casilla en cada nivel (índice 0 = nivel 1). Tabla única: ajustar
 * aquí al calibrar. Longitud 3→7, ritmo 700→400 ms.
 */
object MemoryLevels {
    const val GRID_SIZE = 9

    val LENGTH_BY_LEVEL: List<Int> = listOf(3, 4, 5, 6, 7)
    val STEP_MS_BY_LEVEL: List<Long> = listOf(700L, 600L, 500L, 450L, 400L)

    private fun index(level: Int) =
        level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY) - SessionConfig.MIN_DIFFICULTY

    fun lengthFor(level: Int): Int = LENGTH_BY_LEVEL[index(level)]

    fun stepMsFor(level: Int): Long = STEP_MS_BY_LEVEL[index(level)]
}

/** Secuencia de casillas (0..8) a repetir; se supera con un [ChallengeAnswer.Pattern] idéntico. */
class MemoryInstance(
    override val level: Int,
    val sequence: List<Int>,
    val stepMs: Long,
) : ChallengeInstance {
    override val type: ChallengeType = MemoryChallenge.TYPE
    override val prompt: String = "memory:${sequence.size}"

    /** Un fallo repite la misma secuencia: no se regenera ni se facilita. */
    override val keepOnFailure: Boolean = true

    override fun check(answer: ChallengeAnswer): Boolean =
        answer is ChallengeAnswer.Pattern && answer.type == type && answer.taps == sequence
}

class MemoryChallenge @Inject constructor() : Challenge {
    override val type: ChallengeType = TYPE

    override fun generate(level: Int, params: String, random: Random): ChallengeInstance {
        // Los params no se usan: la longitud y el ritmo salen solo del nivel (también el adaptativo).
        val sequence = List(MemoryLevels.lengthFor(level)) { random.nextInt(MemoryLevels.GRID_SIZE) }
        return MemoryInstance(
            level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY),
            sequence,
            MemoryLevels.stepMsFor(level),
        )
    }

    companion object {
        val TYPE = ChallengeType("memory")
    }
}
