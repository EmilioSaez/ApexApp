package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.Alarm
import java.time.Clock
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeRingingLauncher(var canStart: Boolean = true) : RingingLauncher {
    val started = mutableListOf<RingingSession>()

    override fun start(session: RingingSession): Boolean {
        if (canStart) started += session
        return canStart
    }
}

class AlarmFiringHandlerTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val clock = Clock.fixed(ZonedDateTime.of(2026, 10, 21, 8, 0, 0, 0, zone).toInstant(), zone)
    private val gateway = FakeGateway()
    private val repository = FakeAlarmRepository()
    private val fireState = FakeFireStateRepository()
    private val registry = RingingRegistry()
    private val launcher = FakeRingingLauncher()
    private val notified = mutableListOf<Alarm>()
    private val sessions = mutableListOf<String>()
    private var counter = 0
    private val handler = AlarmFiringHandler(
        repository,
        AlarmClockScheduler(gateway, clock, fireState),
        registry,
        launcher,
        object : AlarmNotifier {
            override fun notify(alarm: Alarm, sessionId: String) {
                notified += alarm
                sessions += sessionId
            }

            override fun cancel(alarmId: Long) = Unit
        },
        object : SessionIdGenerator {
            override fun next() = "session-${++counter}"
        },
        fireState,
        clock,
    )

    @Test
    fun oneShot_startsRingingAndDisables() = runBlocking {
        val id = repository.save(Alarm(hour = 8, minute = 0))
        val session = handler.onFire(id)
        assertNotNull(session)
        assertEquals(listOf(session), launcher.started)
        assertFalse(repository.get(id)!!.enabled)
        assertTrue(gateway.scheduled.isEmpty())
        assertEquals(clock.millis(), fireState.get(id)!!.lastFiredAt)
    }

    @Test
    fun repeating_startsRingingAndSchedulesNext() = runBlocking {
        val id = repository.save(Alarm(hour = 8, minute = 0, repeatDays = setOf(DayOfWeek.WEDNESDAY)))
        handler.onFire(id)
        assertEquals(1, launcher.started.size)
        assertTrue(repository.get(id)!!.enabled)
        val next = ZonedDateTime.of(2026, 10, 28, 8, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(next, gateway.scheduled[id])
    }

    @Test
    fun everyFiring_getsItsOwnSessionId() = runBlocking {
        val id = repository.save(Alarm(hour = 8, minute = 0, repeatDays = setOf(DayOfWeek.WEDNESDAY)))
        val first = handler.onFire(id)!!
        registry.end(first.sessionId)
        val second = handler.onFire(id)!!
        assertEquals(listOf("session-1", "session-2"), listOf(first.sessionId, second.sessionId))
    }

    @Test
    fun duplicateFire_whileRinging_startsOnlyOnce() = runBlocking {
        val id = repository.save(Alarm(hour = 8, minute = 0, repeatDays = setOf(DayOfWeek.WEDNESDAY)))
        handler.onFire(id)
        assertNull(handler.onFire(id))
        assertEquals(1, launcher.started.size)
    }

    @Test
    fun disabledOrMissingAlarm_doesNothing() = runBlocking {
        val id = repository.save(Alarm(hour = 8, minute = 0, enabled = false))
        assertNull(handler.onFire(id))
        assertNull(handler.onFire(999))
        assertTrue(launcher.started.isEmpty())
    }

    @Test
    fun givenSessionId_reusesItAndKeepsAlarmState() = runBlocking {
        val id = repository.save(Alarm(hour = 8, minute = 0, enabled = false))
        val session = handler.onFire(id, sessionId = "abc")
        assertEquals("abc", session!!.sessionId)
        assertFalse(repository.get(id)!!.enabled)
        assertTrue(gateway.scheduled.isEmpty())
    }

    @Test
    fun serviceCannotStart_fallsBackToNotificationWithSameSession() = runBlocking {
        launcher.canStart = false
        val id = repository.save(Alarm(hour = 8, minute = 0))
        val session = handler.onFire(id)!!
        assertEquals(1, notified.size)
        assertEquals(listOf(session.sessionId), sessions)
        assertTrue(registry.isEmpty())
    }

    @Test
    fun testAlarm_ringsWithoutTouchingStorage() = runBlocking {
        val session = handler.onFire(TEST_ALARM_ID)
        assertNotNull(session)
        assertEquals(1, launcher.started.size)
        assertNull(fireState.get(TEST_ALARM_ID))
    }
}
