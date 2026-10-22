package app.luxion.apexapp.ui.editor.sound

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Acceso al sistema para elegir sonidos; separado del ViewModel para poder sustituirlo en tests. */
interface SoundImporter {
    /** Conserva el permiso de lectura del archivo y devuelve su nombre; null si no es audio o no es legible. */
    fun importFile(uri: String): String?

    /** Nombre visible de un tono del sistema, o null si no existe. */
    fun systemToneName(uri: String): String?
}

class AndroidSoundImporter @Inject constructor(
    @ApplicationContext private val context: Context,
) : SoundImporter {
    override fun importFile(uri: String): String? {
        val parsed = Uri.parse(uri)
        val resolver = context.contentResolver
        if (resolver.getType(parsed)?.startsWith("audio/") != true) return null
        return try {
            resolver.takePersistableUriPermission(parsed, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            resolver.openInputStream(parsed)?.close() ?: return null
            resolver.query(parsed, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            } ?: parsed.lastPathSegment
        } catch (e: Exception) {
            null
        }
    }

    override fun systemToneName(uri: String): String? =
        runCatching { RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context) }.getOrNull()
}
