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

/** Tiempos del reto en un solo sitio: ajustar aquí al calibrar. */
object HoldReleaseTiming {
    const val MIN_HOLD_MS = 1_500L
    const val MAX_HOLD_MS = 4_000L

    /** Ventana de acierto tras la señal, por nivel de dificultad (índice 0 = nivel 1). */
    private val WINDOW_BY_LEVEL = listOf(1_200L, 1_000L, 800L, 650L, 500L)

    fun windowFor(level: Int): Long = WINDOW_BY_LEVEL[
        level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY) - SessionConfig.MIN_DIFFICULTY
    ]
}
