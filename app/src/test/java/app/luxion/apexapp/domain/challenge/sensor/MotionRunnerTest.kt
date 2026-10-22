package app.luxion.apexapp.domain.challenge.sensor

import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MotionRunnerTest {
    private class FakeSensors(
        val accel: Flow<AccelSample> = emptyFlow(),
        val motion: Flow<MotionSample> = emptyFlow(),
        val steps: Flow<StepReading> = emptyFlow(),
    ) : MotionSensors {
        override fun linearAcceleration() = accel
        override fun motion() = motion
        override fun stepCounter() = steps
    }

    private fun shakes(count: Int): List<AccelSample> {
        val random = Random(5)
        var t = 0L
        return (0 until count).map {
            t += 250 + random.nextLong(0, 200)
            spike(t, 14f + random.nextFloat() * 8f)
        }
    }

    @Test
    fun `agitar termina con progreso completo`() = runTest {
        val runner = MotionRunner(MotionKind.SHAKE, 10, FakeSensors(accel = shakes(12).asFlow()))
        val progress = runner.progress().toList()
        assertTrue(progress.last().done)
    }

    @Test
    fun `sensor sin datos emite stalled tras el tiempo de espera`() = runTest {
        // Un flujo que no emite nada y no termina.
        val silent = flow<AccelSample> { kotlinx.coroutines.awaitCancellation() }
        val runner = MotionRunner(MotionKind.SHAKE, 10, FakeSensors(accel = silent), noDataTimeoutMs = 3_000, clockMs = { currentTime })
        val first = kotlinx.coroutines.withTimeout(10_000) { runner.progress().first() }
        assertTrue(first.stalled)
    }
}
