package app.luxion.apexapp.domain.challenge.sensor

import kotlin.math.sqrt

/** Estado del reto de contar pasos. */
data class StepState(val steps: Int, val target: Int) {
    val done: Boolean get() = steps >= target
}

/**
 * Cuenta pasos desde el inicio del reto. La primera lectura del contador acumulado es la línea base
 * (los pasos previos no cuentan). Un incremento solo se acredita si el acelerómetro mostró energía de
 * caminar en la ventana reciente. La cadencia humana máxima se aplica sobre el tiempo transcurrido desde la
 * línea base, no entre lecturas: el contador real llega a saltos (pasos agrupados), y limitar cada salto por
 * el hueco con la lectura anterior descartaba pasos legítimos.
 */
class StepVerifier(
    private val target: Int,
    private val windowNanos: Long = 3_000_000_000L,
    private val minWalkRms: Float = 0.8f,
    private val maxStepsPerSecond: Float = 4f,
) {
    private val energy = ArrayDeque<AccelSample>()
    private var baselineSet = false
    private var lastTotal = 0f
    private var baselineNanos = 0L
    private var verified = 0

    var state = StepState(0, target)
        private set

    fun onAccel(sample: AccelSample) {
        energy.addLast(sample)
        while (energy.isNotEmpty() && sample.tNanos - energy.first().tNanos > windowNanos) energy.removeFirst()
    }

    fun onStepCounter(tNanos: Long, total: Float): StepState {
        if (state.done) return state
        if (!baselineSet) {
            baselineSet = true
            lastTotal = total
            baselineNanos = tNanos
            return state
        }
        val delta = (total - lastTotal).toInt()
        lastTotal = total
        if (delta <= 0) return state
        if (walkingRms(tNanos) < minWalkRms) return state
        verified += delta
        val elapsedSeconds = (tNanos - baselineNanos).coerceAtLeast(0L) / 1e9f
        val plausible = (maxStepsPerSecond * elapsedSeconds).toInt()
        state = StepState(verified.coerceAtMost(plausible).coerceAtMost(target), target)
        return state
    }

    /** Energía RMS del acelerómetro (aceleración lineal) en la ventana que termina en [nowNanos]. */
    private fun walkingRms(nowNanos: Long): Float {
        val recent = energy.filter { nowNanos - it.tNanos <= windowNanos }
        if (recent.size < MIN_SAMPLES) return 0f
        val meanSquare = recent.sumOf { (it.magnitude * it.magnitude).toDouble() } / recent.size
        return sqrt(meanSquare).toFloat()
    }

    private companion object {
        const val MIN_SAMPLES = 10
    }
}
