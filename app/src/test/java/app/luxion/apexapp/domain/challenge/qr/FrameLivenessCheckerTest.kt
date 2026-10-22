package app.luxion.apexapp.domain.challenge.qr

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameLivenessCheckerTest {
    private val frameNanos = 33_000_000L

    private fun run(checker: FrameLivenessChecker, frames: List<FrameObservation>): LivenessVerdict =
        frames.map { checker.onFrame(it) }.let { all -> all.firstOrNull { it !is LivenessVerdict.Collecting } ?: all.last() }

    /** Escena real: la mano tiembla y la luz del fondo fluctúa de forma irregular. */
    private fun live(count: Int, value: String = "A", start: Long = 0, seed: Int = 1): List<FrameObservation> {
        val r = Random(seed)
        return (0 until count).map {
            FrameObservation(
                tNanos = start + it * frameNanos,
                value = value,
                centerX = 500f + r.nextFloat() * 6f,
                centerY = 400f + r.nextFloat() * 6f,
                backgroundLuma = 120f + r.nextFloat() * 3f,
            )
        }
    }

    @Test
    fun `escaneo en vivo del objeto real se acepta`() {
        assertEquals(LivenessVerdict.Accepted("A"), run(FrameLivenessChecker(), live(60)))
    }

    @Test
    fun `imagen estatica se rechaza`() {
        val frozen = (0 until 60).map { FrameObservation(it * frameNanos, "A", 500f, 400f, 120f) }
        val verdict = run(FrameLivenessChecker(), frozen)
        assertEquals(LivenessVerdict.Rejected(LivenessVerdict.Reason.STATIC_IMAGE), verdict)
    }

    @Test
    fun `pantalla con parpadeo periodico se rechaza`() {
        val screen = (0 until 90).map {
            FrameObservation(
                tNanos = it * frameNanos,
                value = "A",
                centerX = 500f + (it % 3) * 2f,
                centerY = 400f,
                backgroundLuma = 120f + 5f * sin(2 * PI * it / 6.0).toFloat(),
            )
        }
        val verdict = run(FrameLivenessChecker(), screen)
        assertEquals(LivenessVerdict.Rejected(LivenessVerdict.Reason.SCREEN), verdict)
    }

    @Test
    fun `no acepta antes del tiempo minimo`() {
        val verdict = run(FrameLivenessChecker(), live(10))
        assertTrue(verdict is LivenessVerdict.Collecting)
    }

    @Test
    fun `codigo perdido reinicia la verificacion`() {
        val checker = FrameLivenessChecker()
        live(30).forEach { checker.onFrame(it) }
        checker.onFrame(FrameObservation(31 * frameNanos, null))
        // Tras la pérdida hacen falta de nuevo tiempo y fotogramas completos.
        val verdict = run(checker, live(10, start = 32 * frameNanos, seed = 2))
        assertTrue(verdict is LivenessVerdict.Collecting)
    }

    @Test
    fun `cambiar de codigo reinicia la verificacion`() {
        val checker = FrameLivenessChecker()
        live(40, value = "A").forEach { checker.onFrame(it) }
        val verdict = run(checker, live(5, value = "B", start = 41 * frameNanos, seed = 3))
        assertTrue(verdict is LivenessVerdict.Collecting)
    }
}
