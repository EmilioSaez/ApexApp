package app.luxion.apexapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AlarmEntity::class,
        SoundEntity::class,
        FireStateEntity::class,
        WakeEventEntity::class,
        SleepLogEntity::class,
        SleepPreferencesEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class ApexDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun soundDao(): SoundDao
    abstract fun fireStateDao(): FireStateDao
    abstract fun wakeEventDao(): WakeEventDao
    abstract fun sleepDao(): SleepDao
}

/** v3 → v4: historial de despertar y registros de sueño (solo tablas nuevas). */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `wake_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`sessionId` TEXT NOT NULL, `type` TEXT NOT NULL, `alarmId` INTEGER, `timestamp` INTEGER NOT NULL, " +
                "`scheduledAt` INTEGER, `challengeType` TEXT, `difficulty` INTEGER, " +
                "FOREIGN KEY(`alarmId`) REFERENCES `alarms`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_wake_events_sessionId` ON `wake_events` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_wake_events_timestamp` ON `wake_events` (`timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_wake_events_alarmId` ON `wake_events` (`alarmId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sleep_logs` (`wakeDate` TEXT NOT NULL, `bedtimeAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`wakeDate`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sleep_preferences` (`id` INTEGER NOT NULL, " +
                "`usualBedtimeMinutes` INTEGER, PRIMARY KEY(`id`))",
        )
    }
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
