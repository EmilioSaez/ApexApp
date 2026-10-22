package app.luxion.apexapp.alarm.sound

import app.luxion.apexapp.alarm.RingingSession
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.Sound
import app.luxion.apexapp.domain.SoundConfig
import app.luxion.apexapp.domain.SoundRepository
import app.luxion.apexapp.domain.SoundSource
import java.time.Clock
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SoundAlarmRingerTest {
    private class Tone : TonePlayer {
        val played = mutableListOf<SoundSource>()
        var stops = 0
        override fun play(source: SoundSource, gain: Float, onFailure: () -> Unit): Boolean {
            played += source
            return true
        }

        override fun setGain(gain: Float) = Unit
        override fun stop() {
            stops++
        }
    }

    private class Vibration : AlarmVibrator {
        val patterns = mutableListOf<String>()
        var stops = 0
        override fun start(pattern: String) {
            patterns += pattern
        }

        override fun stop() {
            stops++
        }
    }

    private object Silent : AlarmSpeaker {
        override suspend fun speak(text: String) = true
        override fun stop() = Unit
    }

    private object NoSounds : SoundRepository {
        override fun observeAll(): Flow<List<Sound>> = emptyFlow()
        override suspend fun get(id: Long): Sound? = null
        override suspend fun save(sound: Sound): Long = error("unused")
        override suspend fun delete(id: Long) = error("unused")
    }

    @Test
    fun startPlaysTheSessionAlarmAndStopReleasesEverything() = runTest {
        val tone = Tone()
        val vibration = Vibration()
        val engine = AlarmSoundEngine(
            NoSounds, tone, vibration, Silent,
            object : TimeAnnouncer {
                override fun announce(time: LocalTime) = ""
            },
            Clock.systemUTC(), backgroundScope,
        )
        val ringer = SoundAlarmRinger(engine)
        val alarm = Alarm(id = 9, hour = 6, minute = 30, soundConfig = SoundConfig(vibrationPattern = "pulse"))

        ringer.start(RingingSession("s1", alarm))
        runCurrent()
        assertTrue(engine.playing.value)
        assertEquals(1, tone.played.size)
        assertEquals(listOf("pulse"), vibration.patterns)

        ringer.stop()
        assertFalse(engine.playing.value)
        assertTrue(tone.stops > 0 && vibration.stops > 0)
    }

    @Test
    fun restartingForAnotherSessionReplacesThePreviousOne() = runTest {
        val tone = Tone()
        val engine = AlarmSoundEngine(
            NoSounds, tone, Vibration(), Silent,
            object : TimeAnnouncer {
                override fun announce(time: LocalTime) = ""
            },
            Clock.systemUTC(), backgroundScope,
        )
        val ringer = SoundAlarmRinger(engine)
        ringer.start(RingingSession("s1", Alarm(id = 1, hour = 6, minute = 0)))
        runCurrent()
        ringer.start(RingingSession("s2", Alarm(id = 2, hour = 6, minute = 5)))
        runCurrent()
        assertEquals(2, tone.played.size)
        assertTrue("the first session's tone must be stopped", tone.stops >= 2)
        assertTrue(engine.playing.value)
    }
}
