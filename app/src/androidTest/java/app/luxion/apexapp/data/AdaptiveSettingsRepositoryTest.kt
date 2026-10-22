package app.luxion.apexapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.luxion.apexapp.data.local.ApexDatabase
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.adaptive.AdaptiveSettings
import app.luxion.apexapp.domain.adaptive.ChangeReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdaptiveSettingsRepositoryTest {
    private lateinit var db: ApexDatabase
    private lateinit var alarms: RoomAlarmRepository
    private lateinit var settings: RoomAdaptiveSettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ApexDatabase::class.java).build()
        alarms = RoomAlarmRepository(db.alarmDao())
        settings = RoomAdaptiveSettingsRepository(db.adaptiveSettingsDao())
    }

    @After
    fun tearDown() = db.close()

    private val full = AdaptiveSettings(
        alarmId = 0,
        enabled = true,
        minLevel = 2,
        maxLevel = 4,
        currentLevel = 3,
        lastChangeReason = ChangeReason.SNOOZES,
        lastChangeHits = 2,
        lastChangeFrom = 2,
        lastChangeAt = 1_700_000_000_000,
        lastChangeSessionId = "s9",
        noticePending = true,
    )

    @Test
    fun savedSettingsAreReadBackWithAllFields() = runBlocking {
        val id = alarms.save(Alarm(hour = 7, minute = 0))
        settings.save(full.copy(alarmId = id))
        assertEquals(full.copy(alarmId = id), settings.get(id))
        assertEquals(full.copy(alarmId = id), settings.observe(id).first())
    }

    @Test
    fun saveTwiceUpdatesTheRow() = runBlocking {
        val id = alarms.save(Alarm(hour = 7, minute = 0))
        settings.save(full.copy(alarmId = id))
        settings.save(full.copy(alarmId = id, currentLevel = 4, noticePending = false))
        assertEquals(4, settings.get(id)?.currentLevel)
    }

    @Test
    fun missingRowReadsAsNull() = runBlocking {
        assertNull(settings.get(99))
    }

    @Test
    fun deletingTheAlarmDeletesItsSettings() = runBlocking {
        val id = alarms.save(Alarm(hour = 7, minute = 0))
        settings.save(full.copy(alarmId = id))
        alarms.delete(id)
        assertNull(settings.get(id))
    }

    @Test
    fun settingsSurviveReopeningTheDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("adaptive-reopen")
        var file = Room.databaseBuilder(context, ApexDatabase::class.java, "adaptive-reopen").build()
        val id = RoomAlarmRepository(file.alarmDao()).save(Alarm(hour = 7, minute = 0))
        RoomAdaptiveSettingsRepository(file.adaptiveSettingsDao()).save(full.copy(alarmId = id))
        file.close()

        file = Room.databaseBuilder(context, ApexDatabase::class.java, "adaptive-reopen").build()
        assertEquals(full.copy(alarmId = id), RoomAdaptiveSettingsRepository(file.adaptiveSettingsDao()).get(id))
        file.close()
        context.deleteDatabase("adaptive-reopen")
    }
}
