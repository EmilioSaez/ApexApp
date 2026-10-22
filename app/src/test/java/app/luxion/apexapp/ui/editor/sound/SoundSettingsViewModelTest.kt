package app.luxion.apexapp.ui.editor.sound

import app.luxion.apexapp.alarm.sound.AlarmSoundEngine
import app.luxion.apexapp.alarm.sound.AlarmSpeaker
import app.luxion.apexapp.alarm.sound.AlarmVibrator
import app.luxion.apexapp.alarm.sound.TimeAnnouncer
import app.luxion.apexapp.alarm.sound.TonePlayer
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.Sound
import app.luxion.apexapp.domain.SoundRepository
import app.luxion.apexapp.domain.SoundSource
import java.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class MemorySounds : SoundRepository {
    val all = MutableStateFlow<List<Sound>>(emptyList())
    override fun observeAll(): Flow<List<Sound>> = all
    override suspend fun get(id: Long): Sound? = all.value.firstOrNull { it.id == id }
    override suspend fun save(sound: Sound): Long {
        val id = (all.value.maxOfOrNull { it.id } ?: 0) + 1
        all.value = all.value + sound.copy(id = id)
        return id
    }

    override suspend fun delete(id: Long) {
        all.value = all.value.filterNot { it.id == id }
    }
}

private class FakeImporter : SoundImporter {
    var audioFiles = mapOf("content://song" to "Song.mp3")
    var tones = mapOf("content://tone" to "Beep")
    override fun importFile(uri: String) = audioFiles[uri]
    override fun systemToneName(uri: String) = tones[uri]
}

private class CountingTone : TonePlayer {
    var plays = 0
    var stops = 0
    override fun play(source: SoundSource, gain: Float, onFailure: () -> Unit): Boolean {
        plays++
        return true
    }

    override fun setGain(gain: Float) = Unit
    override fun stop() {
        stops++
    }
}

private object NoVibration : AlarmVibrator {
    override fun start(pattern: String) = Unit
    override fun stop() = Unit
}

private object NoSpeech : AlarmSpeaker {
    override suspend fun speak(text: String) = true
    override fun stop() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class SoundSettingsViewModelTest {
    private val importer = FakeImporter()
    private val sounds = MemorySounds()
    private val tone = CountingTone()

    private fun TestScope.viewModel(): SoundSettingsViewModel {
        val engine = AlarmSoundEngine(
            sounds, tone, NoVibration, NoSpeech,
            object : TimeAnnouncer {
                override fun announce(time: java.time.LocalTime) = time.toString()
            },
            Clock.systemUTC(), backgroundScope,
        )
        return SoundSettingsViewModel(sounds, importer, engine)
    }

    @Test
    fun pickFile_savesOnceAndReusesTheSound() = runTest {
        val vm = viewModel()
        val first = vm.pickFile("content://song")
        val second = vm.pickFile("content://song")
        assertEquals(first, second)
        assertEquals(1, sounds.all.value.size)
        assertEquals(SoundSource.Uri("content://song"), sounds.all.value.single().source)
        assertEquals("Song.mp3", vm.sound(first)?.name)
    }

    @Test
    fun pickFile_rejectsNonAudio() = runTest {
        val vm = viewModel()
        assertNull(vm.pickFile("content://document.pdf"))
        assertTrue(sounds.all.value.isEmpty())
    }

    @Test
    fun pickSystemTone_savesSystemSource() = runTest {
        val vm = viewModel()
        val id = vm.pickSystemTone("content://tone")
        assertEquals(SoundSource.SystemTone("content://tone"), vm.sound(id)?.source)
        assertNull(vm.pickSystemTone("content://unknown"))
    }

    @Test
    fun noSoundSelected_hasNoSound() = runTest {
        assertNull(viewModel().sound(null))
    }

    @Test
    fun preview_startsAndStopsFromTheEditor() = runTest {
        val vm = viewModel()
        val alarm = Alarm(hour = 7, minute = 0)
        vm.togglePreview(alarm)
        runCurrent()
        assertTrue(vm.previewing.value)
        assertEquals(1, tone.plays)
        vm.togglePreview(alarm)
        assertFalse(vm.previewing.value)
    }

    @Test
    fun preview_doesNotInterruptARingingAlarm() = runTest {
        val engine = AlarmSoundEngine(
            sounds, tone, NoVibration, NoSpeech,
            object : TimeAnnouncer {
                override fun announce(time: java.time.LocalTime) = ""
            },
            Clock.systemUTC(), backgroundScope,
        )
        val vm = SoundSettingsViewModel(sounds, importer, engine)
        engine.start(Alarm(hour = 7, minute = 0))
        runCurrent()
        vm.togglePreview(Alarm(hour = 8, minute = 0))
        vm.stopPreview()
        assertTrue(engine.playing.value)
        assertEquals(1, tone.plays)
    }
}
