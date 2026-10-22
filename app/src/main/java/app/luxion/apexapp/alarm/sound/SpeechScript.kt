package app.luxion.apexapp.alarm.sound

import app.luxion.apexapp.domain.SoundConfig
import java.time.LocalTime

/** Construye el texto hablado de la alarma: la hora (si se pide) y después el recordatorio. */
object SpeechScript {
    fun build(config: SoundConfig, now: LocalTime, announceTime: (LocalTime) -> String): List<String> {
        if (!config.ttsEnabled && !config.speakTime) return emptyList()
        return buildList {
            if (config.speakTime) add(announceTime(now))
            if (config.ttsEnabled) config.ttsText?.trim()?.takeIf { it.isNotEmpty() }?.let(::add)
        }
    }
}
