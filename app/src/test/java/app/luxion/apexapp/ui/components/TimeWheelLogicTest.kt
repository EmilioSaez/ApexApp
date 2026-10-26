package app.luxion.apexapp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeWheelLogicTest {
    @Test
    fun hour12_coversMidnightAndNoon() {
        assertEquals(12, hour12Of(0))
        assertEquals(1, hour12Of(1))
        assertEquals(11, hour12Of(11))
        assertEquals(12, hour12Of(12))
        assertEquals(1, hour12Of(13))
        assertEquals(11, hour12Of(23))
    }

    @Test
    fun period_splitsAtNoon() {
        assertFalse(isPm(0))
        assertFalse(isPm(11))
        assertTrue(isPm(12))
        assertTrue(isPm(23))
    }

    @Test
    fun hour24_roundTripsEveryHour() {
        for (h in 0..23) assertEquals(h, hour24Of(hour12Of(h), isPm(h)))
        assertEquals(0, hour24Of(12, pm = false))
        assertEquals(12, hour24Of(12, pm = true))
        assertEquals(23, hour24Of(11, pm = true))
    }

    @Test
    fun changingHourOrPeriod_keepsTheOther() {
        assertEquals(19, withHour12(hour24 = 20, hour12 = 7))
        assertEquals(7, withHour12(hour24 = 8, hour12 = 7))
        assertEquals(19, withPeriod(hour24 = 7, pm = true))
        assertEquals(0, withPeriod(hour24 = 12, pm = false))
    }

    @Test
    fun entryDigits_followsTheClockFormat() {
        assertEquals("0705", entryDigits(7, 5, is24Hour = true))
        assertEquals("2359", entryDigits(23, 59, is24Hour = true))
        assertEquals("1100", entryDigits(23, 0, is24Hour = false))
        assertEquals("1200", entryDigits(0, 0, is24Hour = false))
    }

    @Test
    fun parse24Hour_acceptsTheBoundaries() {
        assertEquals(7 to 5, parseTimeEntry("0705", true, false))
        assertEquals(7 to 5, parseTimeEntry("705", true, false))
        assertEquals(12 to 0, parseTimeEntry("12", true, false))
        assertEquals(0 to 0, parseTimeEntry("0", true, false))
        assertEquals(0 to 0, parseTimeEntry("0000", true, false))
        assertEquals(23 to 59, parseTimeEntry("2359", true, false))
        assertEquals(7 to 0, parseTimeEntry("7", true, false))
    }

    @Test
    fun parse_rejectsInvalidValues() {
        assertNull(parseTimeEntry("", true, false))
        assertNull(parseTimeEntry("24", true, false))
        assertNull(parseTimeEntry("2400", true, false))
        assertNull(parseTimeEntry("0760", true, false))
        assertNull(parseTimeEntry("765", true, false))
        assertNull(parseTimeEntry("12345", true, false))
        assertNull(parseTimeEntry("07:5", true, false))
        assertNull(parseTimeEntry("ab", true, false))
    }

    @Test
    fun parse12Hour_usesThePeriod() {
        assertEquals(7 to 5, parseTimeEntry("705", false, pm = false))
        assertEquals(19 to 5, parseTimeEntry("705", false, pm = true))
        assertEquals(0 to 0, parseTimeEntry("1200", false, pm = false))
        assertEquals(12 to 0, parseTimeEntry("12", false, pm = true))
        assertEquals(23 to 59, parseTimeEntry("1159", false, pm = true))
        assertNull(parseTimeEntry("0", false, pm = false))
        assertNull(parseTimeEntry("1300", false, pm = false))
    }
}
