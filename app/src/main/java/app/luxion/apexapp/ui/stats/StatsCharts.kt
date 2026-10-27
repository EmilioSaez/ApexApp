package app.luxion.apexapp.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.luxion.apexapp.ui.theme.ApexTheme

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
    /** Rótulo bajo cada barra (p. ej. el día de la semana); vacío = sin rótulos. */
    labels: List<String> = emptyList(),
) {
    Surface(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        BarChartBody(title, bars, description, barColor, trackColor, labels)
    }
}

@Composable
private fun BarChartBody(
    title: String,
    bars: List<Bar>,
    description: String,
    barColor: Color,
    trackColor: Color,
    labels: List<String>,
) {
    Column(
        Modifier.fillMaxWidth().padding(ApexTheme.spacing.lg).semantics { contentDescription = "$title. $description" },
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        // El dato resumen va en texto visible, no solo para TalkBack.
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Canvas(Modifier.fillMaxWidth().padding(top = ApexTheme.spacing.sm).height(ApexTheme.spacing.xxxl * 2)) {
            val max = bars.mapNotNull { it.value }.maxOrNull()?.takeIf { it > 0f } ?: 1f
            val slot = size.width / bars.size.coerceAtLeast(1)
            // Pocas barras: finas y con aire; muchas (30/90 días): casi pegadas.
            val barWidth = (slot * if (bars.size <= WeekBars) 0.3f else 0.62f).coerceAtLeast(1f)
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
        if (labels.size == bars.size && labels.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(top = ApexTheme.spacing.xs)) {
                labels.forEach {
                    Text(
                        it,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private const val WeekBars = 7
