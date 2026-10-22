package app.luxion.apexapp.domain.challenge.sensor

import kotlin.math.abs
import kotlin.math.sqrt

/** Aceleración lineal (sin gravedad) en m/s². [tNanos] es la marca de tiempo del sensor. */
data class AccelSample(val tNanos: Long, val x: Float, val y: Float, val z: Float) {
    val magnitude: Float get() = sqrt(x * x + y * y + z * z)
}

/** Un máximo local de intensidad: instante en nanosegundos y amplitud. */
data class Peak(val tNanos: Long, val amplitude: Float)

/** Reglas de plausibilidad: los sensores reales tienen ruido y nunca son perfectamente periódicos. */
object SensorPlausibility {
    /** Mínimo de picos para juzgar la periodicidad. */
    const val MIN_PEAKS_FOR_PERIODICITY = 5

    /** Coeficiente de variación por debajo del cual amplitud y periodo se consideran sintéticos. */
    const val MIN_VARIATION = 0.02

    /** Fracción de muestras consecutivas idénticas a partir de la cual la señal se considera plana. */
    const val MAX_REPEATED_FRACTION = 0.9

    fun isMonotonic(timestamps: List<Long>): Boolean =
        timestamps.zipWithNext().all { (a, b) -> b > a }

    /** Verdadero si las muestras repiten el mismo valor casi siempre (señal inyectada o sin ruido). */
    fun isFlat(samples: List<AccelSample>): Boolean {
        if (samples.size < 10) return false
        val repeated = samples.zipWithNext().count { (a, b) -> a.x == b.x && a.y == b.y && a.z == b.z }
        return repeated.toDouble() / (samples.size - 1) >= MAX_REPEATED_FRACTION
    }

    /** Verdadero si los picos tienen amplitud y periodo prácticamente idénticos. */
    fun isTooPeriodic(peaks: List<Peak>): Boolean {
        if (peaks.size < MIN_PEAKS_FOR_PERIODICITY) return false
        val intervals = peaks.zipWithNext { a, b -> (b.tNanos - a.tNanos).toDouble() }
        val amplitudes = peaks.map { it.amplitude.toDouble() }
        return variation(intervals) < MIN_VARIATION && variation(amplitudes) < MIN_VARIATION
    }

    /** Coeficiente de variación (desviación típica / media). */
    fun variation(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        if (abs(mean) < 1e-12) return 0.0
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return sqrt(variance) / abs(mean)
    }
}
