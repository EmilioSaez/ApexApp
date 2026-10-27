package app.luxion.apexapp.domain.challenge.rhythm

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Constantes de una ronda; ajustarlas aquí al calibrar. Una ronda dura unos 6,5 s. */
object RhythmTuning {
    const val LANES = 3
    const val NOTES_PER_ROUND = 6

    /** Tempo de la música de fondo; las notas caen sobre sus pulsos. */
    const val BPM = 120
    const val BEAT_MS = 60_000L / BPM

    /** Una nota cada dos pulsos (negras 1 y 3 de cada compás). */
    const val NOTE_SPACING_MS = 2 * BEAT_MS

    /** Tiempo que tarda una nota en caer hasta la línea de impacto (tres pulsos); también el margen antes de la primera. */
    const val FALL_MS = 3 * BEAT_MS

    /** Una nota se acierta si el toque cae a menos de esto de su instante de impacto. */
    const val HIT_WINDOW_MS = 250L
    const val MAX_FAILURES = 3
    const val MAX_SAME_LANE_IN_A_ROW = 3
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
