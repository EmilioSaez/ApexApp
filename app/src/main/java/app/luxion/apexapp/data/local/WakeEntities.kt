package app.luxion.apexapp.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Registro append-only; el historial sobrevive al borrado de la alarma (`alarmId` pasa a nulo). */
@Entity(
    tableName = "wake_events",
    foreignKeys = [
        ForeignKey(
            entity = AlarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["alarmId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("sessionId"), Index("timestamp"), Index("alarmId")],
)
data class WakeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val type: String,
    val alarmId: Long?,
    val timestamp: Long,
    val scheduledAt: Long?,
    val challengeType: String?,
    val difficulty: Int?,
)

/** Hora de acostarse manual; [wakeDate] es la fecha local del despertar en ISO (`2026-10-21`). */
@Entity(tableName = "sleep_logs")
data class SleepLogEntity(
    @PrimaryKey val wakeDate: String,
    val bedtimeAt: Long,
)

/** Fila única (`id = 1`) con la hora habitual de acostarse, en minutos desde medianoche. */
@Entity(tableName = "sleep_preferences")
data class SleepPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val usualBedtimeMinutes: Int?,
)

@Dao
interface WakeEventDao {
    @Insert
    suspend fun insert(event: WakeEventEntity): Long

    @Query("SELECT COUNT(*) FROM wake_events WHERE sessionId = :sessionId AND type = :type")
    suspend fun count(sessionId: String, type: String): Int

    @Query(
        "SELECT * FROM wake_events WHERE sessionId IN (" +
            "SELECT sessionId FROM wake_events WHERE COALESCE(scheduledAt, timestamp) >= :from " +
            "AND COALESCE(scheduledAt, timestamp) < :to) ORDER BY timestamp, id",
    )
    fun observeInRange(from: Long, to: Long): Flow<List<WakeEventEntity>>

    @Query(
        "SELECT * FROM wake_events WHERE sessionId IN (" +
            "SELECT sessionId FROM wake_events WHERE alarmId = :alarmId " +
            "GROUP BY sessionId ORDER BY MAX(timestamp) DESC LIMIT :sessions) ORDER BY timestamp, id",
    )
    suspend fun recentForAlarm(alarmId: Long, sessions: Int): List<WakeEventEntity>

    @Query("SELECT * FROM wake_events ORDER BY timestamp, id")
    fun observeAll(): Flow<List<WakeEventEntity>>
}

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_logs")
    fun observeLogs(): Flow<List<SleepLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: SleepLogEntity)

    @Query("DELETE FROM sleep_logs WHERE wakeDate = :wakeDate")
    suspend fun deleteLog(wakeDate: String)

    @Query("SELECT * FROM sleep_preferences WHERE id = 1")
    fun observePreferences(): Flow<SleepPreferencesEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreferences(preferences: SleepPreferencesEntity)
}
