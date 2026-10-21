package app.luxion.apexapp.data.local

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
)
