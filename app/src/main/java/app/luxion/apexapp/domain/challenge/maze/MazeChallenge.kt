package app.luxion.apexapp.domain.challenge.maze

import app.luxion.apexapp.domain.challenge.Challenge
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.MazeTrace
import app.luxion.apexapp.domain.challenge.SessionConfig
import javax.inject.Inject
import kotlin.random.Random

/** Un laberinto a resolver; se supera con un [MazeTrace] válido de la entrada a la salida. */
class MazeInstance(
    override val level: Int,
    val seed: Long,
    val maze: Maze,
) : ChallengeInstance {
    override val type: ChallengeType = MazeChallenge.TYPE
    override val prompt: String = "maze:${maze.cols}x${maze.rows}:$seed"

    /** Un fallo (pared) reinicia el trazo sobre el mismo laberinto: no se regenera ni cambia la dificultad. */
    override val keepOnFailure: Boolean = true

    override fun check(answer: ChallengeAnswer): Boolean {
        if (answer !is MazeTrace || answer.type != type) return false
        val cells = answer.cells
        if (cells.firstOrNull() != maze.start || cells.last() != maze.exit) return false
        return cells.zipWithNext().all { (a, b) ->
            a in 0 until maze.cellCount && b in 0 until maze.cellCount && maze.isOpen(a, b)
        }
    }
}

class MazeChallenge @Inject constructor() : Challenge {
    override val type: ChallengeType = TYPE

    override fun generate(level: Int, params: String, random: Random): ChallengeInstance {
        // Los params no se usan: el tamaño sale solo del nivel (también el adaptativo).
        val size = MazeLevels.sizeFor(level)
        val seed = random.nextLong()
        return MazeInstance(
            level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY),
            seed,
            Maze.generate(size.cols, size.rows, seed),
        )
    }

    companion object {
        val TYPE = ChallengeType("maze")
    }
}
