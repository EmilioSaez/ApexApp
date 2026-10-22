package app.luxion.apexapp.sharing

import android.content.ContentResolver
import android.content.Intent
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Contenido recibido por un intent, a la espera de que la pantalla de importación lo muestre. */
sealed interface ImportInput {
    data class Link(val link: String) : ImportInput
    class File(val bytes: ByteArray) : ImportInput
}

@Singleton
class PendingImport @Inject constructor() {
    private val _content = MutableStateFlow<ImportInput?>(null)
    val content: StateFlow<ImportInput?> = _content

    fun offer(input: ImportInput) {
        _content.value = input
    }

    fun clear() {
        _content.value = null
    }
}

object ImportIntents {
    /** Enlace de la app o archivo `.apexalarm` de un `ACTION_VIEW`; null si el intent no trae contenido nuestro. */
    fun inputOf(intent: Intent?, resolver: ContentResolver): ImportInput? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        return when (uri.scheme) {
            "apexapp" -> ImportInput.Link(uri.toString())
            "content" -> readBounded(resolver, uri)?.let { ImportInput.File(it) }
            else -> null
        }
    }

    /** Lee como mucho el tope del formato más un byte, para que un archivo enorme se rechace sin cargarlo entero. */
    fun readBounded(resolver: ContentResolver, uri: android.net.Uri): ByteArray? = runCatching {
        resolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var total = 0
            while (total <= SharedAlarmCodec.MAX_INPUT_BYTES) {
                val read = input.read(buffer)
                if (read < 0) break
                out.write(buffer, 0, read)
                total += read
            }
            out.toByteArray()
        }
    }.getOrNull()
}
