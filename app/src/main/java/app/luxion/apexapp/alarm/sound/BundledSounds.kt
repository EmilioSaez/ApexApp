package app.luxion.apexapp.alarm.sound

import androidx.annotation.RawRes
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.Sound
import app.luxion.apexapp.domain.SoundSource

/** Sonidos incluidos en la app, identificados por claves estables (aptas para compartir). */
object BundledSounds {
    const val DEFAULT_KEY = "default"

    private val raw: Map<String, Int> = mapOf(DEFAULT_KEY to R.raw.default_alarm)

    val keys: Set<String> get() = raw.keys

    /** Sonido para una clave; si es nula o desconocida, el sonido por defecto. */
    fun resolve(key: String?): Sound {
        val known = if (key != null && key in raw) key else DEFAULT_KEY
        return Sound(name = known, source = SoundSource.Bundled(known))
    }

    @RawRes
    fun rawRes(key: String): Int = raw[key] ?: raw.getValue(DEFAULT_KEY)
}
