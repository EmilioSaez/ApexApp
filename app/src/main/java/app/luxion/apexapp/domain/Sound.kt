package app.luxion.apexapp.domain

sealed interface SoundSource {
    data class Resource(val resId: Int) : SoundSource
    data class Uri(val uri: String) : SoundSource

    /** Tono del sistema (`RingtoneManager`), identificado por su URI. */
    data class SystemTone(val uri: String) : SoundSource

    /** Sonido incluido en la app, identificado por una clave estable y no por `resId`. */
    data class Bundled(val key: String) : SoundSource
}

data class Sound(
    val id: Long = 0,
    val name: String,
    val source: SoundSource,
)
