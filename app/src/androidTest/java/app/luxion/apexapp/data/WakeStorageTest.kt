package app.luxion.apexapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.luxion.apexapp.data.local.ApexDatabase
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.WakeEventType
import app.luxion.apexapp.domain.WakeSessionStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WakeStorageTest {
    private lateinit var db: ApexDatabase
    private lateinit var alarms: RoomAlarmRepository
    private lateinit var recorder: RoomWakeEventRecorder
    private lateinit var history: RoomWakeHistoryRepository
    private lateinit var sleep: RoomSleepRepository

    private val base = Instant.parse("2026-03-10T12:00:00Z").toEpochMilli()
    private val clock = Clock.fixed(Instant.ofEpochMilli(base), ZoneOffset.UTC)
    private val hour = 3_600_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ApexDatabase::class.java).build()
        alarms = RoomAlarmRepository(db.alarmDao())
        recorder = RoomWakeEventRecorder(db, db.wakeEventDao())
        history = RoomWakeHistoryRepository(db.wakeEventDao(), clock)
        sleep = RoomSleepRepository(db.sleepDao())
    }

    @After
    fun tearDown() = db.close()

    /** Sesión completada: suena en [at], se pospone [snoozes] veces y se completa 1 minuto después. */
    private suspend fun session(id: String, alarmId: Long?, at: Long, snoozes: Int = 0, suspicious: Boolean = false) {
        recorder.record(id, WakeEventType.RANG, alarmId, at, scheduledAt = at)
        repeat(snoozes) { recorder.record(id, WakeEventType.SNOOZED, alarmId, at + 1) }
        recorder.record(id, WakeEventType.CHALLENGE_FAILED, alarmId, at + 2, challengeType = "math", difficulty = 3)
        if (suspicious) recorder.record(id, WakeEventType.SUSPICIOUS, alarmId, at + 3)
        recorder.record(id, WakeEventType.COMPLETED, alarmId, at + 60_000)
    }

    @Test
    fun deletingAlarm_keepsEventsWithoutAlarmId() = runBlocking {
        val alarmId = alarms.save(Alarm(hour = 7, minute = 0))
        session("s1", alarmId, base - hour)
        alarms.delete(alarmId)
        val sessions = history.sessions(Instant.ofEpochMilli(base - 24 * hour), Instant.ofEpochMilli(base))
        assertEquals(1, sessions.size)
        assertNull(sessions.single().alarmId)
    }

    @Test
    fun duplicateRangAndCompleted_areIgnored() = runBlocking {
        val alarmId = alarms.save(Alarm(hour = 7, minute = 0))
        session("s1", alarmId, base - hour)
        recorder.record("s1", WakeEventType.RANG, alarmId, base - hour + 5)
        recorder.record("s1", WakeEventType.COMPLETED, alarmId, base - hour + 9)
        assertEquals(1, db.wakeEventDao().count("s1", "RANG"))
        assertEquals(1, db.wakeEventDao().count("s1", "COMPLETED"))
    }

    @Test
    fun observeSessions_emitsOnNewEvent() = runBlocking {
        val alarmId = alarms.save(Alarm(hour = 7, minute = 0))
        val range = Instant.ofEpochMilli(base - 24 * hour) to Instant.ofEpochMilli(base)
        assertTrue(history.observeSessions(range.first, range.second).first().isEmpty())
        session("s1", alarmId, base - hour)
        assertEquals(1, history.observeSessions(range.first, range.second).first().size)
    }

    @Test
    fun sessions_filterByRangeAndChallengeType() = runBlocking {
        val alarmId = alarms.save(Alarm(hour = 7, minute = 0))
        session("old", alarmId, base - 48 * hour)
        session("new", alarmId, base - hour)
        val from = Instant.ofEpochMilli(base - 24 * hour)
        val to = Instant.ofEpochMilli(base)
        assertEquals(listOf("new"), history.sessions(from, to).map { it.sessionId })
        assertEquals(listOf("new"), history.sessions(from, to, challengeType = "math").map { it.sessionId })
        assertTrue(history.sessions(from, to, challengeType = "qr").isEmpty())
    }

    @Test
    fun recentSessions_limitsAndExcludesSuspicious() = runBlocking {
        val alarmId = alarms.save(Alarm(hour = 7, minute = 0))
        for (i in 1..8) session("s$i", alarmId, base - (10 - i) * hour, suspicious = i == 7)
        val recent = history.recentSessions(alarmId, 5, excludeSuspicious = false)
        assertEquals(listOf("s8", "s7", "s6", "s5", "s4"), recent.map { it.sessionId })
        val clean = history.recentSessions(alarmId, 5, excludeSuspicious = true)
        assertEquals(listOf("s8", "s6", "s5", "s4", "s3"), clean.map { it.sessionId })
        assertTrue(recent[1].suspicious)
    }

    @Test
    fun missedSession_isNotCompletedAndStreakBreaks() = runBlocking {
        val alarmId = alarms.save(Alarm(hour = 7, minute = 0))
        recorder.record("m", WakeEventType.MISSED, alarmId, base - hour, scheduledAt = base - 2 * hour)
        val s = history.sessions(Instant.ofEpochMilli(base - 24 * hour), Instant.ofEpochMilli(base)).single()
        assertEquals(WakeSessionStatus.NOT_COMPLETED, s.status)
        assertEquals(0, history.observeStreak().first().current)
    }

    @Test
    fun streak_countsPunctualDays() = runBlocking {
        val alarmId = alarms.save(Alarm(hour = 7, minute = 0))
        session("a", alarmId, base - 48 * hour)
        session("b", alarmId, base - 24 * hour)
        assertEquals(2, history.observeStreak().first().current)
    }

    @Test
    fun sleep_persistsManualBedtimeAndUsualTime() = runBlocking {
        val date = LocalDate.of(2026, 3, 10)
        sleep.setBedtime(date, 123L)
        sleep.setUsualBedtime(LocalTime.of(23, 30))
        assertEquals(mapOf(date to 123L), sleep.observeManualBedtimes().first())
        assertEquals(LocalTime.of(23, 30), sleep.observeUsualBedtime().first())
        sleep.clearBedtime(date)
        assertTrue(sleep.observeManualBedtimes().first().isEmpty())
        sleep.setUsualBedtime(null)
        assertNull(sleep.observeUsualBedtime().first())
    }
}
