package app.luxion.apexapp.alarm.sound

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.Sound
import app.luxion.apexapp.domain.SoundConfig
import app.luxion.apexapp.domain.SoundRepository
import app.luxion.apexapp.domain.SoundSource
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeTone(var rejects: (SoundSource) -> Boolean = { false }) : TonePlayer {
    val played = mutableListOf<SoundSource>()
    val gains = mutableListOf<Float>()
    var stops = 0
    var failure: (() -> Unit)? = null

    override fun play(source: SoundSource, gain: Float, onFailure: () -> Unit): Boolean {
        if (rejects(source)) return false
        played += source
        gains += gain
        failure = onFailure
        return true
    }

    override fun setGain(gain: Float) {
        gains += gain
    }

    override fun stop() {
        stops++
    }
}

private class FakeVibrator : AlarmVibrator {
    val patterns = mutableListOf<String>()
    var stops = 0
    override fun start(pattern: String) {
        patterns += pattern
    }

    override fun stop() {
        stops++
    }
}

private class FakeSpeaker(var works: Boolean = true) : AlarmSpeaker {
    val spoken = mutableListOf<String>()
    var stops = 0
    override suspend fun speak(text: String): Boolean {
        if (works) spoken += text
        return works
    }

    override fun stop() {
        stops++
    }
}

private class FakeSounds(private val byId: Map<Long, Sound> = emptyMap()) : SoundRepository {
    override fun observeAll(): Flow<List<Sound>> = emptyFlow()
    override suspend fun get(id: Long): Sound? = byId[id]
    override suspend fun save(sound: Sound): Long = error("unused")
    override suspend fun delete(id: Long) = error("unused")
}

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmSoundEngineTest {
    private val clock = Clock.fixed(Instant.parse("2026-01-01T07:05:00Z"), ZoneOffset.UTC)
    private val announcer = object : TimeAnnouncer {
        override fun announce(time: LocalTime) = "Son las $time"
    }

    private fun TestScope.engine(
        tone: TonePlayer,
        vibrator: AlarmVibrator = FakeVibrator(),
        speaker: AlarmSpeaker = FakeSpeaker(),
        sounds: SoundRepository = FakeSounds(),
    ) = AlarmSoundEngine(sounds, tone, vibrator, speaker, announcer, clock, backgroundScope)

    private fun alarm(config: SoundConfig = SoundConfig(), soundId: Long? = null) =
        Alarm(id = 1, hour = 7, minute = 5, soundId = soundId, soundConfig = config)

    @Test
    fun withoutSound_playsDefaultTone() = runTest {
        val tone = FakeTone()
        engine(tone).start(alarm())
        runCurrent()
        assertEquals(listOf<SoundSource>(SoundSource.Bundled(BundledSounds.DEFAULT_KEY)), tone.played)
    }

    @Test
    fun configuredSound_isPlayed() = runTest {
        val tone = FakeTone()
        val source = SoundSource.Uri("content://x/1")
        val sounds = FakeSounds(mapOf(5L to Sound(5, "Song", source)))
        engine(tone, sounds = sounds).start(alarm(soundId = 5))
        runCurrent()
        assertEquals(listOf(source), tone.played)
    }

    @Test
    fun unavailableSource_fallsBackToDefault() = runTest {
        val tone = FakeTone(rejects = { it is SoundSource.Uri })
        val sounds = FakeSounds(mapOf(5L to Sound(5, "Gone", SoundSource.Uri("content://gone"))))
        engine(tone, sounds = sounds).start(alarm(soundId = 5))
        runCurrent()
        assertEquals(listOf<SoundSource>(SoundSource.Bundled(BundledSounds.DEFAULT_KEY)), tone.played)
    }

    @Test
    fun playbackError_restartsWithDefault() = runTest {
        val tone = FakeTone()
        val source = SoundSource.Uri("content://x/1")
        val sounds = FakeSounds(mapOf(5L to Sound(5, "Song", source)))
        engine(tone, sounds = sounds).start(alarm(soundId = 5))
        runCurrent()
        tone.failure!!.invoke()
        assertEquals(SoundSource.Bundled(BundledSounds.DEFAULT_KEY), tone.played.last())
    }

    @Test
    fun ramp_raisesGainMonotonicallyAndKeepsCeiling() = runTest {
        val tone = FakeTone()
        engine(tone).start(alarm(SoundConfig(volume = 80, rampSeconds = 4)))
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(tone.gains.sorted(), tone.gains)
        assertEquals(VolumeRamp.START_GAIN, tone.gains.first(), 0.001f)
        assertEquals(0.8f, tone.gains.last(), 0.001f)
    }

    @Test
    fun noRamp_startsAtCeiling() = runTest {
        val tone = FakeTone()
        engine(tone).start(alarm(SoundConfig(volume = 60, rampSeconds = 0)))
        runCurrent()
        assertEquals(listOf(0.6f), tone.gains)
    }

    @Test
    fun vibration_followsConfig() = runTest {
        val on = FakeVibrator()
        engine(FakeTone(), on).start(alarm(SoundConfig(vibrate = true, vibrationPattern = "sos")))
        runCurrent()
        assertEquals(listOf("sos"), on.patterns)

        val off = FakeVibrator()
        engine(FakeTone(), off).start(alarm(SoundConfig(vibrate = false)))
        runCurrent()
        assertTrue(off.patterns.isEmpty())
    }

    @Test
    fun speech_readsTimeThenReminder() = runTest {
        val speaker = FakeSpeaker()
        val config = SoundConfig(ttsEnabled = true, ttsText = "Hidrátate", speakTime = true)
        engine(FakeTone(), speaker = speaker).start(alarm(config))
        runCurrent()
        assertEquals(listOf("Son las 07:05", "Hidrátate"), speaker.spoken)
    }

    @Test
    fun speech_repeatsWhileRinging() = runTest {
        val speaker = FakeSpeaker()
        val config = SoundConfig(ttsEnabled = true, ttsText = "Arriba", speakRepeatSeconds = 20)
        engine(FakeTone(), speaker = speaker).start(alarm(config))
        runCurrent()
        advanceTimeBy(41_000)
        runCurrent()
        assertEquals(3, speaker.spoken.size)
    }

    @Test
    fun speech_ducksToneWhileTalking() = runTest {
        val tone = FakeTone()
        val config = SoundConfig(volume = 100, rampSeconds = 0, ttsEnabled = true, ttsText = "Arriba")
        engine(tone).start(alarm(config))
        runCurrent()
        assertTrue(tone.gains.any { it < 1f })
        assertEquals(1f, tone.gains.last(), 0.001f)
    }

    @Test
    fun ttsFailure_doesNotAffectSoundOrVibration() = runTest {
        val tone = FakeTone()
        val vibrator = FakeVibrator()
        val config = SoundConfig(ttsEnabled = true, ttsText = "Arriba", speakTime = true, speakRepeatSeconds = 15)
        engine(tone, vibrator, FakeSpeaker(works = false)).start(alarm(config))
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(1, tone.played.size)
        assertEquals(1, vibrator.patterns.size)
    }

    @Test
    fun stop_releasesEverythingAndCancelsSession() = runTest {
        val tone = FakeTone()
        val vibrator = FakeVibrator()
        val speaker = FakeSpeaker()
        val engine = engine(tone, vibrator, speaker)
        engine.start(alarm(SoundConfig(rampSeconds = 10, ttsEnabled = true, ttsText = "Arriba", speakRepeatSeconds = 15)))
        runCurrent()
        assertTrue(engine.playing.value)
        engine.stop()
        val gainsAtStop = tone.gains.size
        val spokenAtStop = speaker.spoken.size
        advanceTimeBy(60_000)
        runCurrent()
        assertFalse(engine.playing.value)
        assertTrue(tone.stops > 0 && vibrator.stops > 0 && speaker.stops > 0)
        assertEquals(gainsAtStop, tone.gains.size)
        assertEquals(spokenAtStop, speaker.spoken.size)
    }

    @Test
    fun mute_silencesToneVibrationAndSpeech_andUnmuteRestoresThem() = runTest {
        val tone = FakeTone()
        val vibrator = FakeVibrator()
        val speaker = FakeSpeaker()
        val engine = engine(tone, vibrator, speaker)
        val config = SoundConfig(volume = 80, rampSeconds = 0, ttsEnabled = true, ttsText = "Arriba", speakRepeatSeconds = 20)
        engine.start(alarm(config))
        runCurrent()
        val vibrationStops = vibrator.stops

        engine.setMuted(true)
        assertEquals(0f, tone.gains.last(), 0.001f)
        assertEquals(vibrationStops + 1, vibrator.stops)
        val spokenWhileMuted = speaker.spoken.size
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(spokenWhileMuted, speaker.spoken.size)
        assertEquals(0f, tone.gains.last(), 0.001f)

        engine.setMuted(false)
        runCurrent()
        assertEquals(0.8f, tone.gains.last(), 0.001f)
        assertEquals(2, vibrator.patterns.size)
        assertTrue(speaker.spoken.size > spokenWhileMuted)
    }

    @Test
    fun mute_keepsRampRunningInSilence() = runTest {
        val tone = FakeTone()
        val engine = engine(tone)
        engine.start(alarm(SoundConfig(volume = 100, rampSeconds = 4)))
        runCurrent()
        engine.setMuted(true)
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(0f, tone.gains.last(), 0.001f)

        engine.setMuted(false)
        assertEquals(1f, tone.gains.last(), 0.001f)
    }

    @Test
    fun restart_startsUnmuted() = runTest {
        val tone = FakeTone()
        val engine = engine(tone)
        engine.start(alarm(SoundConfig(rampSeconds = 0)))
        runCurrent()
        engine.setMuted(true)
        engine.start(alarm(SoundConfig(rampSeconds = 0)))
        runCurrent()
        assertEquals(1f, tone.gains.last(), 0.001f)
    }

    @Test
    fun mute_withoutRingingIsIgnored() = runTest {
        val tone = FakeTone()
        val engine = engine(tone)
        engine.setMuted(true)
        engine.start(alarm(SoundConfig(rampSeconds = 0)))
        runCurrent()
        assertEquals(1f, tone.gains.last(), 0.001f)
    }
}
