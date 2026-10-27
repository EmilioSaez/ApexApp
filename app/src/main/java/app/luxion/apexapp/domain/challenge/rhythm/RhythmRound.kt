package app.luxion.apexapp.domain.challenge.rhythm

import kotlin.math.abs

/**
 * Estado de una ronda. No lee ningún reloj: quien la juega le pasa el tiempo en ms desde el inicio de la ronda.
 * Fallo = nota que pasa sin tocar o toque sin nota en la ventana; a [RhythmTuning.MAX_FAILURES] la ronda pide reinicio.
 */
class RhythmRound(val notes: List<RhythmNote>) {
    enum class Status { PLAYING, CLEARED, RESTART }

    private val resolved = BooleanArray(notes.size)

    var hits = 0
        private set
    var failures = 0
        private set
    var status = Status.PLAYING
        private set

    fun isResolved(index: Int): Boolean = resolved[index]

    /** Toque en [lane] en [timeMs]; acierta la nota pendiente más cercana de ese carril dentro de la ventana. */
    fun tap(lane: Int, timeMs: Long) {
        if (status != Status.PLAYING) return
        advance(timeMs)
        if (status != Status.PLAYING) return
        val index = notes.indices
            .filter { !resolved[it] && notes[it].lane == lane && abs(notes[it].timeMs - timeMs) <= RhythmTuning.HIT_WINDOW_MS }
            .minByOrNull { abs(notes[it].timeMs - timeMs) }
        if (index == null) {
            failures++
        } else {
            resolved[index] = true
            hits++
        }
        updateStatus()
    }

    /** Las notas cuya ventana ya cerró sin acierto son fallos. */
    fun advance(timeMs: Long) {
        if (status != Status.PLAYING) return
        for (i in notes.indices) {
            if (!resolved[i] && timeMs > notes[i].timeMs + RhythmTuning.HIT_WINDOW_MS) {
                resolved[i] = true
                failures++
                updateStatus()
                if (status != Status.PLAYING) return
            }
        }
    }

    private fun updateStatus() {
        status = when {
            failures >= RhythmTuning.MAX_FAILURES -> Status.RESTART
            resolved.all { it } -> Status.CLEARED
            else -> Status.PLAYING
        }
    }
}
