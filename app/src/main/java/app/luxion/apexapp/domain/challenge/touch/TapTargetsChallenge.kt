package app.luxion.apexapp.domain.challenge.touch

import app.luxion.apexapp.domain.challenge.Challenge
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import javax.inject.Inject
import kotlin.random.Random

/** Una instancia es el juego completo: se supera con [ChallengeAnswer.GameVerified] cuando la UI lo termina. */
class TapTargetsInstance(
    override val level: Int,
    val rounds: Int,
    private val random: Random,
) : ChallengeInstance {
    override val type: ChallengeType = TapTargetsChallenge.TYPE
    override val prompt: String = "tap_targets:$rounds"

    fun newGame(): TapTargetsGame = TapTargetsGame(rounds, level, random)

    override fun check(answer: ChallengeAnswer): Boolean =
        answer is ChallengeAnswer.GameVerified && answer.type == type
}

/** Reto de reflejos: tocar los círculos de cada ronda antes de que desaparezcan. */
class TapTargetsChallenge @Inject constructor() : Challenge {
    override val type: ChallengeType = TYPE

    override fun generate(level: Int, params: String, random: Random): ChallengeInstance =
        TapTargetsInstance(level, TapTargetsParams.rounds(params), random)

    companion object {
        val TYPE = ChallengeType("tap_targets")
    }
}
