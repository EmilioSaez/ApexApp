package app.luxion.apexapp.domain.challenge.sensor

import kotlin.math.abs
import kotlin.math.sqrt

/** Fase del reto de levantarse. */
enum class StandUpPhase { WAITING_REST, WAITING_RISE, WALKING, DONE }

/** Muestra de movimiento: aceleración lineal y componente vertical (a lo largo de la gravedad). */
data class MotionSample(val linear: AccelSample, val vertical: Float) {
    val tNanos: Long get() = linear.tNanos
}

/**
 * Detecta quieto → incorporarse → caminar de forma sostenida. Cambiar la orientación del móvil sin
 * desplazarse no genera la marcha exigida, así que no supera el reto.
 */
class StandUpDetector(
    private val minRestNanos: Long = 2_000_000_000L,
    private val minWalkNanos: Long = 5_000_000_000L,
    private val restRms: Float = 0.6f,
    private val riseVertical: Float = 1.5f,
    private val walkRms: Float = 1.0f,
    private val windowNanos: Long = 1_000_000_000L,
) {
    private val window = ArrayDeque<MotionSample>()
    private var restSince: Long? = null
    private var walkSince: Long? = null

    var phase = StandUpPhase.WAITING_REST
        private set

    fun onSample(sample: MotionSample): StandUpPhase {
        if (phase == StandUpPhase.DONE) return phase
        window.addLast(sample)
        while (window.isNotEmpty() && sample.tNanos - window.first().tNanos > windowNanos) window.removeFirst()
        val rms = rms()
        when (phase) {
            StandUpPhase.WAITING_REST -> {
                if (rms <= restRms) {
                    val since = restSince ?: sample.tNanos.also { restSince = it }
                    if (sample.tNanos - since >= minRestNanos) phase = StandUpPhase.WAITING_RISE
                } else {
                    restSince = null
                }
            }
            StandUpPhase.WAITING_RISE -> {
                if (abs(sample.vertical) >= riseVertical) {
                    phase = StandUpPhase.WALKING
                    walkSince = null
                }
            }
            StandUpPhase.WALKING -> {
                if (rms >= walkRms) {
                    val since = walkSince ?: sample.tNanos.also { walkSince = it }
                    if (sample.tNanos - since >= minWalkNanos) phase = StandUpPhase.DONE
                } else {
                    walkSince = null
                    if (rms <= restRms) {
                        phase = StandUpPhase.WAITING_REST
                        restSince = null
                    }
                }
            }
            StandUpPhase.DONE -> Unit
        }
        return phase
    }

    private fun rms(): Float {
        if (window.size < 5) return 0f
        val meanSquare = window.sumOf { (it.linear.magnitude * it.linear.magnitude).toDouble() } / window.size
        return sqrt(meanSquare).toFloat()
    }
}
