package app.luxion.apexapp.domain.challenge.touch

import app.luxion.apexapp.domain.AntiCheatLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchPlausibilityTest {
    private fun press(t: Long, touch: Boolean = true, obscured: Boolean = false) =
        TouchSample(t, TouchAction.PRESS, touch, obscured)

    @Test
    fun off_acceptsEverything() {
        val p = TouchPlausibility(AntiCheatLevel.OFF)
        assertEquals(TouchVerdict.Accepted, p.check(press(0, touch = false, obscured = true)))
        assertEquals(TouchVerdict.Accepted, p.check(press(1)))
    }

    @Test
    fun standard_rejectsNonTouchscreenAndObscured() {
        val p = TouchPlausibility(AntiCheatLevel.STANDARD)
        assertEquals(TouchVerdict.Rejected(TouchVerdict.Reason.NOT_TOUCHSCREEN), p.check(press(0, touch = false)))
        assertEquals(TouchVerdict.Rejected(TouchVerdict.Reason.OBSCURED), p.check(press(100, obscured = true)))
    }

    @Test
    fun standard_rejectsInhumanSpeed() {
        val p = TouchPlausibility(AntiCheatLevel.STANDARD)
        assertEquals(TouchVerdict.Accepted, p.check(press(1000)))
        assertEquals(TouchVerdict.Rejected(TouchVerdict.Reason.TOO_FAST), p.check(press(1030)))
        assertEquals(TouchVerdict.Accepted, p.check(press(1100)))
    }

    @Test
    fun standard_toleratesPerfectRegularity() {
        val p = TouchPlausibility(AntiCheatLevel.STANDARD)
        assertTrue((0..9).all { p.check(press(it * 500L)) == TouchVerdict.Accepted })
    }

    @Test
    fun release_isNotSubjectToSpeedOrRegularity() {
        val p = TouchPlausibility(AntiCheatLevel.STRICT)
        p.check(press(1000))
        assertEquals(TouchVerdict.Accepted, p.check(TouchSample(1010, TouchAction.RELEASE)))
    }

    @Test
    fun strict_rejectsSyntheticRegularity() {
        val p = TouchPlausibility(AntiCheatLevel.STRICT)
        val verdicts = (0..6).map { p.check(press(it * 500L)) }
        assertTrue(verdicts.take(5).all { it == TouchVerdict.Accepted })
        assertEquals(TouchVerdict.Rejected(TouchVerdict.Reason.TOO_REGULAR), verdicts[5])
    }

    @Test
    fun strict_acceptsHumanJitter() {
        val p = TouchPlausibility(AntiCheatLevel.STRICT)
        val times = listOf(0L, 520, 1130, 1590, 2240, 2790, 3400, 3910)
        assertTrue(times.all { p.check(press(it)) == TouchVerdict.Accepted })
    }

    @Test
    fun strict_notifiesEachRejection_standardDoesNot() {
        var strict = 0
        var standard = 0
        TouchPlausibility(AntiCheatLevel.STRICT) { strict++ }.apply {
            check(press(0, touch = false))
            check(press(10, obscured = true))
            check(press(20))
        }
        TouchPlausibility(AntiCheatLevel.STANDARD) { standard++ }.check(press(0, touch = false))
        assertEquals(2, strict)
        assertEquals(0, standard)
    }
}
