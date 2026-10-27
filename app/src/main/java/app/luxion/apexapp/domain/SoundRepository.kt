package app.luxion.apexapp.domain

import kotlinx.coroutines.flow.Flow

interface SoundRepository {
    fun observeAll(): Flow<List<Sound>>
    suspend fun get(id: Long): Sound?

    /** Inserta si `id == 0`, actualiza en otro caso. Devuelve el identificador. */
    suspend fun save(sound: Sound): Long
    suspend fun delete(id: Long)
}
