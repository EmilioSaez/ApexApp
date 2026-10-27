package app.luxion.apexapp.domain.challenge.sensor

import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/** Lectura del contador acumulado de pasos. */
data class StepReading(val tNanos: Long, val total: Float)

/** Progreso de un reto de movimiento, de 0 a 1; [stalled] indica que el sensor dejó de emitir datos. */
data class MotionProgress(
    val fraction: Float,
    val rejected: Boolean = false,
    val stalled: Boolean = false,
) {
    val done: Boolean get() = fraction >= 1f
}

/** Fuentes de muestras; las implementa el adaptador de `SensorManager` y los tests. */
interface MotionSensors {
    fun linearAcceleration(): Flow<AccelSample>
    fun stepCounter(): Flow<StepReading>
}

/**
 * Ejecuta un reto de movimiento sobre [sensors] y emite su progreso. Termina al alcanzar el objetivo.
 * Si no llega ningún dato durante [noDataTimeoutMs] emite un progreso con [MotionProgress.stalled].
 */
class MotionRunner(
    private val kind: MotionKind,
    private val target: Int,
    private val sensors: MotionSensors,
    private val noDataTimeoutMs: Long = 10_000L,
    private val clockMs: () -> Long = System::currentTimeMillis,
) {
    fun progress(): Flow<MotionProgress> = channelFlow {
        var lastEventMs = clockMs()
        val watchdog = launch {
            while (true) {
                delay(WATCHDOG_STEP_MS)
                if (clockMs() - lastEventMs > noDataTimeoutMs) send(MotionProgress(0f, stalled = true))
            }
        }
        val source: Flow<MotionProgress> = when (kind) {
            MotionKind.SHAKE -> {
                val detector = ShakeDetector(target)
                sensors.linearAcceleration().map {
                    val s = detector.onSample(it)
                    MotionProgress(s.count.toFloat() / s.target, rejected = s.rejected)
                }
            }
            MotionKind.STEPS -> {
                val verifier = StepVerifier(target)
                launch {
                    sensors.linearAcceleration().collect {
                        lastEventMs = clockMs()
                        verifier.onAccel(it)
                    }
                }
                sensors.stepCounter().map {
                    val s = verifier.onStepCounter(it.tNanos, it.total)
                    MotionProgress(s.steps.toFloat() / s.target)
                }
            }
        }
        source.transformWhile { emit(it); !it.done }.collect {
            lastEventMs = clockMs()
            send(it)
        }
        coroutineContext.cancelChildren()
    }

    private companion object {
        const val WATCHDOG_STEP_MS = 1_000L
    }
}
