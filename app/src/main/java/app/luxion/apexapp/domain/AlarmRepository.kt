package app.luxion.apexapp.domain

import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun observeAll(): Flow<List<Alarm>>
    suspend fun getEnabled(): List<Alarm>
    suspend fun get(id: Long): Alarm?

    /** Inserta si `id == 0`, actualiza en otro caso. Devuelve el identificador. */
    suspend fun save(alarm: Alarm): Long
    suspend fun delete(id: Long)
}
