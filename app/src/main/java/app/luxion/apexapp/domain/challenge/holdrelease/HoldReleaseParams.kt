package app.luxion.apexapp.domain.challenge.holdrelease

import app.luxion.apexapp.domain.challenge.SessionConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class RoundsParams(val rounds: Int = HoldReleaseParams.DEFAULT_ROUNDS)

/** Lectura y escritura de `ChallengeSpec.params` del reto de mantener y soltar: `{"rounds":N}`. */
object HoldReleaseParams {
    const val MIN_ROUNDS = 2
    const val MAX_ROUNDS = 6
    const val DEFAULT_ROUNDS = 3

    private val json = Json { ignoreUnknownKeys = true }

    /** Rondas guardadas en [params], acotadas; [DEFAULT_ROUNDS] si está vacío o es ilegible. */
    fun rounds(params: String): Int =
        runCatching { json.decodeFromString<RoundsParams>(params.ifBlank { return DEFAULT_ROUNDS }) }
            .getOrNull()?.rounds?.coerceIn(MIN_ROUNDS, MAX_ROUNDS) ?: DEFAULT_ROUNDS

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
