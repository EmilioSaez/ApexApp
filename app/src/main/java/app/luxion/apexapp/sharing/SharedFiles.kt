package app.luxion.apexapp.sharing

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Archivos temporales que se entregan a otras apps; solo `cache/shared/` se expone por el FileProvider. */
@Singleton
class SharedFiles @Inject constructor(@ApplicationContext private val context: Context) {
    private val dir get() = File(context.cacheDir, "shared")

    /** Sustituye lo que hubiera en el directorio compartido y devuelve el URI de lectura de [name]. */
    fun write(name: String, bytes: ByteArray): Uri {
        dir.deleteRecursively()
        dir.mkdirs()
        val file = File(dir, name).also { it.writeBytes(bytes) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
