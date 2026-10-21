package app.luxion.apexapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundDao {
    @Query("SELECT * FROM sounds ORDER BY name, id")
    fun observeAll(): Flow<List<SoundEntity>>

    @Query("SELECT * FROM sounds WHERE id = :id")
    suspend fun getById(id: Long): SoundEntity?

    @Insert
    suspend fun insert(sound: SoundEntity): Long

    @Update
    suspend fun update(sound: SoundEntity)

    @Delete
    suspend fun delete(sound: SoundEntity)

    @Query("DELETE FROM sounds WHERE id = :id")
    suspend fun deleteById(id: Long)
}
