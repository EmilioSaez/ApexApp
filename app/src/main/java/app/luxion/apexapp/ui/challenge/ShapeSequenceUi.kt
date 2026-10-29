package app.luxion.apexapp.ui.challenge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.shapes.ShapeKind
import app.luxion.apexapp.domain.challenge.shapes.ShapeSequenceChallenge
import app.luxion.apexapp.domain.challenge.shapes.ShapeSequenceInstance
import app.luxion.apexapp.domain.challenge.shapes.ShapeSequenceLevels
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject
import kotlinx.coroutines.delay

private const val StartDelayMs = 600L
private const val GapMs = 250L
private const val PaletteColumns = 3

class ShapeSequenceRenderer @Inject constructor() : ChallengeRenderer {
    override val type: ChallengeType = ShapeSequenceChallenge.TYPE

    @Composable
    override fun label(): String = stringResource(R.string.challenge_type_shapes)

    @Composable
    override fun Render(
        instance: ChallengeInstance,
        wrong: Boolean,
        onAnswer: (ChallengeAnswer) -> Unit,
        onUnavailable: (reason: String) -> Unit,
    ) {
        val shapes = instance as ShapeSequenceInstance
        // La sesión conserva la instancia tras un fallo, así que el reintento lo cuenta el propio renderer.
        var attempt by remember(shapes) { mutableIntStateOf(0) }
        var taps by remember(shapes, attempt) { mutableStateOf(emptyList<Int>()) }
        var shown by remember(shapes, attempt) { mutableStateOf<ShapeKind?>(null) }
        var shownIndex by remember(shapes, attempt) { mutableIntStateOf(0) }
        var playing by remember(shapes, attempt) { mutableStateOf(true) }
        val total = shapes.sequence.size

        LaunchedEffect(shapes, attempt) {
            playing = true
            delay(StartDelayMs)
            shapes.sequence.forEachIndexed { index, kind ->
                shownIndex = index
                shown = kind
                delay(shapes.shapeMs)
                shown = null
                delay(GapMs)
            }
            playing = false
        }

        val onPick = { kind: ShapeKind ->
            if (!playing) {
                val next = taps + kind.ordinal
                taps = next
                val mismatch = kind != shapes.sequence[next.lastIndex]
                if (mismatch || next.size == total) {
                    onAnswer(ChallengeAnswer.Pattern(shapes.type, next))
                    if (mismatch) attempt++
                }
            }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (playing) R.string.challenge_shapes_watch else R.string.challenge_shapes_repeat),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (playing) {
                        stringResource(R.string.challenge_shapes_progress_watch, shownIndex + 1, total)
                    } else {
                        stringResource(R.string.challenge_shapes_progress_repeat, taps.size, total)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (wrong && !playing) {
                Text(
                    stringResource(R.string.challenge_shapes_wrong),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (playing) {
                ShapeStage(shown)
                StepDots(total = total, current = shownIndex)
            } else {
                AnswerSlots(shapes.sequence.size, taps)
                TextButton(
                    onClick = { taps = taps.dropLast(1) },
                    enabled = taps.isNotEmpty(),
                    modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.challenge_shapes_delete_last)) }
                Palette(shapes.palette, onPick)
            }
        }
    }
}

/** Escenario central de 240 dp: muestra la forma actual y queda vacío entre formas. */
@Composable
private fun ShapeStage(shown: ShapeKind?) {
    val description = shown?.let { stringResource(R.string.challenge_shapes_showing, shapeName(it)) }.orEmpty()
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.size(240.dp).semantics { contentDescription = description },
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (shown != null) ShapeGlyph(shown, MaterialTheme.colorScheme.onSurface, Modifier.size(128.dp))
            }
        }
    }
}

/** Un punto por forma: las ya mostradas en claro, la actual en acento y las pendientes apagadas. */
@Composable
private fun StepDots(total: Int, current: Int) {
    val past = MaterialTheme.colorScheme.onSurface
    val now = MaterialTheme.colorScheme.primary
    val pending = MaterialTheme.colorScheme.surfaceContainerHighest
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm, Alignment.CenterHorizontally),
    ) {
        repeat(total) { index ->
            Canvas(Modifier.size(8.dp)) {
                drawCircle(
                    when {
                        index < current -> past
                        index == current -> now
                        else -> pending
                    },
                )
            }
        }
    }
}

/** Una casilla por forma de la secuencia: las elegidas con su silueta y la siguiente resaltada con el acento. */
@Composable
private fun AnswerSlots(total: Int, taps: List<Int>) {
    val description = stringResource(R.string.challenge_shapes_your_sequence)
    val nextDescription = stringResource(R.string.challenge_shapes_next_slot)
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm),
    ) {
        repeat(total) { index ->
            val kind = taps.getOrNull(index)?.let { ShapeKind.entries[it] }
            val isNext = index == taps.size
            Surface(
                modifier = Modifier.weight(1f).height(56.dp).semantics {
                    if (kind == null && isNext) contentDescription = nextDescription
                },
                shape = MaterialTheme.shapes.small,
                color = if (kind != null) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent,
                border = when {
                    isNext -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                },
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (kind != null) {
                        ShapeGlyph(kind, MaterialTheme.colorScheme.onSurface, Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

/** Paleta de formas en filas de tres, con el nombre bajo cada una. */
@Composable
private fun Palette(palette: List<ShapeKind>, onPick: (ShapeKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
        palette.chunked(PaletteColumns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
                row.forEach { kind ->
                    PaletteButton(kind, { onPick(kind) }, Modifier.weight(1f))
                }
                repeat(PaletteColumns - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PaletteButton(kind: ShapeKind, onClick: () -> Unit, modifier: Modifier) {
    val name = shapeName(kind)
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 88.dp).semantics { contentDescription = name },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            ShapeGlyph(kind, MaterialTheme.colorScheme.onSurface, Modifier.size(36.dp))
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun shapeName(kind: ShapeKind): String = stringResource(
    when (kind) {
        ShapeKind.CIRCLE -> R.string.challenge_shape_circle
        ShapeKind.SQUARE -> R.string.challenge_shape_square
        ShapeKind.TRIANGLE -> R.string.challenge_shape_triangle
        ShapeKind.DIAMOND -> R.string.challenge_shape_diamond
        ShapeKind.CROSS -> R.string.challenge_shape_cross
        ShapeKind.HEXAGON -> R.string.challenge_shape_hexagon
    },
)

/** Silueta rellena de una forma, ocupando el cuadrado del modificador. */
@Composable
private fun ShapeGlyph(kind: ShapeKind, color: Color, modifier: Modifier) {
    Canvas(modifier) { drawShape(kind, color) }
}

/** Siluetas sobre la misma caja de 48 × 48 del diseño, escaladas al tamaño del lienzo. */
private fun DrawScope.drawShape(kind: ShapeKind, color: Color) {
    val k = size.minDimension / 48f
    fun p(x: Float, y: Float) = Offset(x * k, y * k)
    when (kind) {
        ShapeKind.CIRCLE -> drawCircle(color, radius = 18f * k)
        ShapeKind.SQUARE -> drawPath(polygon(listOf(p(7f, 7f), p(41f, 7f), p(41f, 41f), p(7f, 41f))), color)
        ShapeKind.TRIANGLE -> drawPath(polygon(listOf(p(24f, 5f), p(44f, 41f), p(4f, 41f))), color)
        ShapeKind.DIAMOND -> drawPath(polygon(listOf(p(24f, 3f), p(45f, 24f), p(24f, 45f), p(3f, 24f))), color)
        ShapeKind.CROSS -> drawPath(
            polygon(
                listOf(
                    p(18f, 5f), p(30f, 5f), p(30f, 18f), p(43f, 18f), p(43f, 30f), p(30f, 30f),
                    p(30f, 43f), p(18f, 43f), p(18f, 30f), p(5f, 30f), p(5f, 18f), p(18f, 18f),
                ),
            ),
            color,
        )
        ShapeKind.HEXAGON -> drawPath(
            polygon(listOf(p(24f, 4f), p(41.3f, 14f), p(41.3f, 34f), p(24f, 44f), p(6.7f, 34f), p(6.7f, 14f))),
            color,
        )
    }
}

private fun polygon(points: List<Offset>): Path = Path().apply {
    moveTo(points.first().x, points.first().y)
    points.drop(1).forEach { lineTo(it.x, it.y) }
    close()
}

/** Editor sin parámetros: longitud, formas y ritmo salen de la dificultad, aquí solo se muestran. */
class ShapeSequenceParamsEditor @Inject constructor() : ChallengeParamsEditor {
    override val type: ChallengeType = ShapeSequenceChallenge.TYPE

    @Composable
    override fun Content(params: String, difficulty: Int, onChange: (String) -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
            Text(stringResource(R.string.challenge_type_shapes), style = MaterialTheme.typography.titleSmall)
            val length = ShapeSequenceLevels.lengthFor(difficulty)
            val distinct = ShapeSequenceLevels.distinctFor(difficulty)
            Text(
                pluralStringResource(R.plurals.challenge_target_info_shapes, length, length, distinct),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
