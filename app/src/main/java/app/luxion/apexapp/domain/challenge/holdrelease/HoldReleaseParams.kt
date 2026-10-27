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

/** Exigencia de un nivel: rango de mantenimiento antes de la señal, ventana de acierto y tope de rondas extra. */
data class HoldReleaseLevel(val minHoldMs: Long, val maxHoldMs: Long, val windowMs: Long, val maxExtra: Int)

/** Tabla única de calibración, por nivel de dificultad (índice 0 = nivel 1). */
object HoldReleaseTuning {
    val BY_LEVEL: List<HoldReleaseLevel> = listOf(
        HoldReleaseLevel(minHoldMs = 1_500, maxHoldMs = 3_000, windowMs = 1_200, maxExtra = 4),
        HoldReleaseLevel(minHoldMs = 1_500, maxHoldMs = 3_500, windowMs = 1_000, maxExtra = 4),
        HoldReleaseLevel(minHoldMs = 1_500, maxHoldMs = 4_000, windowMs = 800, maxExtra = 3),
        HoldReleaseLevel(minHoldMs = 2_000, maxHoldMs = 4_500, windowMs = 650, maxExtra = 2),
        HoldReleaseLevel(minHoldMs = 2_500, maxHoldMs = 5_000, windowMs = 500, maxExtra = 2),
    )

    fun forLevel(level: Int): HoldReleaseLevel = BY_LEVEL[
        level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY) - SessionConfig.MIN_DIFFICULTY
    ]
}
