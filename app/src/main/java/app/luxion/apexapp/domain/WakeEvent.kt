package app.luxion.apexapp.domain

/** Evento inmutable de una sesión de despertar. Los instantes son epoch millis. */
data class WakeEvent(
    val sessionId: String,
    val type: WakeEventType,
    val alarmId: Long? = null,
    val timestamp: Long,
    val scheduledAt: Long? = null,
    val challengeType: String? = null,
    val difficulty: Int? = null,
)
