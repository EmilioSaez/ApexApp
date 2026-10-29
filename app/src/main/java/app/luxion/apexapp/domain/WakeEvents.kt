package app.luxion.apexapp.domain

enum class WakeEventType { RANG, SNOOZED, CHALLENGE_FAILED, COMPLETED, MISSED, SUSPICIOUS, VERIFIED, VERIFICATION_FAILED }

/** Registro de eventos de despertar; la implementación real la aporta wake-stats. */
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
