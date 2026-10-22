package app.luxion.apexapp.alarm.sound

import app.luxion.apexapp.domain.SoundSource
import java.time.LocalTime

/** Reproductor de tono. Implementación Android en [AndroidTonePlayer]. */
interface TonePlayer {
    /** Empieza en bucle; devuelve false si la fuente no se puede abrir. [onFailure] avisa de errores posteriores. */
    fun play(source: SoundSource, gain: Float, onFailure: () -> Unit): Boolean
    fun setGain(gain: Float)
    fun stop()
}

interface AlarmVibrator {
    /** Vibra en bucle con el patrón; sin vibrador no hace nada. */
    fun start(pattern: String)
    fun stop()
}

interface AlarmSpeaker {
    /** Lee [text] y vuelve al terminar; false si el motor no está disponible o falla. */
    suspend fun speak(text: String): Boolean
    fun stop()
}

interface TimeAnnouncer {
    fun announce(time: LocalTime): String
}
