package app.luxion.apexapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "adaptive_difficulty_settings",
    foreignKeys = [
        ForeignKey(
            entity = AlarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["alarmId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AdaptiveSettingsEntity(
    @PrimaryKey val alarmId: Long,
    val enabled: Boolean,
    val minLevel: Int,
    val maxLevel: Int,
    val currentLevel: Int,
    /** Nombre de `ChangeReason`; texto para no acoplar el esquema al orden del enum. */
    val lastChangeReason: String?,
    val lastChangeHits: Int,
    val lastChangeFrom: Int?,
    val lastChangeAt: Long?,
    val lastChangeSessionId: String?,
    val noticePending: Boolean,
)
