package app.luxion.apexapp.domain

sealed interface SoundSource {
    data class Resource(val resId: Int) : SoundSource
    data class Uri(val uri: String) : SoundSource
}

data class Sound(
    val id: Long = 0,
    val name: String,
    val source: SoundSource,
)
