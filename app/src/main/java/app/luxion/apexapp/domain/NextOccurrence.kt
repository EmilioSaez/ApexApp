package app.luxion.apexapp.domain

import java.time.Clock
import java.time.LocalDateTime
import java.time.ZonedDateTime

/**
 * Próxima ocurrencia estrictamente posterior al instante actual del [clock],
 * o `null` si la alarma está inactiva.
 */
fun Alarm.nextOccurrence(clock: Clock): ZonedDateTime? {
    if (!enabled) return null
    val now = ZonedDateTime.now(clock)
    // Con repetición hay que mirar hoy y los 7 días siguientes; sin ella, hoy y mañana.
    for (offset in 0L..7L) {
        val date = now.toLocalDate().plusDays(offset)
        if (repeatDays.isNotEmpty() && date.dayOfWeek !in repeatDays) continue
        val candidate = LocalDateTime.of(date, java.time.LocalTime.of(hour, minute)).atZone(now.zone)
        if (candidate.isAfter(now)) return candidate
    }
    return null
}
