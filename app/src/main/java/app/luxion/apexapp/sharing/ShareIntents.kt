package app.luxion.apexapp.sharing

import android.content.ClipData
import android.content.Intent
import android.net.Uri

object ShareIntents {
    fun text(text: String, chooserTitle: String): Intent =
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
            chooserTitle,
        )

    /** Entrega [uri] con permiso de lectura temporal para la app elegida y ninguna otra. */
    fun file(uri: Uri, mimeType: String, chooserTitle: String): Intent {
        val send = Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(null, uri)
        return Intent.createChooser(send, chooserTitle)
    }
}
