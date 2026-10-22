package app.luxion.apexapp.sharing

import app.luxion.apexapp.domain.Alarm
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class QrCodeMatrixTest {
    private fun read(matrix: BitMatrix): String {
        val scale = 6
        val size = matrix.width * scale
        val pixels = IntArray(size * size) { i ->
            if (matrix[i % size / scale, i / size / scale]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(size, size, pixels)))
        return QRCodeReader().decode(bitmap).text
    }

    @Test
    fun aTypicalAlarmFitsAndScansBackToTheSameLink() {
        val alarm = Alarm(hour = 7, minute = 30, label = "Trabajo")
        val link = SharedAlarmCodec.toLink(alarm.toShared(null))
        val matrix = QrCodeMatrix.of(link)
        assertNotNull(matrix)
        assertEquals(link, read(matrix!!))
    }

    @Test
    fun tooMuchContentGivesNoQr() {
        assertNull(QrCodeMatrix.of("x".repeat(QrCodeMatrix.MAX_CHARS + 1)))
    }
}
