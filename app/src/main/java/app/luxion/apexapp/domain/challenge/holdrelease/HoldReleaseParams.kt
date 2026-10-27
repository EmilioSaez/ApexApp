package app.luxion.apexapp.domain.challenge.holdrelease

import app.luxion.apexapp.domain.challenge.SessionConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class RoundsParams(val rounds: Int? = null)

/** Lectura y escritura de `ChallengeSpec.params` del reto de mantener y soltar: `{"rounds":N}`. */
object HoldReleaseParams {
    const val MIN_ROUNDS = 2
    const val MAX_ROUNDS = 6

    /** Rondas por defecto por nivel de dificultad (índice 0 = nivel 1). Tabla única: ajustar aquí al calibrar. */
    val DEFAULT_ROUNDS_BY_LEVEL: List<Int> = listOf(2, 3, 3, 4, 5)

    fun defaultRounds(level: Int): Int =
        DEFAULT_ROUNDS_BY_LEVEL[level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY) - SessionConfig.MIN_DIFFICULTY]

    private val json = Json { ignoreUnknownKeys = true }

    /** Rondas guardadas en [params], acotadas; [defaultRounds] del [level] si faltan o son ilegibles. */
    fun rounds(params: String, level: Int): Int =
        (runCatching { json.decodeFromString<RoundsParams>(params) }.getOrNull()?.rounds ?: defaultRounds(level))
            .coerceIn(MIN_ROUNDS, MAX_ROUNDS)

    fun paramsFor(rounds: Int): String = json.encodeToString(RoundsParams(rounds.coerceIn(MIN_ROUNDS, MAX_ROUNDS)))
}

/** Exigencia de un nivel: ventana de acierto tras la señal. La duración del reto no depende del nivel. */
data class HoldReleaseLevel(val windowMs: Long)

/** Tabla única de calibración, por nivel de dificultad (índice 0 = nivel 1). */
object HoldReleaseTuning {
    val BY_LEVEL: List<HoldReleaseLevel> = listOf(1_200L, 1_000L, 800L, 650L, 500L).map(::HoldReleaseLevel)

    fun forLevel(level: Int): HoldReleaseLevel = BY_LEVEL[
        level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY) - SessionConfig.MIN_DIFFICULTY
    ]
}
