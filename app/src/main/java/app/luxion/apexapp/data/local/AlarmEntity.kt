package app.luxion.apexapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alarms",
    foreignKeys = [
        ForeignKey(
            entity = SoundEntity::class,
            parentColumns = ["id"],
            childColumns = ["soundId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("soundId")],
)
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    /** Máscara de bits: bit 0 = lunes ... bit 6 = domingo. */
    val repeatMask: Int,
    val enabled: Boolean,
    val label: String?,
    val soundId: Long?,
    /** JSON de `ChallengeConfig`. */
    @ColumnInfo(defaultValue = "{}") val challengesJson: String = "{}",
    /** JSON de `SoundConfig`. */
    @ColumnInfo(defaultValue = "{}") val soundJson: String = "{}",
    /** JSON de `AntiCheatPolicy`. */
    @ColumnInfo(defaultValue = "{}") val antiCheatJson: String = "{}",
    /** Verificar que el usuario sigue despierto tras apagar la alarma. */
    @ColumnInfo(defaultValue = "0") val awakeVerificationEnabled: Boolean = false,
)
