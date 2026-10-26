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
 * Detecta quieto → incorporarse → caminar. Cambiar la orientación del móvil sin desplazarse no genera la
 * marcha exigida, así que no supera el reto. La marcha se acumula: una parada breve ([maxPauseNanos]) al
 * girar una esquina o abrir una puerta no la reinicia, porque exigir [minWalkNanos] ininterrumpidos
 * descartaba marchas reales con el móvil en la mano.
 */
class StandUpDetector(
    private val minRestNanos: Long = 2_000_000_000L,
    private val minWalkNanos: Long = 5_000_000_000L,
    private val restRms: Float = 0.6f,
    private val riseVertical: Float = 1.0f,
    private val walkRms: Float = 0.8f,
    private val windowNanos: Long = 1_000_000_000L,
    private val maxPauseNanos: Long = 2_000_000_000L,
) {
    private val window = ArrayDeque<MotionSample>()
    private var restSince: Long? = null
    private var walkedNanos = 0L
    private var pausedNanos = 0L
    private var lastNanos = 0L

    var phase = StandUpPhase.WAITING_REST
        private set

    fun onSample(sample: MotionSample): StandUpPhase {
        if (phase == StandUpPhase.DONE) return phase
        val previous = lastNanos
        lastNanos = sample.tNanos
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
                    walkedNanos = 0L
                    pausedNanos = 0L
                }
            }
            StandUpPhase.WALKING -> {
                // Un hueco largo sin muestras no regala tiempo de marcha.
                val dt = (sample.tNanos - previous).coerceIn(0L, windowNanos)
                if (rms >= walkRms) {
                    pausedNanos = 0L
                    walkedNanos += dt
                    if (walkedNanos >= minWalkNanos) phase = StandUpPhase.DONE
                } else {
                    pausedNanos += dt
                    if (pausedNanos > maxPauseNanos) {
                        walkedNanos = 0L
                        if (rms <= restRms) {
                            phase = StandUpPhase.WAITING_REST
                            restSince = null
                        }
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
