package app.luxion.apexapp.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class BedtimeSource { MANUAL, ESTIMATED }

/** Una noche con hora de acostarse conocida y despertar completado. */
data class SleepNight(
    val wakeDate: LocalDate,
    val bedtimeAt: Long,
    val wakeAt: Long,
    val source: BedtimeSource,
) {
    val sleepMs: Long get() = wakeAt - bedtimeAt
}

/**
 * Horas de sueño por noche. El despertar es la primera sesión completada de cada día.
 * [manualBedtimes] se indexa por fecha del despertar; el registro manual prevalece sobre
 * la hora habitual [usualBedtime]. Sin ninguno, o con duración no positiva, la noche no cuenta.
 */
fun computeSleepNights(
    sessions: List<WakeSession>,
    manualBedtimes: Map<LocalDate, Long>,
    usualBedtime: LocalTime?,
    zone: ZoneId,
): List<SleepNight> =
    sessions.filter { it.status == WakeSessionStatus.COMPLETED }
        .groupBy { Instant.ofEpochMilli(it.completedAt!!).atZone(zone).toLocalDate() }
        .toSortedMap()
        .mapNotNull { (date, day) ->
            val wakeAt = day.minOf { it.completedAt!! }
            val manual = manualBedtimes[date]
            val night = when {
                manual != null -> SleepNight(date, manual, wakeAt, BedtimeSource.MANUAL)
                usualBedtime != null -> SleepNight(date, estimate(date, usualBedtime, wakeAt, zone), wakeAt, BedtimeSource.ESTIMATED)
                else -> return@mapNotNull null
            }
            night.takeIf { it.sleepMs > 0 }
        }

/** Último instante con la hora habitual anterior al despertar (23:00 cae en la víspera, 01:00 el mismo día). */
private fun estimate(wakeDate: LocalDate, usual: LocalTime, wakeAt: Long, zone: ZoneId): Long {
    val sameDay = wakeDate.atTime(usual).atZone(zone).toInstant().toEpochMilli()
    return if (sameDay < wakeAt) sameDay else wakeDate.minusDays(1).atTime(usual).atZone(zone).toInstant().toEpochMilli()
}
