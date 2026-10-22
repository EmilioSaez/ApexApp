package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.WakeEventRecorder
import app.luxion.apexapp.domain.WakeEventType
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class MutableClock(var instant: Instant, private val zone: ZoneId) : Clock() {
    override fun getZone() = zone
    override fun withZone(zone: ZoneId) = MutableClock(instant, zone)
    override fun instant() = instant
}

private class RecordingRecorder : WakeEventRecorder {
    data class Event(val type: WakeEventType, val alarmId: Long?, val scheduledAt: Long?)

    val events = mutableListOf<Event>()

    override suspend fun record(
        sessionId: String,
        type: WakeEventType,
        alarmId: Long?,
        timestamp: Long,
        scheduledAt: Long?,
        challengeType: String?,
        difficulty: Int?,
    ) {
        events += Event(type, alarmId, scheduledAt)
    }
}

class AlarmControllerTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val start = ZonedDateTime.of(2026, 10, 21, 7, 0, 0, 0, zone) // miércoles
    private val clock = MutableClock(start.toInstant(), zone)
    private val gateway = FakeGateway()
    private val repository = FakeAlarmRepository()
    private val fireState = FakeFireStateRepository()
    private val launcher = FakeRingingLauncher()
    private val recorder = RecordingRecorder()
    private val scheduler = AlarmClockScheduler(gateway, clock, fireState)
    private val handler = AlarmFiringHandler(
        repository, scheduler, RingingRegistry(), launcher,
        NoOpNotifier, CountingSessionIds(),
        fireState, clock,
    )
    private val controller = AlarmController(repository, scheduler, fireState, handler, recorder, clock)

    private fun at(day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone)

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
        assertNull(fireState.get(id))
    }

    @Test
    fun rescheduleAll_onlyActiveAlarms() = runBlocking {
        repository.save(Alarm(hour = 8, minute = 0))
        repository.save(Alarm(hour = 9, minute = 0, enabled = false))
        controller.rescheduleAll()
        assertEquals(1, gateway.scheduled.size)
    }

    @Test
    fun timeChange_reschedulesAccordingToNewLocalTime() = runBlocking {
        controller.save(Alarm(hour = 8, minute = 0, repeatDays = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)))
        val id = repository.getEnabled().single().id
        // Cambio de zona a Nueva York: la misma alarma pasa a sonar a las 8:00 hora local de allí.
        val newYork = ZoneId.of("America/New_York")
        val nyClock = Clock.fixed(start.toInstant(), newYork)
        val nyScheduler = AlarmClockScheduler(gateway, nyClock, fireState)
        val nyHandler = AlarmFiringHandler(repository, nyScheduler, RingingRegistry(), launcher, NoOpNotifier, CountingSessionIds(), fireState, nyClock)
        AlarmController(repository, nyScheduler, fireState, nyHandler, recorder, nyClock).rescheduleAll()
        val expected = ZonedDateTime.of(2026, 10, 21, 8, 0, 0, 0, newYork).toInstant().toEpochMilli()
        assertEquals(expected, gateway.scheduled[id])
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun missedWithinWindow_ringsImmediatelyAndReschedules() = runBlocking {
        controller.save(Alarm(hour = 8, minute = 0, repeatDays = setOf(DayOfWeek.WEDNESDAY)))
        val id = repository.getEnabled().single().id
        clock.instant = at(21, 8, 5).toInstant() // el dispositivo estaba apagado a las 8:00
        controller.rescheduleAll()
        assertEquals(1, launcher.started.size)
        assertEquals(at(28, 8, 0).toInstant().toEpochMilli(), gateway.scheduled[id])
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun missedOutsideWindow_isRecordedAndRepeatingIsRescheduled() = runBlocking {
        controller.save(Alarm(hour = 8, minute = 0, repeatDays = setOf(DayOfWeek.WEDNESDAY)))
        val id = repository.getEnabled().single().id
        clock.instant = at(21, 9, 0).toInstant()
        controller.rescheduleAll()
        assertTrue(launcher.started.isEmpty())
        assertEquals(
            listOf(RecordingRecorder.Event(WakeEventType.MISSED, id, at(21, 8, 0).toInstant().toEpochMilli())),
            recorder.events,
        )
        assertEquals(at(28, 8, 0).toInstant().toEpochMilli(), gateway.scheduled[id])
    }

    @Test
    fun missedOutsideWindow_oneShotIsDisabled() = runBlocking {
        controller.save(Alarm(hour = 8, minute = 0))
        val id = repository.getEnabled().single().id
        clock.instant = at(21, 12, 0).toInstant()
        controller.rescheduleAll()
        assertEquals(WakeEventType.MISSED, recorder.events.single().type)
        assertFalse(repository.get(id)!!.enabled)
        assertTrue(gateway.scheduled.isEmpty())
    }

    @Test
    fun alarmAlreadyFired_isNotTreatedAsMissed() = runBlocking {
        controller.save(Alarm(hour = 8, minute = 0, repeatDays = setOf(DayOfWeek.WEDNESDAY)))
        val id = repository.getEnabled().single().id
        clock.instant = at(21, 8, 0).toInstant()
        handler.onFire(id)
        launcher.started.clear()
        clock.instant = at(21, 8, 30).toInstant()
        controller.rescheduleAll()
        assertTrue(launcher.started.isEmpty())
        assertTrue(recorder.events.isEmpty())
    }
}
