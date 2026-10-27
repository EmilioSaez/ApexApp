package app.luxion.apexapp.domain.challenge.holdrelease

import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HoldReleaseTest {
    private val tuning = HoldReleaseTuning.forLevel(3)
    private val window = tuning.windowMs

    private fun game(rounds: Int = 3, seed: Int = 1, level: Int = 3) =
        HoldReleaseGame(rounds, HoldReleaseTuning.forLevel(level), Random(seed))

    /** Pulsa en [t] y devuelve el instante de la señal, que sale de dejar correr el reloj. */
    private fun HoldReleaseGame.pressAndFindSignal(t: Long): Long {
        press(t)
        var now = t
        while (phase == HoldPhase.HOLDING) tick(++now)
        return now
    }

    private fun HoldReleaseGame.hit(t: Long, reaction: Long = 300): RoundOutcome? {
        val signal = pressAndFindSignal(t)
        return release(signal + reaction)
    }

    @Test
    fun `params ausentes corruptos o fuera de rango`() {
        assertEquals(3, HoldReleaseParams.rounds(""))
        assertEquals(3, HoldReleaseParams.rounds("no es json"))
        assertEquals(3, HoldReleaseParams.rounds("{}"))
        assertEquals(2, HoldReleaseParams.rounds("""{"rounds":0}"""))
        assertEquals(6, HoldReleaseParams.rounds("""{"rounds":99}"""))
        assertEquals(5, HoldReleaseParams.rounds(HoldReleaseParams.paramsFor(5)))
    }

    @Test
    fun `la exigencia crece con la dificultad`() {
        val levels = (1..5).map(HoldReleaseTuning::forLevel)
        assertEquals(levels.map { it.windowMs }.sortedDescending(), levels.map { it.windowMs })
        assertEquals(levels.map { it.maxHoldMs }.sorted(), levels.map { it.maxHoldMs })
        assertEquals(levels.map { it.minHoldMs }.sorted(), levels.map { it.minHoldMs })
        assertEquals(levels.map { it.maxExtra }.sortedDescending(), levels.map { it.maxExtra })
        assertTrue(levels.first().windowMs > levels.last().windowMs)
        assertTrue(levels.first().maxHoldMs < levels.last().maxHoldMs)
        assertTrue(levels.first().maxExtra > levels.last().maxExtra)
        assertTrue(levels.all { it.minHoldMs < it.maxHoldMs && it.maxExtra >= 1 })
    }

    @Test
    fun `niveles fuera de rango se acotan`() {
        assertEquals(HoldReleaseTuning.forLevel(1), HoldReleaseTuning.forLevel(-3))
        assertEquals(HoldReleaseTuning.forLevel(5), HoldReleaseTuning.forLevel(9))
    }

    @Test
    fun `acertar todas las rondas supera el reto`() {
        val g = game()
        repeat(3) { assertEquals(RoundOutcome.HIT, g.hit(it * 10_000L)) }
        assertTrue(g.done)
        assertEquals(3, g.roundsDone)
    }

    @Test
    fun `el instante de la señal cae en el rango y varia con la semilla`() {
        val signals = (1..20).map { seed -> game(seed = seed).run { pressAndFindSignal(0) } }
        assertTrue(signals.all { it in tuning.minHoldMs..tuning.maxHoldMs })
        assertTrue(signals.toSet().size > 1)
    }

    @Test
    fun `el rango de mantenimiento sigue el nivel`() {
        (1..5).forEach { level ->
            val t = HoldReleaseTuning.forLevel(level)
            val signals = (1..20).map { seed -> game(seed = seed, level = level).run { pressAndFindSignal(0) } }
            assertTrue(signals.all { it in t.minHoldMs..t.maxHoldMs })
        }
    }

    @Test
    fun `soltar antes de la señal falla`() {
        val g = game()
        g.press(0)
        assertEquals(RoundOutcome.EARLY, g.release(500))
        assertEquals(HoldPhase.IDLE, g.phase)
    }

    @Test
    fun `soltar tras la ventana falla`() {
        val g = game()
        val signal = g.pressAndFindSignal(0)
        assertEquals(RoundOutcome.LATE, g.release(signal + window + 1))
    }

    @Test
    fun `mantener pasada la ventana falla sin soltar y el soltar posterior se ignora`() {
        val g = game()
        val signal = g.pressAndFindSignal(0)
        assertEquals(RoundOutcome.LATE, g.tick(signal + window + 1))
        assertNull(g.release(signal + window + 50))
    }

    @Test
    fun `un fallo suma una ronda extra y conserva las superadas`() {
        val g = game()
        g.hit(0)
        g.press(10_000)
        g.release(10_100)
        assertEquals(4, g.totalRounds)
        assertEquals(1, g.roundsDone)
    }

    @Test
    fun `las rondas extra tienen tope`() {
        val g = game(rounds = 6)
        repeat(2) { g.press(it * 10_000L); g.release(it * 10_000L + 10) }
        g.hit(50_000)
        repeat(2) { g.press(60_000L + it * 10_000L); g.release(60_000L + it * 10_000L + 10) }
        g.hit(90_000)
        repeat(2) { g.press(100_000L + it * 10_000L); g.release(100_000L + it * 10_000L + 10) }
        assertEquals(tuning.maxExtra, g.extraRounds)
    }

    @Test
    fun `el tope de rondas extra baja con la dificultad`() {
        fun extrasAfterFailures(level: Int): Int {
            val g = game(rounds = 6, level = level)
            repeat(2) { g.press(it * 10_000L); g.release(it * 10_000L + 10) }
            g.hit(50_000)
            repeat(2) { g.press(60_000L + it * 10_000L); g.release(60_000L + it * 10_000L + 10) }
            g.hit(90_000)
            repeat(2) { g.press(100_000L + it * 10_000L); g.release(100_000L + it * 10_000L + 10) }
            return g.extraRounds
        }
        assertEquals(HoldReleaseTuning.forLevel(1).maxExtra, extrasAfterFailures(1))
        assertEquals(HoldReleaseTuning.forLevel(5).maxExtra, extrasAfterFailures(5))
        assertTrue(extrasAfterFailures(1) > extrasAfterFailures(5))
    }

    @Test
    fun `tres fallos seguidos reinician la ronda sin ronda extra`() {
        val g = game()
        g.hit(0)
        repeat(3) { g.press(10_000L + it * 10_000L); g.release(10_000L + it * 10_000L + 10) }
        assertEquals(2, g.extraRounds)
        assertEquals(1, g.roundsDone)
        assertEquals(HoldPhase.IDLE, g.phase)
    }

    @Test
    fun `cancelar la pulsacion cuenta como fallo`() {
        val g = game()
        g.press(0)
        assertEquals(RoundOutcome.CANCELLED, g.cancel())
        assertEquals(1, g.extraRounds)
        assertNull(g.cancel())
    }

    @Test
    fun `la instancia solo acepta GameVerified de su tipo`() {
        val instance = HoldReleaseChallenge().generate(3, HoldReleaseParams.paramsFor(4), Random(1)) as HoldReleaseInstance
        assertEquals(4, instance.rounds)
        assertTrue(instance.check(ChallengeAnswer.GameVerified(HoldReleaseChallenge.TYPE)))
        assertFalse(instance.check(ChallengeAnswer.GameVerified(ChallengeType("otro"))))
        assertFalse(instance.check(ChallengeAnswer.MotionVerified(HoldReleaseChallenge.TYPE)))
    }
}
