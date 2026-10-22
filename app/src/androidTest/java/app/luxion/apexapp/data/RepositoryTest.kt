package app.luxion.apexapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.luxion.apexapp.data.local.ApexDatabase
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AntiCheatLevel
import app.luxion.apexapp.domain.AntiCheatPolicy
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.SnoozePolicy
import app.luxion.apexapp.domain.SoundConfig
import app.luxion.apexapp.domain.Sound
import app.luxion.apexapp.domain.SoundSource
import java.time.DayOfWeek
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: ApexDatabase
    private lateinit var alarms: RoomAlarmRepository
    private lateinit var sounds: RoomSoundRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ApexDatabase::class.java).build()
        alarms = RoomAlarmRepository(db.alarmDao())
        sounds = RoomSoundRepository(db.soundDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun alarm_roundTripKeepsAllFields() = runBlocking {
        val alarm = Alarm(
            hour = 6,
            minute = 45,
            repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY),
            enabled = false,
            label = "Gym",
            challenges = ChallengeConfig(3, listOf(ChallengeSpec("math", """{"ops":2}"""))),
            soundConfig = SoundConfig(volume = 70, vibrate = false, ttsEnabled = true, ttsText = "Hola"),
            antiCheat = AntiCheatPolicy(AntiCheatLevel.STANDARD, SnoozePolicy(allowed = true)),
        )
        val id = alarms.save(alarm)
        assertEquals(alarm.copy(id = id), alarms.get(id))
    }

    @Test
    fun alarm_saveWithIdUpdates() = runBlocking {
        val id = alarms.save(Alarm(hour = 6, minute = 0))
        alarms.save(Alarm(id = id, hour = 7, minute = 15))
        assertEquals(7, alarms.get(id)!!.hour)
        assertEquals(1, alarms.observeAll().first().size)
    }

    @Test
    fun alarm_getEnabledFilters() = runBlocking {
        alarms.save(Alarm(hour = 6, minute = 0, enabled = true))
        alarms.save(Alarm(hour = 7, minute = 0, enabled = false))
        assertEquals(listOf(6), alarms.getEnabled().map { it.hour })
    }

    @Test
    fun alarm_deleteRemoves() = runBlocking {
        val id = alarms.save(Alarm(hour = 6, minute = 0))
        alarms.delete(id)
        assertNull(alarms.get(id))
    }

    @Test
    fun sound_roundTripBothSources() = runBlocking {
        val res = sounds.save(Sound(name = "Beep", source = SoundSource.Resource(42)))
        val uri = sounds.save(Sound(name = "Song", source = SoundSource.Uri("content://x/1")))
        assertEquals(SoundSource.Resource(42), sounds.get(res)!!.source)
        assertEquals(SoundSource.Uri("content://x/1"), sounds.get(uri)!!.source)
    }

    @Test
    fun deletingSound_keepsAlarmWithoutSound() = runBlocking {
        val soundId = sounds.save(Sound(name = "Beep", source = SoundSource.Resource(1)))
        val alarmId = alarms.save(Alarm(hour = 6, minute = 0, soundId = soundId))
        assertEquals(soundId, alarms.get(alarmId)!!.soundId)
        sounds.delete(soundId)
        assertNull(alarms.get(alarmId)!!.soundId)
        assertTrue(sounds.observeAll().first().isEmpty())
    }

    @Test
    fun sound_roundTripSystemAndBundledSources() = runBlocking {
        val system = sounds.save(Sound(name = "Beep", source = SoundSource.SystemTone("content://settings/system/alarm_alert")))
        val bundled = sounds.save(Sound(name = "default", source = SoundSource.Bundled("default")))
        assertEquals(SoundSource.SystemTone("content://settings/system/alarm_alert"), sounds.get(system)!!.source)
        assertEquals(SoundSource.Bundled("default"), sounds.get(bundled)!!.source)
    }

    @Test
    fun alarm_keepsExtendedSoundConfig() = runBlocking {
        val config = SoundConfig(
            volume = 60, rampSeconds = 45, vibrationPattern = "c:200,100,400",
            speakTime = true, ttsEnabled = true, ttsText = "Arriba", speakRepeatSeconds = 30,
        )
        val id = alarms.save(Alarm(hour = 6, minute = 0, soundConfig = config))
        assertEquals(config, alarms.get(id)!!.soundConfig)
    }

    @Test
    fun alarm_withDefaultSoundConfigHasNeutralDefaults() = runBlocking {
        val id = alarms.save(Alarm(hour = 6, minute = 0))
        val config = alarms.get(id)!!.soundConfig
        assertEquals(30, config.rampSeconds)
        assertEquals("steady", config.vibrationPattern)
        assertEquals(false, config.speakTime)
    }
}
