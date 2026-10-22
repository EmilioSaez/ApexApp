package app.luxion.apexapp.domain.adaptive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveDifficultyCalculatorTest {
    private val calculator = AdaptiveDifficultyCalculator()

    private fun s(id: Int, snoozes: Int = 0, failures: Int = 0, seconds: Long? = 60) =
        SessionSummary("s$id", snoozes, failures, seconds?.let { it * 1000 })

    /** Más reciente primero, como las entrega el historial. */
    private fun decide(level: Int, vararg recent: SessionSummary, min: Int = 1, max: Int = 5, marker: String? = null) =
        calculator.decide(level, min, max, recent.toList(), marker)

    @Test
    fun fewerThanThreeSessionsDoNotChange() {
        val d = decide(3, s(2, snoozes = 2), s(1, snoozes = 2))
        assertEquals(3, d.level)
        assertFalse(d.changed)
    }

    @Test
    fun onlyFiveMostRecentSessionsCount() {
        // 6 sesiones: las 3 recientes son neutras, las antiguas piden subir pero no cuentan.
        val d = decide(3, s(6), s(5), s(4), s(3, snoozes = 1), s(2, snoozes = 1), s(1, snoozes = 1))
        assertFalse(d.changed)
    }

    @Test
    fun twoOfLastThreeWithSnoozesRaisesLevel() {
        val d = decide(3, s(3, snoozes = 1), s(2), s(1, snoozes = 2))
        assertEquals(4, d.level)
        assertEquals(ChangeReason.SNOOZES, d.reason)
        assertEquals(2, d.hits)
    }

    @Test
    fun oneSnoozeInThreeDoesNotRaise() {
        assertFalse(decide(3, s(3, snoozes = 1), s(2), s(1)).changed)
    }

    @Test
    fun threeCleanFastSessionsRaiseLevel() {
        val d = decide(2, s(3, seconds = 10), s(2, seconds = 20), s(1, seconds = 29))
        assertEquals(3, d.level)
        assertEquals(ChangeReason.EASY, d.reason)
    }

    @Test
    fun easyRuleNeedsAllThreeUnderThirtySeconds() {
        assertFalse(decide(2, s(3, seconds = 10), s(2, seconds = 30), s(1, seconds = 10)).changed)
        assertFalse(decide(2, s(3, seconds = 10), s(2, failures = 1, seconds = 10), s(1, seconds = 10)).changed)
        assertFalse(decide(2, s(3, seconds = null), s(2, seconds = 10), s(1, seconds = 10)).changed)
    }

    @Test
    fun twoOfLastThreeWithManyFailuresLowersLevel() {
        val d = decide(4, s(3, failures = 3), s(2), s(1, failures = 5))
        assertEquals(3, d.level)
        assertEquals(ChangeReason.FAILURES, d.reason)
        assertEquals(2, d.hits)
    }

    @Test
    fun twoFailuresInASessionIsNotStruggle() {
        assertFalse(decide(4, s(3, failures = 2), s(2, failures = 2), s(1, failures = 2)).changed)
    }

    @Test
    fun slowSessionsWithoutSnoozesLowerLevel() {
        val d = decide(4, s(3, seconds = 121), s(2, seconds = 200), s(1))
        assertEquals(3, d.level)
        assertEquals(ChangeReason.SLOW, d.reason)
    }

    @Test
    fun slowTimeIsIgnoredWhenSessionHadSnoozes() {
        // El tiempo incluye los snoozes: no indica bloqueo.
        val d = decide(3, s(3, snoozes = 1, seconds = 600), s(2, snoozes = 1, seconds = 600), s(1))
        assertEquals(ChangeReason.SNOOZES, d.reason)
        assertEquals(4, d.level)
    }

    @Test
    fun lowerWinsOverRaiseWhenSignalsConflict() {
        // Dos sesiones con snoozes (subir) y dos con 3 fallos (bajar): prevalece bajar.
        val d = decide(3, s(3, snoozes = 1, failures = 3), s(2, snoozes = 1, failures = 3), s(1))
        assertEquals(2, d.level)
        assertEquals(ChangeReason.FAILURES, d.reason)
    }

    @Test
    fun upperLimitHolds() {
        val d = decide(5, s(3, snoozes = 1), s(2, snoozes = 1), s(1))
        assertEquals(5, d.level)
        assertFalse(d.changed)
    }

    @Test
    fun customMaxHolds() {
        assertEquals(3, decide(3, s(3, snoozes = 1), s(2, snoozes = 1), s(1), max = 3).level)
    }

    @Test
    fun lowerLimitHolds() {
        val d = decide(1, s(3, failures = 3), s(2, failures = 3), s(1))
        assertEquals(1, d.level)
        assertFalse(d.changed)
    }

    @Test
    fun levelOutsideRangeIsClamped() {
        assertEquals(4, decide(5, s(3), s(2), s(1), max = 4).level)
        assertEquals(2, decide(1, s(3), s(2), s(1), min = 2).level)
    }

    @Test
    fun sessionsUpToTheLastChangeDoNotCount() {
        // El cambio ocurrió tras s3: solo s5 y s4 son nuevas (menos de 3) → sin cambio.
        val recent = arrayOf(s(5, snoozes = 1), s(4, snoozes = 1), s(3, snoozes = 1), s(2, snoozes = 1), s(1))
        assertFalse(decide(4, *recent, marker = "s3").changed)
    }

    @Test
    fun nextChangeNeedsThreeNewSessions() {
        val recent = arrayOf(s(6, snoozes = 1), s(5, snoozes = 1), s(4), s(3, snoozes = 1))
        val d = decide(4, *recent, marker = "s3")
        assertTrue(d.changed)
        assertEquals(5, d.level)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rangeMustBeOrdered() {
        decide(3, min = 4, max = 2)
    }
}
