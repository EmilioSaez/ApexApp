package app.luxion.apexapp.domain.challenge.sensor

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeDetectorTest {
    private fun feed(detector: ShakeDetector, samples: List<AccelSample>) = samples.forEach { detector.onSample(it) }

    /** Sacudidas humanas: amplitud y separación variables, con ruido entre ellas. */
    private fun humanShakes(count: Int, startMs: Long = 0): List<AccelSample> {
        val random = Random(3)
        val out = ArrayList<AccelSample>()
        var t = startMs
        repeat(count) {
            out += noisySamples(t, 200, 2f, random = random)
            t += 200
            out += spike(t, 14f + random.nextFloat() * 8f)
            t += 20
            out += noisySamples(t, 100 + random.nextLong(0, 150), 2f, random = random)
            t += 100 + 150
        }
        return out.sortedBy { it.tNanos }.distinctBy { it.tNanos }
    }

    @Test
    fun `sacudidas suficientes superan el reto`() {
        val detector = ShakeDetector(target = 6)
        feed(detector, humanShakes(8))
        assertTrue(detector.state.done)
        assertFalse(detector.state.rejected)
    }

    @Test
    fun `movimiento suave no cuenta`() {
        val detector = ShakeDetector(target = 3)
        feed(detector, noisySamples(0, 10_000, level = 5f, noise = 1.5f))
        assertEquals(0, detector.state.count)
    }

    @Test
    fun `pausa prolongada reinicia el progreso`() {
        val detector = ShakeDetector(target = 5)
        detector.onSample(spike(0, 15f))
        detector.onSample(spike(300, 16f))
        assertEquals(2, detector.state.count)
        detector.onSample(spike(3_000, 15f))
        assertEquals(1, detector.state.count)
    }

    @Test
    fun `periodicidad perfecta se rechaza`() {
        val detector = ShakeDetector(target = 10)
        // Picos idénticos cada 300 ms: amplitud y periodo sin ninguna variación.
        for (i in 0 until 8) detector.onSample(spike(i * 300L, 15f))
        assertTrue(detector.state.rejected)
        assertEquals(0, detector.state.count)
    }

    @Test
    fun `senal plana sin ruido se rechaza`() {
        val detector = ShakeDetector(target = 3)
        feed(detector, (0 until 30).map { AccelSample(it * 20L * MS, 0f, 0f, 0f) })
        assertTrue(detector.state.rejected)
    }

    @Test
    fun `marcas de tiempo no monotonas se rechazan`() {
        val detector = ShakeDetector(target = 3)
        detector.onSample(spike(500, 15f))
        detector.onSample(spike(100, 15f))
        assertTrue(detector.state.rejected)
    }
}
