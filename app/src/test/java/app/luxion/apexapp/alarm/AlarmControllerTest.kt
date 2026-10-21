package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.Alarm
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmControllerTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val clock = Clock.fixed(ZonedDateTime.of(2026, 10, 21, 7, 0, 0, 0, zone).toInstant(), zone)
    private val gateway = FakeGateway()
    private val repository = FakeAlarmRepository()
    private val controller = AlarmController(repository, AlarmClockScheduler(gateway, clock))

    @Test
    fun save_persistsAndSchedulesWithAssignedId() = runBlocking {
        val result = controller.save(Alarm(hour = 8, minute = 0))
        assertEquals(ScheduleResult.Scheduled, result)
        val id = repository.getEnabled().single().id
        assertTrue(id in gateway.scheduled)
    }

    @Test
    fun setEnabledFalse_cancelsSchedule() = runBlocking {
        controller.save(Alarm(hour = 8, minute = 0))
        val id = repository.getEnabled().single().id
        controller.setEnabled(id, false)
        assertTrue(gateway.scheduled.isEmpty())
        assertEquals(false, repository.get(id)!!.enabled)
    }

    @Test
    fun delete_cancelsAndRemoves() = runBlocking {
        controller.save(Alarm(hour = 8, minute = 0))
        val id = repository.getEnabled().single().id
        controller.delete(id)
        assertTrue(gateway.scheduled.isEmpty())
        assertNull(repository.get(id))
    }

    @Test
    fun rescheduleAll_onlyActiveAlarms() = runBlocking {
        repository.save(Alarm(hour = 8, minute = 0))
        repository.save(Alarm(hour = 9, minute = 0, enabled = false))
        controller.rescheduleAll()
        assertEquals(1, gateway.scheduled.size)
    }
}
