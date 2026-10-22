package app.luxion.apexapp.data

import app.luxion.apexapp.data.local.FireStateDao
import app.luxion.apexapp.data.local.FireStateEntity
import app.luxion.apexapp.domain.FireState
import app.luxion.apexapp.domain.FireStateRepository
import javax.inject.Inject

class RoomFireStateRepository @Inject constructor(
    private val dao: FireStateDao,
) : FireStateRepository {
    override suspend fun get(alarmId: Long): FireState? =
        dao.get(alarmId)?.let { FireState(it.alarmId, it.scheduledAt, it.lastFiredAt) }

    override suspend fun markScheduled(alarmId: Long, scheduledAt: Long?) {
        val current = dao.get(alarmId)
        dao.upsert(FireStateEntity(alarmId, scheduledAt, current?.lastFiredAt))
    }

    override suspend fun markFired(alarmId: Long, firedAt: Long) {
        val current = dao.get(alarmId)
        dao.upsert(FireStateEntity(alarmId, current?.scheduledAt, firedAt))
    }

    override suspend fun clear(alarmId: Long) = dao.clear(alarmId)
}
