package app.luxion.apexapp.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Una barra; [value] `null` = sin datos (no se dibuja) y [color] `null` = color principal. */
data class Bar(val value: Float?, val color: Color? = null)

/**
 * Gráfica de barras mínima con Canvas. [description] es el resumen que lee TalkBack, ya que
 * el dibujo en sí no es accesible.
 */
@Composable
fun BarChart(
    title: String,
    bars: List<Bar>,
    description: String,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Column(modifier.fillMaxWidth().semantics { contentDescription = "$title. $description" }) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val max = bars.mapNotNull { it.value }.maxOrNull()?.takeIf { it > 0f } ?: 1f
            val slot = size.width / bars.size.coerceAtLeast(1)
            val barWidth = (slot * 0.62f).coerceAtLeast(1f)
            val radius = CornerRadius(barWidth / 4f)
            bars.forEachIndexed { i, bar ->
                val x = i * slot + (slot - barWidth) / 2f
                drawRoundRect(trackColor, Offset(x, size.height - 2f), Size(barWidth, 2f), radius)
                bar.value?.let {
                    val h = (it / max * size.height).coerceAtLeast(2f)
                    drawRoundRect(bar.color ?: barColor, Offset(x, size.height - h), Size(barWidth, h), radius)
                }
            }
        }
    }
}
