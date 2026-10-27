package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.WakeEvent
import app.luxion.apexapp.domain.WakeEventRecorder
import app.luxion.apexapp.domain.WakeEventType

class FakeWakeEventRecorder(var failWith: Throwable? = null) : WakeEventRecorder {
    val events = mutableListOf<WakeEvent>()

    override suspend fun record(
        sessionId: String,
        type: WakeEventType,
        alarmId: Long?,
        timestamp: Long,
        scheduledAt: Long?,
        challengeType: String?,
        difficulty: Int?,
    ) {
        failWith?.let { throw it }
        events += WakeEvent(sessionId, type, alarmId, timestamp, scheduledAt, challengeType, difficulty)
    }
}
