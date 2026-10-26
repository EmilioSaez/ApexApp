package app.luxion.apexapp.ui.sharing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import app.luxion.apexapp.ui.theme.ApexTheme
import com.google.zxing.common.BitMatrix

/**
 * Negro sobre blanco aunque la app esté en modo oscuro: muchos escáneres leen peor un QR invertido,
 * así que aquí el color no sale del tema a propósito.
 */
@Composable
fun QrCode(matrix: BitMatrix, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .background(Color.White, MaterialTheme.shapes.small)
            .padding(ApexTheme.spacing.md)
            .aspectRatio(1f),
    ) {
        val cell = size.width / matrix.width
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell, cell))
            }
        }
    }
}
