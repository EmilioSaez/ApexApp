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
    fun release_isNotSubjectToSpeed() {
        val p = TouchPlausibility(AntiCheatLevel.STANDARD)
        p.check(press(1000))
        assertEquals(TouchVerdict.Accepted, p.check(TouchSample(1010, TouchAction.RELEASE)))
    }
}
