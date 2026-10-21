package app.luxion.apexapp.data

import app.luxion.apexapp.data.local.AlarmDao
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AlarmRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAlarmRepository @Inject constructor(
    private val dao: AlarmDao,
) : AlarmRepository {
    override fun observeAll(): Flow<List<Alarm>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getEnabled(): List<Alarm> = dao.getEnabled().map { it.toDomain() }

    override suspend fun get(id: Long): Alarm? = dao.getById(id)?.toDomain()

    override suspend fun save(alarm: Alarm): Long {
        if (alarm.id == 0L) return dao.insert(alarm.toEntity())
        dao.update(alarm.toEntity())
        return alarm.id
    }

    override suspend fun delete(id: Long) = dao.deleteById(id)
}
