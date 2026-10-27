package app.luxion.apexapp.anticheat

enum class SuspicionReason { REPEATED_EXIT, VOLUME_TAMPER, PROCESS_INTERRUPTED, TIMED_OUT, SYNTHETIC_INPUT }

/**
 * Acumula intentos de manipulación por sesión y decide si es sospechosa. No guarda estado en
 * disco: la restauración tras muerte del proceso la señala quien llama con [interrupted].
 */
class SessionIntegrityTracker(
    private val exitThreshold: Int = DEFAULT_EXIT_THRESHOLD,
    private val volumeThreshold: Int = DEFAULT_VOLUME_THRESHOLD,
    private val syntheticThreshold: Int = DEFAULT_SYNTHETIC_THRESHOLD,
) {
    private class State {
        var exits = 0
        var volumeResets = 0
        var syntheticInputs = 0
        var interrupted = false
    }

    private val sessions = HashMap<String, State>()

    @Synchronized
    fun begin(sessionId: String) {
        sessions.getOrPut(sessionId) { State() }
    }

    @Synchronized
    fun exitAttempt(sessionId: String) {
        sessions.getOrPut(sessionId) { State() }.exits++
    }

    @Synchronized
    fun volumeReset(sessionId: String) {
        sessions.getOrPut(sessionId) { State() }.volumeResets++
    }

    /** Un juego táctil descartó un toque por no parecer humano (solo se informa con anti-trampas `STRICT`). */
    @Synchronized
    fun syntheticInput(sessionId: String) {
        sessions.getOrPut(sessionId) { State() }.syntheticInputs++
    }

    @Synchronized
    fun interrupted(sessionId: String) {
        sessions.getOrPut(sessionId) { State() }.interrupted = true
    }

    /** Cierra la sesión y devuelve los motivos de sospecha; vacío si es limpia. */
    @Synchronized
    fun finish(sessionId: String, timedOut: Boolean): Set<SuspicionReason> {
        val state = sessions.remove(sessionId) ?: State()
        return buildSet {
            if (state.exits >= exitThreshold) add(SuspicionReason.REPEATED_EXIT)
            if (state.volumeResets >= volumeThreshold) add(SuspicionReason.VOLUME_TAMPER)
            if (state.syntheticInputs >= syntheticThreshold) add(SuspicionReason.SYNTHETIC_INPUT)
            if (state.interrupted) add(SuspicionReason.PROCESS_INTERRUPTED)
            if (timedOut) add(SuspicionReason.TIMED_OUT)
        }
    }

    companion object {
        const val DEFAULT_EXIT_THRESHOLD = 3
        const val DEFAULT_VOLUME_THRESHOLD = 3
        const val DEFAULT_SYNTHETIC_THRESHOLD = 3
    }
}
