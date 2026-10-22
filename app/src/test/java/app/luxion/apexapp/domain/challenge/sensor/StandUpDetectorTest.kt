package app.luxion.apexapp.domain.challenge.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StandUpDetectorTest {
    private fun motion(samples: List<AccelSample>, vertical: Float = 0f) = samples.map { MotionSample(it, vertical) }

    @Test
    fun `levantarse y caminar supera el reto`() {
        val d = StandUpDetector()
        motion(noisySamples(0, 3_000, level = 0.1f, noise = 0.05f)).forEach { d.onSample(it) }
        assertEquals(StandUpPhase.WAITING_RISE, d.phase)
        d.onSample(MotionSample(spike(3_020, 2.5f), vertical = 2.5f))
        assertEquals(StandUpPhase.WALKING, d.phase)
        motion(noisySamples(3_040, 7_000, level = 2.5f, noise = 1.5f)).forEach { d.onSample(it) }
        assertEquals(StandUpPhase.DONE, d.phase)
    }

    @Test
    fun `cambiar de orientacion sin desplazarse no supera el reto`() {
        val d = StandUpDetector()
        motion(noisySamples(0, 3_000, level = 0.1f, noise = 0.05f)).forEach { d.onSample(it) }
        // Un giro brusco del móvil genera un pico vertical pero ninguna marcha posterior.
        d.onSample(MotionSample(spike(3_020, 2.5f), vertical = 2.5f))
        motion(noisySamples(3_040, 8_000, level = 0.1f, noise = 0.05f)).forEach { d.onSample(it) }
        assertNotEquals(StandUpPhase.DONE, d.phase)
    }

    @Test
    fun `caminar sin haber estado quieto no avanza`() {
        val d = StandUpDetector()
        motion(noisySamples(0, 10_000, level = 2.5f, noise = 1.5f)).forEach { d.onSample(it) }
        assertEquals(StandUpPhase.WAITING_REST, d.phase)
    }
}
