package app.luxion.apexapp.alarm.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import app.luxion.apexapp.domain.SoundSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** `MediaPlayer` por el stream de alarma, en bucle, con audio focus que no se cede al perderlo. */
class AndroidTonePlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) : TonePlayer {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null

    override fun play(source: SoundSource, gain: Float, onFailure: () -> Unit): Boolean {
        stop()
        requestFocus()
        val created = MediaPlayer()
        return try {
            created.setAudioAttributes(attributes)
            created.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            open(created, source)
            created.isLooping = true
            created.setVolume(gain, gain)
            created.setOnErrorListener { _, _, _ ->
                onFailure()
                true
            }
            created.prepare()
            created.start()
            player = created
            true
        } catch (e: Exception) {
            created.release()
            false
        }
    }

    private fun open(target: MediaPlayer, source: SoundSource) {
        when (source) {
            is SoundSource.Resource -> setRaw(target, source.resId)
            is SoundSource.Bundled -> setRaw(target, BundledSounds.rawRes(source.key))
            is SoundSource.Uri -> target.setDataSource(context, Uri.parse(source.uri))
            is SoundSource.SystemTone -> target.setDataSource(context, Uri.parse(source.uri))
        }
    }

    private fun setRaw(target: MediaPlayer, resId: Int) {
        context.resources.openRawResourceFd(resId).use {
            target.setDataSource(it.fileDescriptor, it.startOffset, it.length)
        }
    }

    override fun setGain(gain: Float) {
        runCatching { player?.setVolume(gain, gain) }
    }

    override fun stop() {
        player?.let {
            runCatching { it.stop() }
            it.release()
        }
        player = null
        focus?.let { audioManager.abandonAudioFocusRequest(it) }
        focus = null
    }

    private fun requestFocus() {
        // La alarma sigue sonando aunque se pierda el focus: no hay listener que la pause.
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(false)
            .build()
        audioManager.requestAudioFocus(request)
        focus = request
    }
}
