package app.luxion.apexapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AlarmEntity::class, SoundEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ApexDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun soundDao(): SoundDao
}
