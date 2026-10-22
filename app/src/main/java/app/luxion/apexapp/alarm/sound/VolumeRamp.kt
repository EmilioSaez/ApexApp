package app.luxion.apexapp.alarm.sound

/** Curva lineal del volumen progresivo, de [START_GAIN] a [ceiling] en `rampSeconds`. */
object VolumeRamp {
    const val START_GAIN = 0.05f

    /** Ganancia (0..1) tras [elapsedMs] de sonar; `rampSeconds == 0` suena directamente al techo. */
    fun gainAt(elapsedMs: Long, rampSeconds: Int, ceiling: Float): Float {
        val top = ceiling.coerceIn(0f, 1f)
        if (rampSeconds <= 0) return top
        val progress = (elapsedMs.toFloat() / (rampSeconds * 1000f)).coerceIn(0f, 1f)
        val start = minOf(START_GAIN, top)
        return start + (top - start) * progress
    }
}
