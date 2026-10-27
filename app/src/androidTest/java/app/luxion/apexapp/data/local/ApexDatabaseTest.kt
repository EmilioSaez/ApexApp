package app.luxion.apexapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApexDatabaseTest {
    private lateinit var db: ApexDatabase
    private lateinit var alarms: AlarmDao
    private lateinit var sounds: SoundDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ApexDatabase::class.java).build()
        alarms = db.alarmDao()
        sounds = db.soundDao()
    }

    @After
    fun tearDown() = db.close()

    private fun alarm(hour: Int = 7, soundId: Long? = null) =
        AlarmEntity(hour = hour, minute = 30, repeatMask = 0, enabled = true, label = null, soundId = soundId)

    @Test
    fun insert_assignsUniqueIds() = runBlocking {
        val a = alarms.insert(alarm())
        val b = alarms.insert(alarm())
        assertNotEquals(a, b)
        assertEquals(2, alarms.observeAll().first().size)
    }

    @Test
    fun update_changesTime() = runBlocking {
        val id = alarms.insert(alarm(hour = 7))
        alarms.update(alarms.getById(id)!!.copy(hour = 9))
        assertEquals(9, alarms.getById(id)!!.hour)
    }

    @Test
    fun delete_removesAlarm() = runBlocking {
        val id = alarms.insert(alarm())
        alarms.deleteById(id)
        assertNull(alarms.getById(id))
        assertTrue(alarms.observeAll().first().isEmpty())
    }

    @Test
    fun observeAll_emitsAgainOnChange() = runBlocking {
        val flow = alarms.observeAll()
        assertTrue(flow.first().isEmpty())
        alarms.insert(alarm())
        assertEquals(1, flow.first().size)
    }

    @Test
    fun alarm_referencesSound() = runBlocking {
        val soundId = sounds.insert(SoundEntity(name = "Beep", sourceType = "resource", sourceValue = "1"))
        val id = alarms.insert(alarm(soundId = soundId))
        assertEquals(soundId, alarms.getById(id)!!.soundId)
    }

    @Test
    fun deletingSound_keepsAlarmWithoutSound() = runBlocking {
        val sound = SoundEntity(name = "Beep", sourceType = "resource", sourceValue = "1")
        val soundId = sounds.insert(sound)
        val id = alarms.insert(alarm(soundId = soundId))
        sounds.delete(sound.copy(id = soundId))
        val stored = alarms.getById(id)
        assertEquals(id, stored!!.id)
        assertNull(stored.soundId)
    }
}
