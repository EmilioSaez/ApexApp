package app.luxion.apexapp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AdaptiveSettingsDao {
    @Query("SELECT * FROM adaptive_difficulty_settings WHERE alarmId = :alarmId")
    fun observe(alarmId: Long): Flow<AdaptiveSettingsEntity?>

    @Query("SELECT * FROM adaptive_difficulty_settings WHERE alarmId = :alarmId")
    suspend fun get(alarmId: Long): AdaptiveSettingsEntity?

    @Upsert
    suspend fun upsert(entity: AdaptiveSettingsEntity)

    @Query("DELETE FROM adaptive_difficulty_settings WHERE alarmId = :alarmId")
    suspend fun delete(alarmId: Long)
}
