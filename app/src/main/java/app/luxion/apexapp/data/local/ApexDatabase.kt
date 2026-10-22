package app.luxion.apexapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AlarmEntity::class, SoundEntity::class, FireStateEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class ApexDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun soundDao(): SoundDao
    abstract fun fireStateDao(): FireStateDao
}

/** v1 → v2: campos de extensión de la alarma (retos, sonido y anti-trampa) como JSON. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN challengesJson TEXT NOT NULL DEFAULT '{}'")
        db.execSQL("ALTER TABLE alarms ADD COLUMN soundJson TEXT NOT NULL DEFAULT '{}'")
        db.execSQL("ALTER TABLE alarms ADD COLUMN antiCheatJson TEXT NOT NULL DEFAULT '{}'")
    }
}

/** v2 → v3: estado de programación y disparo por alarma (detección de alarmas perdidas). */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `alarm_fire_state` (" +
                "`alarmId` INTEGER NOT NULL, `scheduledAt` INTEGER, `lastFiredAt` INTEGER, PRIMARY KEY(`alarmId`))",
        )
    }
}
