package app.luxion.apexapp.alarm.sound

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.SoundRepository
import app.luxion.apexapp.domain.SoundSource
import java.time.Clock
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Orquesta sonido, rampa de volumen, vibración y voz de una alarma que suena. No es un servicio:
 * lo arranca quien aloja la alarma sonando (`AlarmRinger`) o la previsualización del editor.
 */
class AlarmSoundEngine(
    private val sounds: SoundRepository,
    private val tone: TonePlayer,
    private val vibrator: AlarmVibrator,
    private val speaker: AlarmSpeaker,
    private val announcer: TimeAnnouncer,
    private val clock: Clock,
    private val scope: CoroutineScope,
) {
    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing

    private var session: Job? = null
    private var rampGain = 1f
    private var ducked = false
    private var muted = false
    private var alarm: Alarm? = null
    private var speech: Job? = null

    fun start(alarm: Alarm) {
        stop()
        this.alarm = alarm
        _playing.value = true
        session = scope.launch {
            val config = alarm.soundConfig
            val ceiling = config.volume / 100f
            rampGain = VolumeRamp.gainAt(0, config.rampSeconds, ceiling)
            ducked = false
            startTone(resolveSource(alarm), rampGain)
            if (config.vibrate) vibrator.start(config.vibrationPattern)
            if (config.rampSeconds > 0) launch { runRamp(config.rampSeconds, ceiling) }
            if (SpeechScript.build(config, LocalTime.now(clock), announcer::announce).isNotEmpty()) {
                speech = launch { runSpeech(alarm) }
            }
        }
    }

    /**
     * Silencia o reactiva sonido, vibración y voz sin tocar el volumen del stream de alarma (el bloqueo
     * de volumen sigue intacto). El tono sigue en bucle con ganancia 0 para reactivarse al instante.
     */
    fun setMuted(muted: Boolean) {
        val active = alarm ?: return
        if (this.muted == muted) return
        this.muted = muted
        tone.setGain(currentGain())
        val config = active.soundConfig
        if (muted) {
            vibrator.stop()
            speech?.cancel()
            speaker.stop()
        } else {
            if (config.vibrate) vibrator.start(config.vibrationPattern)
            if (SpeechScript.build(config, LocalTime.now(clock), announcer::announce).isNotEmpty()) {
                speech = scope.launch { runSpeech(active) }
            }
        }
    }

    fun stop() {
        session?.cancel()
        session = null
        speech?.cancel()
        speech = null
        alarm = null
        muted = false
        tone.stop()
        vibrator.stop()
        speaker.stop()
        _playing.value = false
    }

    private suspend fun resolveSource(alarm: Alarm): SoundSource =
        alarm.soundId?.let { sounds.get(it) }?.source ?: defaultSource()

    private fun defaultSource(): SoundSource = SoundSource.Bundled(BundledSounds.DEFAULT_KEY)

    /** Una alarma nunca debe sonar en silencio: si la fuente falla, suena el tono por defecto. */
    private fun startTone(source: SoundSource, gain: Float) {
        val fallback = { tone.play(defaultSource(), currentGain(), onFailure = {}) }
        val opened = tone.play(source, gain) { fallback() }
        if (!opened) fallback()
    }

    private suspend fun runRamp(rampSeconds: Int, ceiling: Float) {
        var elapsed = 0L
        while (elapsed < rampSeconds * 1000L) {
            delay(RAMP_STEP_MS)
            elapsed += RAMP_STEP_MS
            rampGain = VolumeRamp.gainAt(elapsed, rampSeconds, ceiling)
            tone.setGain(currentGain())
        }
    }

    private suspend fun runSpeech(alarm: Alarm) {
        val config = alarm.soundConfig
        while (true) {
            val lines = SpeechScript.build(config, LocalTime.now(clock), announcer::announce)
            ducked = true
            tone.setGain(currentGain())
            try {
                for (line in lines) if (!speaker.speak(line)) return
            } finally {
                ducked = false
                tone.setGain(currentGain())
            }
            if (config.speakRepeatSeconds == 0) return
            delay(config.speakRepeatSeconds * 1000L)
        }
    }

    private fun currentGain(): Float = when {
        muted -> 0f
        ducked -> rampGain * DUCK_FACTOR
        else -> rampGain
    }

    companion object {
        const val RAMP_STEP_MS = 250L
        const val DUCK_FACTOR = 0.3f
    }
}
