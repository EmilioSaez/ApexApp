package app.luxion.apexapp.alarm.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.content.ContentValues
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.Sound
import app.luxion.apexapp.domain.SoundConfig
import app.luxion.apexapp.domain.SoundRepository
import app.luxion.apexapp.domain.SoundSource
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Verificación 7.2 con los componentes reales de Android: la alarma suena aunque falle algo. */
class AlarmFallbackTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sounds = FakeSounds()
    private lateinit var engine: AlarmSoundEngine
    private var otherApp: MediaPlayer? = null
    private var otherFocus: AudioFocusRequest? = null
    private var disabledTts: String? = null

    @Before
    fun setUp() {
        engine = AlarmSoundEngine(
            sounds = sounds,
            tone = AndroidTonePlayer(context),
            vibrator = AndroidAlarmVibrator(context),
            speaker = AndroidAlarmSpeaker(context),
            announcer = AndroidTimeAnnouncer(context),
            clock = Clock.systemDefaultZone(),
            scope = scope,
        )
    }

    @After
    fun tearDown() {
        engine.stop()
        scope.cancel()
        otherApp?.release()
        otherFocus?.let { audioManager.abandonAudioFocusRequest(it) }
        disabledTts?.let { shell("pm enable --user 0 $it") }
    }

    @Test
    fun otherAppAudioInProgress_alarmStillPlaysOnAlarmStream() {
        val media = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        otherFocus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(media).build()
        audioManager.requestAudioFocus(otherFocus!!)
        otherApp = MediaPlayer().apply {
            setAudioAttributes(media)
            context.resources.openRawResourceFd(R.raw.default_alarm).use {
                setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            isLooping = true
            prepare()
            start()
        }
        assertTrue("el audio de la otra app debía estar sonando", usagePlaying(AudioAttributes.USAGE_MEDIA))

        engine.start(Alarm(hour = 6, minute = 0, soundConfig = SoundConfig(rampSeconds = 0, vibrate = false)))

        assertTrue("la alarma debía sonar por USAGE_ALARM", waitFor { usagePlaying(AudioAttributes.USAGE_ALARM) })
        assertTrue("el audio de la otra app no debía haber detenido la alarma", usagePlaying(AudioAttributes.USAGE_ALARM))
    }

    @Test
    fun deletedSafFile_fallsBackToDefaultTone() {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "alarm-fallback-test.mp3")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
        }
        val resolver = context.contentResolver
        val created = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)!!
        resolver.openOutputStream(created)!!.use { it.write(ByteArray(16)) }
        assertTrue(resolver.delete(created, null, null) > 0)
        val uri = created.toString()
        sounds.stored = Sound(id = 1, name = "borrado", source = SoundSource.Uri(uri))

        engine.start(Alarm(hour = 6, minute = 0, soundId = 1, soundConfig = SoundConfig(rampSeconds = 0, vibrate = false)))

        assertTrue("sin el archivo debía sonar el tono por defecto", waitFor { usagePlaying(AudioAttributes.USAGE_ALARM) })
    }

    @Test
    fun missingTtsEngine_alarmStillPlaysTone() {
        val engineName = shell("settings get secure tts_default_synth").trim()
            .takeIf { it.isNotEmpty() && it != "null" } ?: GOOGLE_TTS
        shell("pm disable-user --user 0 $engineName")
        disabledTts = engineName
        assertTrue("el motor TTS debía quedar deshabilitado", shell("pm list packages -d").contains(engineName))
        assertFalse(
            "sin motor TTS, speak debía devolver false",
            runBlocking { AndroidAlarmSpeaker(context).speak("Despierta") },
        )

        engine.start(
            Alarm(
                hour = 6,
                minute = 0,
                soundConfig = SoundConfig(rampSeconds = 0, vibrate = false, ttsEnabled = true, ttsText = "Despierta"),
            ),
        )

        assertTrue("sin TTS debía seguir sonando el tono", waitFor { usagePlaying(AudioAttributes.USAGE_ALARM) })
        Thread.sleep(3_000)
        assertTrue("el fallo del TTS no debía parar el tono", usagePlaying(AudioAttributes.USAGE_ALARM))
    }

    private fun usagePlaying(usage: Int): Boolean =
        audioManager.activePlaybackConfigurations.any { it.audioAttributes.usage == usage }

    private fun shell(command: String): String {
        val pipe = instrumentation.uiAutomation.executeShellCommand(command)
        return java.io.FileInputStream(pipe.fileDescriptor).bufferedReader().readText().also { pipe.close() }
    }

    private fun waitFor(timeoutMs: Long = 5_000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(100)
        }
        return condition()
    }

    private companion object {
        const val GOOGLE_TTS = "com.google.android.tts"
    }

    private class FakeSounds : SoundRepository {
        var stored: Sound? = null
        override fun observeAll(): Flow<List<Sound>> = emptyFlow()
        override suspend fun get(id: Long): Sound? = stored
        override suspend fun save(sound: Sound): Long = sound.id
        override suspend fun delete(id: Long) = Unit
    }
}
