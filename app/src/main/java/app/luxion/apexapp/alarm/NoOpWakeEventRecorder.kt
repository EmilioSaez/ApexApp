package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.WakeEventRecorder
import app.luxion.apexapp.domain.WakeEventType
import javax.inject.Inject

/** Sustituto hasta que wake-stats aporte la implementación real. */
class NoOpWakeEventRecorder @Inject constructor() : WakeEventRecorder {
    override suspend fun record(
        sessionId: String,
        type: WakeEventType,
        alarmId: Long?,
        timestamp: Long,
        scheduledAt: Long?,
        challengeType: String?,
        difficulty: Int?,
    ) = Unit
}
