package app.luxion.apexapp.domain.challenge.shapes

import app.luxion.apexapp.domain.challenge.Challenge
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.SessionConfig
import javax.inject.Inject
import kotlin.random.Random

/** Formas posibles; el `ordinal` es el identificador que viaja en [ChallengeAnswer.Pattern.taps]. */
enum class ShapeKind { CIRCLE, SQUARE, TRIANGLE, DIAMOND, CROSS, HEXAGON }

/**
 * Longitud de la secuencia, formas distintas y milisegundos por forma en cada nivel (índice 0 = nivel 1). Tabla
 * única: ajustar aquí al calibrar. Longitud 3→7, formas 3→6, 1200→700 ms por forma.
 */
object ShapeSequenceLevels {
    val LENGTH_BY_LEVEL: List<Int> = listOf(3, 4, 5, 6, 7)
    val DISTINCT_BY_LEVEL: List<Int> = listOf(3, 3, 4, 5, 6)
    val SHAPE_MS_BY_LEVEL: List<Long> = listOf(1200L, 1050L, 900L, 800L, 700L)

    private fun index(level: Int) =
        level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY) - SessionConfig.MIN_DIFFICULTY

    fun lengthFor(level: Int): Int = LENGTH_BY_LEVEL[index(level)]

    fun distinctFor(level: Int): Int = DISTINCT_BY_LEVEL[index(level)]

    fun shapeMsFor(level: Int): Long = SHAPE_MS_BY_LEVEL[index(level)]
}

/**
 * Secuencia de formas a repetir y paleta de formas entre las que elegir; se supera con un
 * [ChallengeAnswer.Pattern] cuyos `taps` son los `ordinal` de la secuencia en el mismo orden.
 */
class ShapeSequenceInstance(
    override val level: Int,
    val sequence: List<ShapeKind>,
    val palette: List<ShapeKind>,
    val shapeMs: Long,
) : ChallengeInstance {
    override val type: ChallengeType = ShapeSequenceChallenge.TYPE
    override val prompt: String = "shapes:${sequence.size}"

    /** Un fallo repite la misma secuencia: no se regenera ni se facilita. */
    override val keepOnFailure: Boolean = true

    private val expected = sequence.map { it.ordinal }

    override fun check(answer: ChallengeAnswer): Boolean =
        answer is ChallengeAnswer.Pattern && answer.type == type && answer.taps == expected
}

class ShapeSequenceChallenge @Inject constructor() : Challenge {
    override val type: ChallengeType = TYPE

    override fun generate(level: Int, params: String, random: Random): ChallengeInstance {
        // Los params no se usan: longitud, formas y ritmo salen solo del nivel (también el adaptativo).
        val palette = ShapeKind.entries.shuffled(random).take(ShapeSequenceLevels.distinctFor(level)).sorted()
        val sequence = List(ShapeSequenceLevels.lengthFor(level)) { palette[random.nextInt(palette.size)] }
        return ShapeSequenceInstance(
            level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY),
            sequence,
            palette,
            ShapeSequenceLevels.shapeMsFor(level),
        )
    }

    companion object {
        val TYPE = ChallengeType("shapes")
    }
}
