package app.luxion.apexapp.domain.challenge.rhythm

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

/** Constantes de una ronda; ajustarlas aquí al calibrar. */
object RhythmTuning {
    const val LANES = 3

    /** Una nota se acierta si el toque cae a menos de esto de su instante de impacto. */
    const val HIT_WINDOW_MS = 250L
    const val MAX_FAILURES = 3
    const val MAX_SAME_LANE_IN_A_ROW = 3

    /** Duración objetivo de una ronda (~7,5 s): igual en todos los niveles, lo que cambia es la velocidad. */
    const val ROUND_MS = 7_500L

    private val BPM_BY_LEVEL = intArrayOf(90, 105, 120, 150, 180)

    /** Ritmo del nivel de dificultad [level] (1..5, se acota); el nivel 3 equivale al ritmo previo (120 BPM, 6 notas). */
    fun pace(level: Int): RhythmPace = RhythmPace(BPM_BY_LEVEL[level.coerceIn(1, BPM_BY_LEVEL.size) - 1])
}

/**
 * Velocidad de una ronda. Las notas caen sobre los pulsos de la música: una cada dos pulsos y tres pulsos de caída.
 * A más [bpm], más rápido caen y más notas caben en [RhythmTuning.ROUND_MS].
 */
data class RhythmPace(val bpm: Int) {
    val beatMs: Long = 60_000L / bpm

    /** Una nota cada dos pulsos (negras 1 y 3 de cada compás). */
    val noteSpacingMs: Long = 2 * beatMs

    /** Tiempo que tarda una nota en caer hasta la línea de impacto; también el margen antes de la primera. */
    val fallMs: Long = 3 * beatMs

    val notesPerRound: Int = ((RhythmTuning.ROUND_MS - fallMs).toDouble() / noteSpacingMs).roundToInt()
}

@Serializable
private data class RhythmSpecParams(val rounds: Int = RhythmParams.DEFAULT_ROUNDS)

/** Lectura y escritura de `ChallengeSpec.params` del reto de ritmo: `{"rounds":N}`. */
object RhythmParams {
    const val MIN_ROUNDS = 2
    const val MAX_ROUNDS = 8
    const val DEFAULT_ROUNDS = 3

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Rondas de [params]; ausente o ilegible da [DEFAULT_ROUNDS] y fuera de rango se acota. */
    fun rounds(params: String): Int =
        runCatching { json.decodeFromString<RhythmSpecParams>(params).rounds }
            .getOrDefault(DEFAULT_ROUNDS)
            .coerceIn(MIN_ROUNDS, MAX_ROUNDS)

    fun paramsFor(rounds: Int): String =
        json.encodeToString(RhythmSpecParams(rounds.coerceIn(MIN_ROUNDS, MAX_ROUNDS)))
}
