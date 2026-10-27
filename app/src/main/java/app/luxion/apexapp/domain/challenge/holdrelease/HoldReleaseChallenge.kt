package app.luxion.apexapp.domain.challenge.holdrelease

import app.luxion.apexapp.domain.challenge.Challenge
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import javax.inject.Inject
import kotlin.random.Random

/** El progreso vive en el [HoldReleaseGame] que crea la UI; la respuesta solo llega al superar todas las rondas. */
class HoldReleaseInstance(
    override val level: Int,
    val rounds: Int,
    private val random: Random,
) : ChallengeInstance {
    override val type: ChallengeType = HoldReleaseChallenge.TYPE
    override val prompt: String = "${type.id}:$rounds"

    fun newGame(): HoldReleaseGame = HoldReleaseGame(rounds, HoldReleaseTuning.forLevel(level), random)

    override fun check(answer: ChallengeAnswer): Boolean =
        answer is ChallengeAnswer.GameVerified && answer.type == type
}

class HoldReleaseChallenge @Inject constructor() : Challenge {
    override val type: ChallengeType = TYPE

    override fun generate(level: Int, params: String, random: Random): ChallengeInstance =
        HoldReleaseInstance(level, HoldReleaseParams.rounds(params, level), random)

    companion object {
        val TYPE = ChallengeType("holdrelease")
    }
}
