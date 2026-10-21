package app.luxion.apexapp.data

import app.luxion.apexapp.data.local.SoundDao
import app.luxion.apexapp.domain.Sound
import app.luxion.apexapp.domain.SoundRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSoundRepository @Inject constructor(
    private val dao: SoundDao,
) : SoundRepository {
    override fun observeAll(): Flow<List<Sound>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun get(id: Long): Sound? = dao.getById(id)?.toDomain()

    override suspend fun save(sound: Sound): Long {
        if (sound.id == 0L) return dao.insert(sound.toEntity())
        dao.update(sound.toEntity())
        return sound.id
    }

    override suspend fun delete(id: Long) = dao.deleteById(id)
}
