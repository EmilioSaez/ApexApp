package app.luxion.apexapp.ui.challenge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

private const val StartDelayMs = 600L
private const val GapMs = 250L

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

        LaunchedEffect(shapes, attempt) {
            playing = true
            delay(StartDelayMs)
            shapes.sequence.forEachIndexed { index, kind ->
                shownIndex = index + 1
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
                if (mismatch || next.size == shapes.sequence.size) {
                    onAnswer(ChallengeAnswer.Pattern(shapes.type, next))
                    if (mismatch) attempt++
                }
            }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg)) {
            Text(
                stringResource(if (playing) R.string.challenge_shapes_watch else R.string.challenge_shapes_repeat),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            if (wrong && !playing) {
                Text(
                    stringResource(R.string.challenge_shapes_wrong),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            ShapeStage(shown = shown, playing = playing)
            ProgressMarks(
                total = shapes.sequence.size,
                done = if (playing) shownIndex else taps.size,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
                shapes.palette.forEach { kind ->
                    ShapeChoice(
                        kind = kind,
                        enabled = !playing,
                        onClick = { onPick(kind) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Escenario central: muestra la forma actual durante la fase de muestra y queda vacío entre formas. */
@Composable
private fun ShapeStage(shown: ShapeKind?, playing: Boolean) {
    val description = shown?.let { stringResource(R.string.challenge_shapes_showing, shapeName(it)) }
        ?: stringResource(if (playing) R.string.challenge_shapes_stage_wait else R.string.challenge_shapes_stage_ready)
    Surface(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.4f).semantics { contentDescription = description },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (shown != null) {
                ShapeGlyph(shown, MaterialTheme.colorScheme.primary, Modifier.size(112.dp))
            }
        }
    }
}

/** Una marca por forma de la secuencia: rellena las ya mostradas o elegidas. Solo estado, sin revelar nada. */
@Composable
private fun ProgressMarks(total: Int, done: Int) {
    val filled = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = "$done/$total" },
        horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm, Alignment.CenterHorizontally),
    ) {
        repeat(total) { index ->
            Canvas(Modifier.size(width = 24.dp, height = 4.dp)) {
                drawRect(if (index < done) filled else empty)
            }
        }
    }
}

@Composable
private fun ShapeChoice(kind: ShapeKind, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val description = shapeName(kind)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.aspectRatio(1f).semantics { contentDescription = description },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ShapeGlyph(
                kind,
                if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                Modifier.fillMaxSize(0.5f),
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
        ShapeKind.STAR -> R.string.challenge_shape_star
        ShapeKind.DIAMOND -> R.string.challenge_shape_diamond
    },
)

/** Silueta rellena de una forma, ocupando el cuadrado del modificador. */
@Composable
private fun ShapeGlyph(kind: ShapeKind, color: Color, modifier: Modifier) {
    Canvas(modifier) { drawShape(kind, color) }
}

private fun DrawScope.drawShape(kind: ShapeKind, color: Color) {
    val w = size.width
    val h = size.height
    when (kind) {
        ShapeKind.CIRCLE -> drawCircle(color, radius = minOf(w, h) / 2f)
        ShapeKind.SQUARE -> drawRect(color)
        ShapeKind.TRIANGLE -> drawPath(polygon(listOf(Offset(w / 2f, 0f), Offset(w, h), Offset(0f, h))), color)
        ShapeKind.DIAMOND ->
            drawPath(polygon(listOf(Offset(w / 2f, 0f), Offset(w, h / 2f), Offset(w / 2f, h), Offset(0f, h / 2f))), color)
        ShapeKind.STAR -> drawPath(star(w, h), color)
    }
}

private fun polygon(points: List<Offset>): Path = Path().apply {
    moveTo(points.first().x, points.first().y)
    points.drop(1).forEach { lineTo(it.x, it.y) }
    close()
}

/** Estrella de cinco puntas inscrita en el cuadrado, con la punta hacia arriba. */
private fun star(w: Float, h: Float): Path {
    val cx = w / 2f
    val cy = h / 2f
    val outer = minOf(w, h) / 2f
    val inner = outer * 0.45f
    return Path().apply {
        repeat(10) { i ->
            val radius = if (i % 2 == 0) outer else inner
            val angle = -PI / 2 + i * PI / 5
            val x = cx + (radius * cos(angle)).toFloat()
            val y = cy + (radius * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
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
