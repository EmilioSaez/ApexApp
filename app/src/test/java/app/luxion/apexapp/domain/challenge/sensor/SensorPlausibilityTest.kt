package app.luxion.apexapp.domain.challenge.sensor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorPlausibilityTest {
    @Test
    fun `picos con variacion natural son plausibles`() {
        val peaks = listOf(0L to 14f, 310L to 17f, 640L to 15f, 900L to 19f, 1_260L to 13f, 1_520L to 16f)
            .map { Peak(it.first * MS, it.second) }
        assertFalse(SensorPlausibility.isTooPeriodic(peaks))
    }

    @Test
    fun `picos identicos son sinteticos`() {
        val peaks = (0 until 6).map { Peak(it * 300L * MS, 15f) }
        assertTrue(SensorPlausibility.isTooPeriodic(peaks))
    }

    @Test
    fun `pocos picos no permiten juzgar`() {
        assertFalse(SensorPlausibility.isTooPeriodic((0 until 3).map { Peak(it * 300L * MS, 15f) }))
    }

    @Test
    fun `senal con ruido no es plana y senal constante si`() {
        assertFalse(SensorPlausibility.isFlat(noisySamples(0, 1_000, level = 1f)))
        assertTrue(SensorPlausibility.isFlat((0 until 20).map { AccelSample(it * 20L * MS, 1f, 1f, 1f) }))
    }

    @Test
    fun `monotonia de marcas de tiempo`() {
        assertTrue(SensorPlausibility.isMonotonic(listOf(1L, 2L, 5L)))
        assertFalse(SensorPlausibility.isMonotonic(listOf(1L, 1L, 5L)))
        assertFalse(SensorPlausibility.isMonotonic(listOf(3L, 2L)))
    }
}
