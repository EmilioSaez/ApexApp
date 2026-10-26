package app.luxion.apexapp.alarm.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.PowerManager
import androidx.test.platform.app.InstrumentationRegistry
import app.luxion.apexapp.domain.SoundSource
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AlarmStreamTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private lateinit var player: AndroidTonePlayer

    @Before
    fun setUp() {
        player = AndroidTonePlayer(context)
    }

    @After
    fun tearDown() {
        player.stop()
        shell("input keyevent KEYCODE_WAKEUP")
    }

    @Test
    fun tone_playsOnAlarmStreamWithScreenOff() {
        shell("input keyevent KEYCODE_SLEEP")
        waitFor { !context.getSystemService(PowerManager::class.java).isInteractive }

        val started = player.play(SoundSource.Bundled(BundledSounds.DEFAULT_KEY), 1f) {}
        assertTrue("el reproductor no pudo arrancar", started)

        val audioManager = context.getSystemService(AudioManager::class.java)
        val onAlarmStream = waitFor {
            audioManager.activePlaybackConfigurations.any {
                it.audioAttributes.usage == AudioAttributes.USAGE_ALARM
            }
        }
        assertTrue("no hay reproducción activa con USAGE_ALARM", onAlarmStream)
        assertTrue("la pantalla debía seguir apagada", !context.getSystemService(PowerManager::class.java).isInteractive)
    }

    private fun shell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).close()
    }

    private fun waitFor(timeoutMs: Long = 5_000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(100)
        }
        return condition()
    }
}
