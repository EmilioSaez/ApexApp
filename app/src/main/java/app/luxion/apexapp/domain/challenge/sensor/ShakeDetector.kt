package app.luxion.apexapp.domain.challenge.sensor

/** Estado visible del reto de agitar. */
data class ShakeState(val count: Int, val target: Int, val rejected: Boolean = false) {
    val done: Boolean get() = count >= target
}

/**
 * Cuenta sacudidas: picos de aceleración lineal sobre [thresholdMs2], separados al menos [refractoryNanos].
 * Si pasan más de [maxIdleNanos] sin sacudida el progreso se reinicia. Una señal sintética se rechaza.
 */
class ShakeDetector(
    private val target: Int,
    private val thresholdMs2: Float = DEFAULT_THRESHOLD,
    private val refractoryNanos: Long = 200_000_000L,
    private val maxIdleNanos: Long = 2_000_000_000L,
) {
    private val peaks = ArrayList<Peak>()
    private val window = ArrayDeque<AccelSample>()
    private var lastTimestamp = Long.MIN_VALUE
    private var validFrom = 0

    var state = ShakeState(0, target)
        private set

    fun onSample(sample: AccelSample): ShakeState {
        if (state.done) return state
        if (sample.tNanos <= lastTimestamp) return reject()
        lastTimestamp = sample.tNanos
        window.addLast(sample)
        if (window.size > WINDOW) window.removeFirst()
        if (SensorPlausibility.isFlat(window.toList())) return reject()

        if (peaks.isNotEmpty() && sample.tNanos - peaks.last().tNanos > maxIdleNanos) reset()
        val lastPeak = peaks.lastOrNull()
        val magnitude = sample.magnitude
        if (magnitude >= thresholdMs2 && (lastPeak == null || sample.tNanos - lastPeak.tNanos >= refractoryNanos)) {
            peaks += Peak(sample.tNanos, magnitude)
            // Se juzga siempre la ventana más reciente: una fuente sintética sigue rechazada pico a pico.
            if (SensorPlausibility.isTooPeriodic(peaks.takeLast(SensorPlausibility.MIN_PEAKS_FOR_PERIODICITY))) {
                validFrom = peaks.size
                state = ShakeState(0, target, rejected = true)
                return state
            }
            state = ShakeState((peaks.size - validFrom).coerceAtMost(target), target)
        }
        return state
    }

    private fun reject(): ShakeState {
        reset()
        state = ShakeState(0, target, rejected = true)
        return state
    }

    private fun reset() {
        peaks.clear()
        validFrom = 0
        state = ShakeState(0, target)
    }

    companion object {
        const val DEFAULT_THRESHOLD = 12f
        private const val WINDOW = 50
    }
}
