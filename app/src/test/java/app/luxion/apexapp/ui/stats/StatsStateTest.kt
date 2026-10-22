package app.luxion.apexapp.ui.stats

import app.luxion.apexapp.domain.WakeEvent
import app.luxion.apexapp.domain.WakeEventType
import app.luxion.apexapp.domain.groupSessions
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsStateTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 3, 10)
    private val now = Instant.parse("2026-03-10T12:00:00Z")

    private fun at(day: Int, hour: Int, minute: Int = 0) =
        LocalDate.of(2026, 3, day).atTime(hour, minute).toInstant(zone).toEpochMilli()

    private fun completedDay(id: String, day: Int, wakeAfterMs: Long) = listOf(
        WakeEvent(id, WakeEventType.RANG, 1, at(day, 7), at(day, 7)),
        WakeEvent(id, WakeEventType.SNOOZED, 1, at(day, 7) + 1),
        WakeEvent(id, WakeEventType.COMPLETED, 1, at(day, 7) + wakeAfterMs),
    )

    private fun build(range: StatsRange, vararg events: List<WakeEvent>, usual: LocalTime? = null): StatsUiState {
        val sessions = groupSessions(events.flatMap { it }, now)
        return buildStatsState(range, sessions, sessions, emptyMap(), usual, today, zone)
    }

    @Test
    fun noSessions_hasNoDataAndFullRangeOfEmptyDays() {
        val state = build(StatsRange.WEEK)
        assertFalse(state.hasData)
        assertEquals(7, state.days.size)
        assertTrue(state.days.all { it.timeToWakeMs == null && it.punctualRate == null })
        assertNull(state.averageSleepMs)
    }

    @Test
    fun rangeDaysAreOrderedOldestToToday() {
        val state = build(StatsRange.QUARTER)
        assertEquals(90, state.days.size)
        assertEquals(today, state.days.last().date)
        assertEquals(today.minusDays(89), state.days.first().date)
    }

    @Test
    fun sessionsLandOnTheirDay() {
        val state = build(StatsRange.WEEK, completedDay("a", 9, 120_000), completedDay("b", 10, 60_000))
        assertTrue(state.hasData)
        assertEquals(60_000L, state.days.last().timeToWakeMs)
        assertEquals(120_000L, state.days[5].timeToWakeMs)
        assertEquals(1, state.days.last().snoozes)
        assertEquals(1.0, state.days.last().punctualRate!!, 0.0)
        assertEquals(2, state.streak.current)
    }

    @Test
    fun estimatedSleepIsAveraged() {
        val state = build(StatsRange.WEEK, completedDay("a", 9, 0), usual = LocalTime.of(23, 0))
        assertEquals(1, state.nights.size)
        assertEquals(8 * 3_600_000L, state.averageSleepMs)
    }
}
