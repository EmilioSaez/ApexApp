package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.Alarm
import java.time.Clock
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmFiringHandlerTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val clock = Clock.fixed(ZonedDateTime.of(2026, 10, 21, 8, 0, 0, 0, zone).toInstant(), zone)
    private val gateway = FakeGateway()
    private val repository = FakeAlarmRepository()
    private val notified = mutableListOf<Alarm>()
    private val handler = AlarmFiringHandler(
        repository,
        AlarmClockScheduler(gateway, clock),
        object : AlarmNotifier {
            override fun notify(alarm: Alarm) {
                notified += alarm
            }
        },
    )

    @Test
    fun oneShot_notifiesAndDisables() = runBlocking {
        val id = repository.save(Alarm(hour = 8, minute = 0))
        handler.onFire(id)
        assertEquals(1, notified.size)
        assertFalse(repository.get(id)!!.enabled)
        assertTrue(gateway.scheduled.isEmpty())
    }

    @Test
    fun repeating_notifiesAndSchedulesNext() = runBlocking {
        val id = repository.save(Alarm(hour = 8, minute = 0, repeatDays = setOf(DayOfWeek.WEDNESDAY)))
        handler.onFire(id)
        assertEquals(1, notified.size)
        assertTrue(repository.get(id)!!.enabled)
        val next = ZonedDateTime.of(2026, 10, 28, 8, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(next, gateway.scheduled[id])
    }

    @Test
    fun disabledOrMissingAlarm_doesNothing() = runBlocking {
        val id = repository.save(Alarm(hour = 8, minute = 0, enabled = false))
        handler.onFire(id)
        handler.onFire(999)
        assertTrue(notified.isEmpty())
    }
}
