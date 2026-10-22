package app.luxion.apexapp.alarm.sound

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred

/** TTS por el stream de alarma. Si el motor falla o no soporta el idioma, `speak` devuelve false. */
class AndroidAlarmSpeaker @Inject constructor(
    @ApplicationContext private val context: Context,
) : AlarmSpeaker {
    private var tts: TextToSpeech? = null
    private var ready: CompletableDeferred<Boolean>? = null
    private val pending = mutableMapOf<String, CompletableDeferred<Boolean>>()

    private suspend fun engine(): TextToSpeech? {
        ready?.let { return if (it.await()) tts else null }
        val signal = CompletableDeferred<Boolean>().also { ready = it }
        val created = TextToSpeech(context) { status -> signal.complete(status == TextToSpeech.SUCCESS) }
        tts = created
        if (!signal.await()) {
            release()
            return null
        }
        created.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        val language = created.setLanguage(Locale.getDefault())
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            release()
            return null
        }
        created.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                pending.remove(utteranceId)?.complete(true)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                pending.remove(utteranceId)?.complete(false)
            }
        })
        return created
    }

    override suspend fun speak(text: String): Boolean {
        val engine = engine() ?: return false
        val id = UUID.randomUUID().toString()
        val done = CompletableDeferred<Boolean>()
        pending[id] = done
        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) {
            pending.remove(id)
            return false
        }
        return try {
            done.await()
        } finally {
            pending.remove(id)
        }
    }

    override fun stop() {
        tts?.stop()
        pending.values.forEach { it.complete(false) }
        pending.clear()
        release()
    }

    private fun release() {
        tts?.shutdown()
        tts = null
        ready = null
    }
}
