package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AlarmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAlarmRepository : AlarmRepository {
    private val alarms = MutableStateFlow<Map<Long, Alarm>>(emptyMap())
    private var nextId = 1L

    override fun observeAll(): Flow<List<Alarm>> = alarms.map { it.values.toList() }

    override suspend fun getEnabled() = alarms.value.values.filter { it.enabled }

    override suspend fun get(id: Long) = alarms.value[id]

    override suspend fun save(alarm: Alarm): Long {
        val id = if (alarm.id == 0L) nextId++ else alarm.id
        alarms.value = alarms.value + (id to alarm.copy(id = id))
        return id
    }

    override suspend fun delete(id: Long) {
        alarms.value = alarms.value - id
    }
}
