package app.luxion.apexapp.domain

import java.time.Duration
import java.time.Instant

enum class WakeSessionStatus {
    /** Sonó hace menos de 24 h y aún no se ha completado. */
    PENDING,
    COMPLETED,

    /** Perdida, o sin completar pasadas 24 h. */
    NOT_COMPLETED,
}

data class WakeSession(
    val sessionId: String,
    val alarmId: Long?,
    val scheduledAt: Long,
    val firedAt: Long,
    val snoozeCount: Int,
    val failCount: Int,
    val completedAt: Long?,
    val suspicious: Boolean,
    val status: WakeSessionStatus,
    val challengeTypes: Set<String>,
) {
    /** Milisegundos desde que sonó hasta completar; incluye los snoozes. */
    val timeToWake: Long? get() = completedAt?.let { it - firedAt }
}

private val UNRESOLVED_AFTER = Duration.ofHours(24)

/** Agrupa eventos en sesiones, ordenadas de la más antigua a la más reciente. */
fun groupSessions(events: List<WakeEvent>, now: Instant): List<WakeSession> =
    events.groupBy { it.sessionId }.map { (id, list) ->
        val sorted = list.sortedBy { it.timestamp }
        val rang = sorted.firstOrNull { it.type == WakeEventType.RANG }
        val missed = sorted.firstOrNull { it.type == WakeEventType.MISSED }
        val completed = sorted.firstOrNull { it.type == WakeEventType.COMPLETED }
        val first = sorted.first()
        val scheduledAt = sorted.firstNotNullOfOrNull { it.scheduledAt } ?: first.timestamp
        val firedAt = rang?.timestamp ?: if (missed != null) scheduledAt else first.timestamp
        val status = when {
            completed != null -> WakeSessionStatus.COMPLETED
            missed != null -> WakeSessionStatus.NOT_COMPLETED
            now.toEpochMilli() - firedAt >= UNRESOLVED_AFTER.toMillis() -> WakeSessionStatus.NOT_COMPLETED
            else -> WakeSessionStatus.PENDING
        }
        WakeSession(
            sessionId = id,
            alarmId = sorted.firstNotNullOfOrNull { it.alarmId },
            scheduledAt = scheduledAt,
            firedAt = firedAt,
            snoozeCount = sorted.count { it.type == WakeEventType.SNOOZED },
            failCount = sorted.count { it.type == WakeEventType.CHALLENGE_FAILED },
            completedAt = completed?.timestamp,
            suspicious = sorted.any { it.type == WakeEventType.SUSPICIOUS },
            status = status,
            challengeTypes = sorted.mapNotNull { it.challengeType }.toSet(),
        )
    }.sortedBy { it.firedAt }
