package app.luxion.apexapp.sharing

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Matriz del QR de un enlace compartido. */
object QrCodeMatrix {
    /** Más texto da un QR demasiado denso para escanearlo con fiabilidad: se ofrece el archivo en su lugar. */
    const val MAX_CHARS = 1000

    fun of(text: String): BitMatrix? {
        if (text.length > MAX_CHARS) return null
        return try {
            QRCodeWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                0,
                0,
                mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1),
            )
        } catch (_: WriterException) {
            null
        }
    }
}
