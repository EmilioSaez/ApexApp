package app.luxion.apexapp.domain.challenge

import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec

/** Configuración de una sesión de retos, ya normalizada. */
data class SessionConfig(
    val difficulty: Int,
    val count: Int,
    val mode: ChallengeMode,
    val challenges: List<ChallengeSpec>,
) {
    init {
        require(difficulty in MIN_DIFFICULTY..MAX_DIFFICULTY) { "difficulty must be in 1..5" }
        require(count in MIN_COUNT..MAX_COUNT) { "count must be in 1..10" }
    }

    companion object {
        const val MIN_DIFFICULTY = 1
        const val MAX_DIFFICULTY = 5
        const val DEFAULT_DIFFICULTY = 3
        const val MIN_COUNT = 1
        const val MAX_COUNT = 10

        /** 0 (sin definir) equivale a [DEFAULT_DIFFICULTY]; fuera de rango se acota. */
        fun normalizeDifficulty(raw: Int): Int =
            if (raw == 0) DEFAULT_DIFFICULTY else raw.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)

        fun from(config: ChallengeConfig): SessionConfig = SessionConfig(
            difficulty = normalizeDifficulty(config.difficulty),
            count = config.count,
            mode = config.mode,
            challenges = config.challenges,
        )
    }
}
