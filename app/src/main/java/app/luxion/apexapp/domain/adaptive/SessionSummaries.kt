package app.luxion.apexapp.domain.adaptive

import app.luxion.apexapp.domain.WakeSession
import app.luxion.apexapp.domain.WakeSessionStatus

/** Null si la sesión no tiene resultado fiable: no completada, sospechosa o de otra alarma. */
fun WakeSession.toSummaryOrNull(alarmId: Long): SessionSummary? {
    if (this.alarmId != alarmId || status != WakeSessionStatus.COMPLETED || suspicious) return null
    return SessionSummary(sessionId, snoozeCount, failCount, timeToWake)
}

fun List<WakeSession>.toSummaries(alarmId: Long): List<SessionSummary> = mapNotNull { it.toSummaryOrNull(alarmId) }
