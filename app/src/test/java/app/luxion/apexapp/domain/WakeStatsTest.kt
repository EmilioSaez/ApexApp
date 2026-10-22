package app.luxion.apexapp.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeStatsTest {
    private val zone = ZoneOffset.UTC
    private val min = 60_000L

    private fun at(day: Int, hour: Int, minute: Int = 0, second: Int = 0): Long =
        LocalDate.of(2026, 3, day).atTime(hour, minute, second).atZone(zone).toInstant().toEpochMilli()

    private val now = Instant.ofEpochMilli(at(30, 12))

    private fun ev(id: String, type: WakeEventType, ts: Long, scheduledAt: Long? = null, alarmId: Long? = 1) =
        WakeEvent(id, type, alarmId, ts, scheduledAt)

    /** Sesión completa: suena a la hora programada y se completa [wakeAfter] ms después. */
    private fun day(id: String, d: Int, wakeAfter: Long, snoozes: Int = 0, fails: Int = 0): List<WakeEvent> {
        val sched = at(d, 7)
        return buildList {
            add(ev(id, WakeEventType.RANG, sched, sched))
            repeat(snoozes) { add(ev(id, WakeEventType.SNOOZED, sched + 1)) }
            repeat(fails) { add(ev(id, WakeEventType.CHALLENGE_FAILED, sched + 2)) }
            add(ev(id, WakeEventType.COMPLETED, sched + wakeAfter))
        }
    }

    private fun sessions(vararg events: List<WakeEvent>) = groupSessions(events.flatMap { it }, now)

    @Test
    fun timeToWakeIsCompletedMinusFired() {
        val s = sessions(day("a", 1, 150_000)).single()
        assertEquals(150_000L, s.timeToWake)
        assertEquals(150_000L, computeStats(listOf(s)).averageTimeToWakeMs)
    }

    @Test
    fun emptyRangeHasNoMetrics() {
        val stats = computeStats(emptyList())
        assertNull(stats.averageTimeToWakeMs)
        assertNull(stats.punctualRate)
        assertEquals(0, stats.completed)
    }

    @Test
    fun duplicateCompletedAndRangAreCountedOnce() {
        val events = day("a", 1, min) + ev("a", WakeEventType.COMPLETED, at(1, 7, 5)) + ev("a", WakeEventType.RANG, at(1, 7, 3))
        val s = groupSessions(events, now).single()
        assertEquals(at(1, 7), s.firedAt)
        assertEquals(at(1, 7, 1), s.completedAt)
    }

    @Test
    fun snoozesAndFailsAreCounted() {
        val s = sessions(day("a", 1, 5 * min, snoozes = 2, fails = 3)).single()
        assertEquals(2, s.snoozeCount)
        assertEquals(3, s.failCount)
    }

    @Test
    fun sessionUnresolvedAfter24hIsNotCompleted() {
        val recent = groupSessions(listOf(ev("r", WakeEventType.RANG, at(30, 7), at(30, 7))), now).single()
        assertEquals(WakeSessionStatus.PENDING, recent.status)
        val old = groupSessions(listOf(ev("o", WakeEventType.RANG, at(28, 7), at(28, 7))), now).single()
        assertEquals(WakeSessionStatus.NOT_COMPLETED, old.status)
        assertNull(old.timeToWake)
    }

    @Test
    fun missedSessionIsNotCompletedImmediately() {
        val s = groupSessions(listOf(ev("m", WakeEventType.MISSED, at(30, 11), at(30, 7))), now).single()
        assertEquals(WakeSessionStatus.NOT_COMPLETED, s.status)
        assertEquals(0, s.failCount)
        assertEquals(0, s.snoozeCount)
    }

    @Test
    fun suspiciousEventMarksSession() {
        val s = sessions(day("a", 1, min) + ev("a", WakeEventType.SUSPICIOUS, at(1, 7, 1))).single()
        assertTrue(s.suspicious)
    }

    @Test
    fun punctualityWindowIsTenMinutes() {
        val a = sessions(day("a", 1, 8 * min)).single()
        val b = sessions(day("b", 2, 15 * min)).single()
        assertTrue(a.isPunctual)
        assertFalse(b.isPunctual)
        assertEquals(0.5, computeStats(listOf(a, b)).punctualRate!!, 0.0)
    }

    @Test
    fun pendingSessionsAreExcludedFromStats() {
        val pending = groupSessions(listOf(ev("p", WakeEventType.RANG, at(30, 7), at(30, 7))), now)
        assertEquals(0, computeStats(pending).completed + computeStats(pending).notCompleted)
        assertNull(computeStats(pending).punctualRate)
    }

    @Test
    fun streakContinues() {
        val s = sessions(day("a", 1, min), day("b", 2, min), day("c", 3, min))
        assertEquals(Streak(3, 3), computeStreak(s, zone))
    }

    @Test
    fun failedDayResetsCurrentAndKeepsBest() {
        val missed = listOf(ev("x", WakeEventType.MISSED, at(4, 8), at(4, 7)))
        val s = groupSessions(day("a", 1, min) + day("b", 2, min) + day("c", 3, min) + missed, now)
        assertEquals(Streak(0, 3), computeStreak(s, zone))
    }

    @Test
    fun dayWithoutAlarmNeitherAddsNorBreaks() {
        val s = sessions(day("a", 1, min), day("c", 3, min))
        assertEquals(Streak(2, 2), computeStreak(s, zone))
    }

    @Test
    fun sleepManualWinsOverEstimated() {
        val s = sessions(day("a", 2, min))
        val manual = mapOf(LocalDate.of(2026, 3, 2) to at(2, 0, 15))
        val night = computeSleepNights(s, manual, LocalTime.of(23, 0), zone).single()
        assertEquals(BedtimeSource.MANUAL, night.source)
        assertEquals(at(2, 7, 1) - at(2, 0, 15), night.sleepMs)
    }

    @Test
    fun sleepEstimatedUsesPreviousEveningAndSevenHoursThirty() {
        val sched = at(2, 7)
        val events = listOf(
            ev("a", WakeEventType.RANG, sched - 30 * min, sched),
            ev("a", WakeEventType.COMPLETED, sched),
        )
        val night = computeSleepNights(groupSessions(events, now), emptyMap(), LocalTime.of(23, 30), zone).single()
        assertEquals(BedtimeSource.ESTIMATED, night.source)
        assertEquals(at(1, 23, 30), night.bedtimeAt)
        assertEquals(450 * min, night.sleepMs)
    }

    @Test
    fun sleepWithoutBedtimeDataIsSkipped() {
        assertTrue(computeSleepNights(sessions(day("a", 2, min)), emptyMap(), null, zone).isEmpty())
    }

    @Test
    fun sleepSkipsNightsWithoutCompletedWake() {
        val missed = groupSessions(listOf(ev("m", WakeEventType.MISSED, at(2, 8), at(2, 7))), now)
        assertTrue(computeSleepNights(missed, emptyMap(), LocalTime.of(23, 0), zone).isEmpty())
    }
}
