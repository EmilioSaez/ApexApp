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

    private companion object {
        const val DB = "migration-test"
    }
}
