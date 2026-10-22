package app.luxion.apexapp.domain

enum class WakeEventType { RANG, SNOOZED, CHALLENGE_FAILED, COMPLETED, MISSED, SUSPICIOUS }

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

/** Escritura del registro de despertar. Es el contrato que usan los demás changes. */
interface WakeEventRecorder {
    suspend fun record(
        sessionId: String,
        type: WakeEventType,
        alarmId: Long? = null,
        timestamp: Long = System.currentTimeMillis(),
        scheduledAt: Long? = null,
        challengeType: String? = null,
        difficulty: Int? = null,
    )
}
