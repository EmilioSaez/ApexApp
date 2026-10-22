package app.luxion.apexapp.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "alarm_fire_state")
data class FireStateEntity(
    @PrimaryKey val alarmId: Long,
    val scheduledAt: Long?,
    val lastFiredAt: Long?,
)

@Dao
interface FireStateDao {
    @Query("SELECT * FROM alarm_fire_state WHERE alarmId = :alarmId")
    suspend fun get(alarmId: Long): FireStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: FireStateEntity)

    @Query("DELETE FROM alarm_fire_state WHERE alarmId = :alarmId")
    suspend fun clear(alarmId: Long)
}
