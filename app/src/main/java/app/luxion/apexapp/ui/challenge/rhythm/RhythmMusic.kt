package app.luxion.apexapp.ui.challenge.rhythm

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import app.luxion.apexapp.domain.challenge.rhythm.RhythmTuning
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Música de fondo del reto de ritmo, sintetizada en código (sin recursos de audio ni licencias):
 * un compás de 4 pulsos a [RhythmTuning.BPM] con bombo en cada pulso, hi-hat a contratiempo y bajo en los
 * pulsos 1 y 3, que es donde caen las notas. Se repite en bucle.
 */
class RhythmMusic {
    private val track: AudioTrack? = runCatching { buildTrack() }.getOrNull()

    /** Arranca el compás desde el principio; llamar justo al empezar la ronda para que el pulso coincida con las notas. */
    fun restart() {
        track?.runCatching {
            pause()
            flush()
            setPlaybackHeadPosition(0)
            play()
        }
    }

    fun release() {
        track?.runCatching {
            stop()
            release()
        }
    }

    private fun buildTrack(): AudioTrack {
        val pcm = renderBar()
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        track.setLoopPoints(0, pcm.size, -1)
        return track
    }

    private fun renderBar(): ShortArray {
        val beatSamples = (SAMPLE_RATE * RhythmTuning.BEAT_MS / 1000).toInt()
        val mix = FloatArray(beatSamples * BEATS_PER_BAR)
        val noise = Random(1)
        for (beat in 0 until BEATS_PER_BAR) {
            val start = beat * beatSamples
            // Bombo: seno que baja de 120 a 50 Hz con caída rápida.
            for (i in 0 until beatSamples / 2) {
                val t = i / SAMPLE_RATE.toFloat()
                val freq = 50f + 70f * exp(-t * 30f)
                mix[start + i] += 0.8f * exp(-t * 12f) * sin(2 * PI * freq * t).toFloat()
            }
            // Hi-hat: ráfaga de ruido corta a mitad de pulso.
            val hat = start + beatSamples / 2
            for (i in 0 until beatSamples / 8) {
                val t = i / SAMPLE_RATE.toFloat()
                mix[hat + i] += 0.25f * exp(-t * 80f) * (noise.nextFloat() * 2 - 1)
            }
            // Bajo en los pulsos 1 y 3: La grave y Mi.
            if (beat % 2 == 0) {
                val freq = if (beat == 0) 110f else 82.4f
                for (i in 0 until beatSamples * 2) {
                    val index = start + i
                    if (index >= mix.size) break
                    val t = i / SAMPLE_RATE.toFloat()
                    mix[index] += 0.3f * exp(-t * 2.5f) * sin(2 * PI * freq * t).toFloat()
                }
            }
        }
        return ShortArray(mix.size) { (mix[it].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
        const val BEATS_PER_BAR = 4
    }
}
