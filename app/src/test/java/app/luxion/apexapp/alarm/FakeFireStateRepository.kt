package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.FireState
import app.luxion.apexapp.domain.FireStateRepository

class FakeFireStateRepository : FireStateRepository {
    private val states = mutableMapOf<Long, FireState>()

    override suspend fun get(alarmId: Long) = states[alarmId]

    override suspend fun markScheduled(alarmId: Long, scheduledAt: Long?) {
        states[alarmId] = (states[alarmId] ?: FireState(alarmId)).copy(scheduledAt = scheduledAt)
    }

    override suspend fun markFired(alarmId: Long, firedAt: Long) {
        states[alarmId] = (states[alarmId] ?: FireState(alarmId)).copy(lastFiredAt = firedAt)
    }

    override suspend fun clear(alarmId: Long) {
        states.remove(alarmId)
    }
}
