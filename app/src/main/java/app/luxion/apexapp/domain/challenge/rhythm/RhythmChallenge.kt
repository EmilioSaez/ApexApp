package app.luxion.apexapp.domain.challenge.rhythm

import app.luxion.apexapp.domain.challenge.Challenge
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import javax.inject.Inject
import kotlin.random.Random

/** Reto de ritmo: una sola ronda; la dificultad de la alarma fija la velocidad de las notas y la duración de la ronda. */
class RhythmChallenge @Inject constructor() : Challenge {
    override val type: ChallengeType = TYPE

    override fun generate(level: Int, params: String, random: Random): ChallengeInstance =
        RhythmInstance(level, random.nextLong())

    companion object {
        val TYPE = ChallengeType("rhythm")
    }
}

/** [seed] inicializa los patrones de las rondas de esta sesión. */
class RhythmInstance(
    override val level: Int,
    val seed: Long,
) : ChallengeInstance {
    val pace: RhythmPace = RhythmTuning.pace(level)
    override val type: ChallengeType = RhythmChallenge.TYPE
    override val prompt: String = "rhythm"

    override fun check(answer: ChallengeAnswer): Boolean =
        answer is ChallengeAnswer.GameVerified && answer.type == type
}
