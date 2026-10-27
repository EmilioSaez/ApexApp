package app.luxion.apexapp.domain.challenge.rhythm

import kotlin.random.Random

/** Nota que debe tocarse en [lane] en [timeMs] desde el inicio de la ronda. */
data class RhythmNote(val lane: Int, val timeMs: Long)

object RhythmPattern {
    fun generate(random: Random): List<RhythmNote> {
        val lanes = ArrayList<Int>(RhythmTuning.NOTES_PER_ROUND)
        repeat(RhythmTuning.NOTES_PER_ROUND) {
            var lane = random.nextInt(RhythmTuning.LANES)
            val run = lanes.takeLastWhile { it == lane }.size
            if (run >= RhythmTuning.MAX_SAME_LANE_IN_A_ROW) {
                // Se desplaza a otro carril para no repetir el mismo demasiadas veces seguidas.
                lane = (lane + 1 + random.nextInt(RhythmTuning.LANES - 1)) % RhythmTuning.LANES
            }
            lanes += lane
        }
        return lanes.mapIndexed { i, lane ->
            RhythmNote(lane, RhythmTuning.FALL_MS + i * RhythmTuning.NOTE_SPACING_MS)
        }
    }
}
