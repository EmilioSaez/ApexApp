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

    /**
     * Número de retos a superar: nunca menos que los tipos elegidos, aunque la alarma guardada traiga un
     * `count` menor.
     */
    val effectiveCount: Int get() = normalizeCount(count, challenges.distinctBy { it.type }.size)

    companion object {
        const val MIN_DIFFICULTY = 1
        const val MAX_DIFFICULTY = 5
        const val DEFAULT_DIFFICULTY = 3
        const val MIN_COUNT = 1
        const val MAX_COUNT = 10

        /** 0 (sin definir) equivale a [DEFAULT_DIFFICULTY]; fuera de rango se acota. */
        fun normalizeDifficulty(raw: Int): Int =
            if (raw == 0) DEFAULT_DIFFICULTY else raw.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)

        /** Mínimo de retos para [typeCount] tipos elegidos: uno de cada tipo, con tope en [MAX_COUNT]. */
        fun minCount(typeCount: Int): Int = typeCount.coerceIn(MIN_COUNT, MAX_COUNT)

        /** Acota [raw] entre [minCount] y [MAX_COUNT]. */
        fun normalizeCount(raw: Int, typeCount: Int): Int = raw.coerceIn(minCount(typeCount), MAX_COUNT)

        fun from(config: ChallengeConfig): SessionConfig = SessionConfig(
            difficulty = normalizeDifficulty(config.difficulty),
            count = normalizeCount(config.count, config.challenges.distinctBy { it.type }.size),
            mode = config.mode,
            challenges = config.challenges,
        )
    }
}
