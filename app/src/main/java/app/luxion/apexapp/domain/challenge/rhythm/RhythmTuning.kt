package app.luxion.apexapp.domain.challenge.rhythm

import kotlin.math.roundToInt

/** Constantes de la ronda; ajustarlas aquí al calibrar. */
object RhythmTuning {
    const val LANES = 3

    /** Una nota se acierta si el toque cae a menos de esto de su instante de impacto. */
    const val HIT_WINDOW_MS = 250L
    const val MAX_FAILURES = 3
    const val MAX_SAME_LANE_IN_A_ROW = 3

    private val BPM_BY_LEVEL = intArrayOf(90, 105, 120, 150, 180)

    /** Duración objetivo de la única ronda por nivel: más dificultad, más larga. */
    private val ROUND_MS_BY_LEVEL = longArrayOf(6_000L, 7_000L, 7_500L, 9_000L, 11_000L)

    /** Ritmo del nivel de dificultad [level] (1..5, se acota); el nivel 3 equivale al ritmo previo (120 BPM, 6 notas). */
    fun pace(level: Int): RhythmPace {
        val index = level.coerceIn(1, BPM_BY_LEVEL.size) - 1
        return RhythmPace(BPM_BY_LEVEL[index], ROUND_MS_BY_LEVEL[index])
    }
}

/**
 * Velocidad y duración de la ronda. Las notas caen sobre los pulsos de la música: una cada dos pulsos y tres pulsos de caída.
 * A más [bpm], más rápido caen y más notas caben en [roundMs].
 */
data class RhythmPace(val bpm: Int, val roundMs: Long) {
    val beatMs: Long = 60_000L / bpm

    /** Una nota cada dos pulsos (negras 1 y 3 de cada compás). */
    val noteSpacingMs: Long = 2 * beatMs

    /** Tiempo que tarda una nota en caer hasta la línea de impacto; también el margen antes de la primera. */
    val fallMs: Long = 3 * beatMs

    val notesPerRound: Int = ((roundMs - fallMs).toDouble() / noteSpacingMs).roundToInt()
}
