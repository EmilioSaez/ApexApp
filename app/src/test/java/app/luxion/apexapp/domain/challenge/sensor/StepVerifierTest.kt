package app.luxion.apexapp.domain.challenge.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepVerifierTest {
    private fun walk(verifier: StepVerifier, fromMs: Long, durationMs: Long) =
        noisySamples(fromMs, durationMs, level = 2f, noise = 1.5f).forEach { verifier.onAccel(it) }

    @Test
    fun `pasos previos al inicio no cuentan`() {
        val v = StepVerifier(target = 10)
        walk(v, 0, 2_000)
        // El contador acumulado ya traía 5000 pasos: es la línea base.
        v.onStepCounter(2_000 * MS, 5_000f)
        assertEquals(0, v.state.steps)
    }

    @Test
    fun `caminar real acredita pasos hasta el objetivo`() {
        val v = StepVerifier(target = 10)
        walk(v, 0, 8_000)
        v.onStepCounter(0, 100f)
        var total = 100f
        for (s in 1..6) {
            total += 2f
            v.onStepCounter(s * 1_000L * MS, total)
        }
        assertTrue(v.state.done)
    }

    @Test
    fun `pasos sin movimiento del acelerometro no se acreditan`() {
        val v = StepVerifier(target = 5)
        // Dispositivo en reposo: solo ruido de sensor.
        noisySamples(0, 6_000, level = 0.05f, noise = 0.05f).forEach { v.onAccel(it) }
        v.onStepCounter(0, 0f)
        v.onStepCounter(3_000 * MS, 20f)
        assertEquals(0, v.state.steps)
    }

    @Test
    fun `cadencia imposible se limita`() {
        val v = StepVerifier(target = 100)
        walk(v, 0, 3_000)
        v.onStepCounter(0, 0f)
        // 60 pasos en un segundo: solo se acreditan los plausibles (4 por segundo).
        v.onStepCounter(1_000 * MS, 60f)
        assertEquals(4, v.state.steps)
    }

    @Test
    fun `pasos agrupados en lecturas seguidas no se pierden`() {
        val v = StepVerifier(target = 100)
        walk(v, 0, 6_000)
        v.onStepCounter(0, 0f)
        // El contador real retiene pasos y los entrega a saltos: 8 tras la latencia y 3 un instante después.
        v.onStepCounter(4_000 * MS, 8f)
        v.onStepCounter(4_100 * MS, 11f)
        assertEquals(11, v.state.steps)
    }

    @Test
    fun `un paso temprano se acredita cuando el tiempo lo hace plausible`() {
        val v = StepVerifier(target = 100)
        walk(v, 0, 6_000)
        v.onStepCounter(0, 0f)
        v.onStepCounter(100 * MS, 1f)
        assertEquals(0, v.state.steps)
        v.onStepCounter(2_000 * MS, 3f)
        assertEquals(3, v.state.steps)
    }
}
