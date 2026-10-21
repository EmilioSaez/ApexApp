package app.luxion.apexapp.domain

import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextOccurrenceTest {
    private val zone = ZoneId.of("Europe/Madrid")

    // 2026-10-21 es miércoles
    private fun clockAt(hour: Int, minute: Int) = Clock.fixed(
        ZonedDateTime.of(2026, 10, 21, hour, minute, 0, 0, zone).toInstant(),
        zone,
    )

    private fun at(day: Int, hour: Int, minute: Int) = ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone)

    @Test
    fun oneShot_laterToday() {
        val alarm = Alarm(hour = 8, minute = 30)
        assertEquals(at(21, 8, 30), alarm.nextOccurrence(clockAt(7, 0)))
    }

    @Test
    fun oneShot_timeAlreadyPassed_goesToTomorrow() {
        val alarm = Alarm(hour = 8, minute = 30)
        assertEquals(at(22, 8, 30), alarm.nextOccurrence(clockAt(9, 0)))
    }

    @Test
    fun oneShot_exactlyNow_goesToTomorrow() {
        val alarm = Alarm(hour = 8, minute = 30)
        assertEquals(at(22, 8, 30), alarm.nextOccurrence(clockAt(8, 30)))
    }

    @Test
    fun oneShot_acrossMidnight() {
        val alarm = Alarm(hour = 0, minute = 5)
        assertEquals(at(22, 0, 5), alarm.nextOccurrence(clockAt(23, 50)))
    }

    @Test
    fun repeating_todayIfStillAhead() {
        val alarm = Alarm(hour = 8, minute = 0, repeatDays = setOf(DayOfWeek.WEDNESDAY))
        assertEquals(at(21, 8, 0), alarm.nextOccurrence(clockAt(7, 0)))
    }

    @Test
    fun repeating_nextMatchingDay() {
        val alarm = Alarm(hour = 8, minute = 0, repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        assertEquals(at(23, 8, 0), alarm.nextOccurrence(clockAt(9, 0)))
    }

    @Test
    fun repeating_sameWeekdayPassed_goesNextWeek() {
        val alarm = Alarm(hour = 8, minute = 0, repeatDays = setOf(DayOfWeek.WEDNESDAY))
        assertEquals(at(28, 8, 0), alarm.nextOccurrence(clockAt(9, 0)))
    }

    @Test
    fun disabled_returnsNull() {
        val alarm = Alarm(hour = 8, minute = 0, enabled = false)
        assertNull(alarm.nextOccurrence(clockAt(7, 0)))
    }

    @Test
    fun dstChange_keepsWallClockTime() {
        // El cambio de hora en Madrid fue el 25-oct-2026 (02:00 -> 03:00 se retrasa a 02:00)
        val alarm = Alarm(hour = 8, minute = 0, repeatDays = setOf(DayOfWeek.SUNDAY))
        assertEquals(at(25, 8, 0), alarm.nextOccurrence(clockAt(9, 0)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidHour_rejected() {
        Alarm(hour = 24, minute = 0)
    }
}
