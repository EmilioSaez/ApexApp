package app.luxion.apexapp.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ApexDatabase::class.java,
    )

    @Test
    fun migrate1To2_keepsAlarmsAndAddsDefaults() {
        helper.createDatabase(DB, 1).apply {
            execSQL("INSERT INTO alarms (id, hour, minute, repeatMask, enabled, label, soundId) VALUES (1, 6, 45, 3, 1, 'Gym', NULL)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 2, true, MIGRATION_1_2)
        db.query("SELECT hour, minute, repeatMask, label, challengesJson, soundJson, antiCheatJson FROM alarms WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(6, it.getInt(0))
            assertEquals(45, it.getInt(1))
            assertEquals(3, it.getInt(2))
            assertEquals("Gym", it.getString(3))
            assertEquals("{}", it.getString(4))
            assertEquals("{}", it.getString(5))
            assertEquals("{}", it.getString(6))
        }
    }

    @Test
    fun migrate2To3_keepsAlarmsAndCreatesFireStateTable() {
        helper.createDatabase(DB, 2).apply {
            execSQL(
                "INSERT INTO alarms (id, hour, minute, repeatMask, enabled, label, soundId, challengesJson, soundJson, antiCheatJson) " +
                    "VALUES (1, 6, 45, 0, 1, 'Gym', NULL, '{}', '{}', '{}')",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 3, true, MIGRATION_2_3)
        db.query("SELECT hour, minute, label FROM alarms WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(6, it.getInt(0))
            assertEquals(45, it.getInt(1))
            assertEquals("Gym", it.getString(2))
        }
        db.execSQL("INSERT INTO alarm_fire_state (alarmId, scheduledAt, lastFiredAt) VALUES (1, 1000, NULL)")
        db.query("SELECT scheduledAt, lastFiredAt FROM alarm_fire_state WHERE alarmId = 1").use {
            it.moveToFirst()
            assertEquals(1000L, it.getLong(0))
            assertEquals(true, it.isNull(1))
        }
    }

    @Test
    fun migrate3To4_keepsAlarmsAndAddsEmptyWakeTables() {
        helper.createDatabase(DB, 3).apply {
            execSQL("INSERT INTO alarms (id, hour, minute, repeatMask, enabled, label, soundId, challengesJson, soundJson, antiCheatJson) VALUES (1, 6, 45, 3, 1, 'Gym', NULL, '{}', '{}', '{}')")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 4, true, MIGRATION_3_4)
        db.query("SELECT hour, minute, label FROM alarms WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(6, it.getInt(0))
            assertEquals(45, it.getInt(1))
            assertEquals("Gym", it.getString(2))
        }
        listOf("wake_events", "sleep_logs", "sleep_preferences").forEach { table ->
            db.query("SELECT COUNT(*) FROM $table").use {
                it.moveToFirst()
                assertEquals(0, it.getInt(0))
            }
        }
    }

    private companion object {
        const val DB = "migration-test"
    }
}
