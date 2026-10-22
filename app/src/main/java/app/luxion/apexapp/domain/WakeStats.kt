package app.luxion.apexapp.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Una sesión es puntual si se completa dentro de este margen desde su hora programada. */
val PUNCTUAL_WINDOW: Duration = Duration.ofMinutes(10)

val WakeSession.isPunctual: Boolean
    get() = status == WakeSessionStatus.COMPLETED &&
        completedAt!! - scheduledAt <= PUNCTUAL_WINDOW.toMillis()

/** Las métricas con `null` no tienen datos (rango vacío), no valen cero. */
data class WakeStats(
    val completed: Int,
    val notCompleted: Int,
    val averageTimeToWakeMs: Long?,
    val totalSnoozes: Int,
    val averageSnoozes: Double?,
    val totalFails: Int,
    val averageFails: Double?,
    val punctualRate: Double?,
)

/** Las sesiones pendientes (aún en curso) no cuentan en ninguna métrica. */
fun computeStats(sessions: List<WakeSession>): WakeStats {
    val resolved = sessions.filter { it.status != WakeSessionStatus.PENDING }
    val completed = resolved.filter { it.status == WakeSessionStatus.COMPLETED }
    return WakeStats(
        completed = completed.size,
        notCompleted = resolved.size - completed.size,
        averageTimeToWakeMs = completed.mapNotNull { it.timeToWake }.takeIf { it.isNotEmpty() }?.average()?.toLong(),
        totalSnoozes = resolved.sumOf { it.snoozeCount },
        averageSnoozes = resolved.takeIf { it.isNotEmpty() }?.map { it.snoozeCount }?.average(),
        totalFails = resolved.sumOf { it.failCount },
        averageFails = resolved.takeIf { it.isNotEmpty() }?.map { it.failCount }?.average(),
        punctualRate = resolved.takeIf { it.isNotEmpty() }?.let { r -> r.count { it.isPunctual }.toDouble() / r.size },
    )
}

data class Streak(val current: Int, val best: Int)

/**
 * Un día cuenta si todas sus sesiones resueltas son puntuales; un día con alguna no puntual
 * rompe la racha; un día sin sesiones resueltas ni suma ni rompe.
 */
fun computeStreak(sessions: List<WakeSession>, zone: ZoneId): Streak {
    val days: List<Boolean> = sessions
        .filter { it.status != WakeSessionStatus.PENDING }
        .groupBy { LocalDate.ofInstant(Instant.ofEpochMilli(it.scheduledAt), zone) }
        .toSortedMap()
        .values
        .map { day -> day.all { it.isPunctual } }
    var best = 0
    var run = 0
    for (ok in days) {
        run = if (ok) run + 1 else 0
        best = maxOf(best, run)
    }
    return Streak(current = run, best = best)
}
