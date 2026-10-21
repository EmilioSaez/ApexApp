package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.Alarm
import java.time.Clock
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeGateway(var canExact: Boolean = true) : AlarmClockGateway {
    val scheduled = mutableMapOf<Long, Long>()

    override fun canScheduleExact() = canExact
    override fun setAlarmClock(alarmId: Long, triggerAtMillis: Long) {
        scheduled[alarmId] = triggerAtMillis
    }

    override fun cancel(alarmId: Long) {
        scheduled.remove(alarmId)
    }
}

class AlarmClockSchedulerTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val now = ZonedDateTime.of(2026, 10, 21, 7, 0, 0, 0, zone) // miércoles
    private val clock = Clock.fixed(now.toInstant(), zone)
    private val gateway = FakeGateway()
    private val scheduler = AlarmClockScheduler(gateway, clock)

    private fun millis(day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun schedule_setsNextOccurrence() {
        val result = scheduler.schedule(Alarm(id = 1, hour = 8, minute = 0))
        assertEquals(ScheduleResult.Scheduled, result)
        assertEquals(millis(21, 8, 0), gateway.scheduled[1])
    }

    @Test
    fun schedule_usesRepeatDays() {
        scheduler.schedule(Alarm(id = 1, hour = 8, minute = 0, repeatDays = setOf(DayOfWeek.FRIDAY)))
        assertEquals(millis(23, 8, 0), gateway.scheduled[1])
    }

    @Test
    fun schedule_replacesPreviousForSameAlarm() {
        scheduler.schedule(Alarm(id = 1, hour = 8, minute = 0))
        scheduler.schedule(Alarm(id = 1, hour = 9, minute = 0))
        assertEquals(1, gateway.scheduled.size)
        assertEquals(millis(21, 9, 0), gateway.scheduled[1])
    }

    @Test
    fun schedule_disabledCancelsAndReportsNotScheduled() {
        scheduler.schedule(Alarm(id = 1, hour = 8, minute = 0))
        val result = scheduler.schedule(Alarm(id = 1, hour = 8, minute = 0, enabled = false))
        assertEquals(ScheduleResult.NotScheduled, result)
        assertTrue(gateway.scheduled.isEmpty())
    }

    @Test
    fun schedule_withoutExactPermission_reportsAndDoesNotSchedule() {
        gateway.canExact = false
        val result = scheduler.schedule(Alarm(id = 1, hour = 8, minute = 0))
        assertEquals(ScheduleResult.PermissionMissing, result)
        assertTrue(gateway.scheduled.isEmpty())
    }

    @Test
    fun cancel_removesSchedule() {
        scheduler.schedule(Alarm(id = 1, hour = 8, minute = 0))
        scheduler.cancel(1)
        assertTrue(gateway.scheduled.isEmpty())
    }
}
