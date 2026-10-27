package app.luxion.apexapp.ui.stats

import app.luxion.apexapp.domain.SleepNight
import app.luxion.apexapp.domain.Streak
import app.luxion.apexapp.domain.WakeSession
import app.luxion.apexapp.domain.WakeSessionStatus
import app.luxion.apexapp.domain.WakeStats
import app.luxion.apexapp.domain.computeSleepNights
import app.luxion.apexapp.domain.computeStats
import app.luxion.apexapp.domain.computeStreak
import app.luxion.apexapp.domain.isPunctual
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class StatsRange(val days: Int) { WEEK(7), MONTH(30), QUARTER(90) }

/** Valores de un día del rango; `null` = sin datos ese día (no cero). */
data class DayPoint(
    val date: LocalDate,
    val timeToWakeMs: Long?,
    val snoozes: Int,
    val fails: Int,
    val punctualRate: Double?,
)

data class StatsUiState(
    val range: StatsRange = StatsRange.WEEK,
    val isLoading: Boolean = true,
    val hasData: Boolean = false,
    val stats: WakeStats = computeStats(emptyList()),
    val streak: Streak = Streak(0, 0),
    val days: List<DayPoint> = emptyList(),
    val nights: List<SleepNight> = emptyList(),
    val averageSleepMs: Long? = null,
    val usualBedtime: LocalTime? = null,
)

/** Un punto por día del rango, de más antiguo a más reciente, incluidos los días sin sesiones. */
fun buildStatsState(
    range: StatsRange,
    sessions: List<WakeSession>,
    allSessionsForStreak: List<WakeSession>,
    manualBedtimes: Map<LocalDate, Long>,
    usualBedtime: LocalTime?,
    today: LocalDate,
    zone: ZoneId,
): StatsUiState {
    val dates = (range.days - 1 downTo 0).map { today.minusDays(it.toLong()) }
    val byDay = sessions
        .filter { it.status != WakeSessionStatus.PENDING }
        .groupBy { Instant.ofEpochMilli(it.scheduledAt).atZone(zone).toLocalDate() }
    val days = dates.map { date ->
        val day = byDay[date].orEmpty()
        val completed = day.filter { it.status == WakeSessionStatus.COMPLETED }
        DayPoint(
            date = date,
            timeToWakeMs = completed.mapNotNull { it.timeToWake }.takeIf { it.isNotEmpty() }?.average()?.toLong(),
            snoozes = day.sumOf { it.snoozeCount },
            fails = day.sumOf { it.failCount },
            punctualRate = day.takeIf { it.isNotEmpty() }?.let { d -> d.count { it.isPunctual }.toDouble() / d.size },
        )
    }
    val nights = computeSleepNights(sessions, manualBedtimes, usualBedtime, zone)
    return StatsUiState(
        range = range,
        isLoading = false,
        hasData = sessions.any { it.status != WakeSessionStatus.PENDING },
        stats = computeStats(sessions),
        streak = computeStreak(allSessionsForStreak, zone),
        days = days,
        nights = nights,
        averageSleepMs = nights.map { it.sleepMs }.takeIf { it.isNotEmpty() }?.average()?.toLong(),
        usualBedtime = usualBedtime,
    )
}
