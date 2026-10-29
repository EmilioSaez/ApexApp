package app.luxion.apexapp.domain.challenge.gridmemory

import app.luxion.apexapp.domain.challenge.Challenge
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.SessionConfig
import javax.inject.Inject
import kotlin.random.Random

/**
 * Lado del tablero, casillas a recordar y milisegundos de exposición en cada nivel (índice 0 = nivel 1). Tabla
 * única: ajustar aquí al calibrar. Tablero 3×3→6×6, 3→10 casillas, exposición 1500→1100 ms.
 */
object GridMemoryLevels {
    val SIDE_BY_LEVEL: List<Int> = listOf(3, 4, 4, 5, 6)
    val COUNT_BY_LEVEL: List<Int> = listOf(3, 4, 6, 8, 10)
    val SHOW_MS_BY_LEVEL: List<Long> = listOf(1500L, 1500L, 1400L, 1200L, 1100L)

    private fun index(level: Int) =
        level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY) - SessionConfig.MIN_DIFFICULTY

    fun sideFor(level: Int): Int = SIDE_BY_LEVEL[index(level)]

    fun countFor(level: Int): Int = COUNT_BY_LEVEL[index(level)]

    fun showMsFor(level: Int): Long = SHOW_MS_BY_LEVEL[index(level)]
}

/**
 * Conjunto de casillas (0 until side²) que se iluminan; se supera con un [ChallengeAnswer.Pattern] cuyas casillas,
 * sin importar el orden, sean exactamente esas.
 */
class GridMemoryInstance(
    override val level: Int,
    val side: Int,
    val cells: Set<Int>,
    val showMs: Long,
) : ChallengeInstance {
    override val type: ChallengeType = GridMemoryChallenge.TYPE
    override val prompt: String = "grid_memory:$side:${cells.size}"

    /** Un fallo vuelve a mostrar el mismo patrón: no se regenera ni se facilita. */
    override val keepOnFailure: Boolean = true

    fun isTarget(cell: Int): Boolean = cell in cells

    override fun check(answer: ChallengeAnswer): Boolean =
        answer is ChallengeAnswer.Pattern &&
            answer.type == type &&
            answer.taps.size == cells.size &&
            answer.taps.toSet() == cells
}

class GridMemoryChallenge @Inject constructor() : Challenge {
    override val type: ChallengeType = TYPE

    override fun generate(level: Int, params: String, random: Random): ChallengeInstance {
        // Los params no se usan: tablero, cantidad y ritmo salen solo del nivel (también el adaptativo).
        val side = GridMemoryLevels.sideFor(level)
        val cells = (0 until side * side).shuffled(random).take(GridMemoryLevels.countFor(level)).toSet()
        return GridMemoryInstance(
            level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY),
            side,
            cells,
            GridMemoryLevels.showMsFor(level),
        )
    }

    companion object {
        val TYPE = ChallengeType("grid_memory")
    }
}
