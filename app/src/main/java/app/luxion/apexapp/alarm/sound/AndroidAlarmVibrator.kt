package app.luxion.apexapp.alarm.sound

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import app.luxion.apexapp.domain.VibrationPatterns
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidAlarmVibrator @Inject constructor(
    @ApplicationContext private val context: Context,
) : AlarmVibrator {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    @Suppress("DEPRECATION")
    override fun start(pattern: String) {
        val device = vibrator?.takeIf { it.hasVibrator() } ?: return
        val effect = VibrationEffect.createWaveform(VibrationPatterns.waveform(pattern), 0)
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
        device.vibrate(effect, attributes)
    }

    override fun stop() {
        vibrator?.cancel()
    }
}
