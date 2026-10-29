package app.luxion.apexapp.ui.challenge

import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.MazeTrace
import app.luxion.apexapp.domain.challenge.maze.MazeChallenge
import app.luxion.apexapp.domain.challenge.maze.MazeDirection
import app.luxion.apexapp.domain.challenge.maze.MazeInstance
import app.luxion.apexapp.domain.challenge.maze.MazeLayout
import app.luxion.apexapp.domain.challenge.maze.MazeLevels
import app.luxion.apexapp.domain.challenge.maze.MazeSize
import app.luxion.apexapp.domain.challenge.touch.TouchAction
import app.luxion.apexapp.domain.challenge.touch.TouchSample
import app.luxion.apexapp.domain.challenge.touch.TouchVerdict
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.launch

/** Alto máximo que se concede al calcular la celda; el laberinto mayor (9 filas × 40 dp) cabe de sobra. */
private const val AvailableHeightDp = 440f
/** Distancia mínima del dedo para contar como swipe. */
private const val SwipeThresholdDp = 24f

/** Duración de la animación de la ficha por cada celda. */
private const val StepMs = 110
private val IconSize = 28.dp

class MazeRenderer @Inject constructor() : ChallengeRenderer {
    override val type: ChallengeType = MazeChallenge.TYPE

    @Composable
    override fun label(): String = stringResource(R.string.challenge_type_maze)

    @Composable
    override fun Render(
        instance: ChallengeInstance,
        wrong: Boolean,
        onAnswer: (ChallengeAnswer) -> Unit,
        onUnavailable: (reason: String) -> Unit,
    ) {
        MazeContent(instance as MazeInstance) { onAnswer(MazeTrace(type, it)) }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MazeContent(instance: MazeInstance, onTrace: (List<Int>) -> Unit) {
    val maze = instance.maze
    val plausibility = rememberTouchPlausibility()
    val density = LocalDensity.current.density
    val answer by rememberUpdatedState(onTrace)

    var rejected by remember(instance) { mutableStateOf(false) }
    var solved by remember(instance) { mutableStateOf(false) }
    var moving by remember(instance) { mutableStateOf(false) }
    // Celdas por las que ha pasado la ficha, empezando en la entrada.
    val trace = remember(instance) { mutableListOf(maze.start) }
    val position = remember(instance) {
        Animatable(Offset(maze.col(maze.start).toFloat(), maze.row(maze.start).toFloat()), Offset.VectorConverter)
    }
    val scope = rememberCoroutineScope()

    fun swipe(direction: MazeDirection) {
        val path = maze.slide(trace.last(), direction)
        if (path.isEmpty()) return
        moving = true
        scope.launch {
            for (cell in path) {
                position.animateTo(Offset(maze.col(cell).toFloat(), maze.row(cell).toFloat()), tween(StepMs, easing = LinearEasing))
                trace += cell
            }
            moving = false
            if (trace.last() == maze.exit) {
                solved = true
                answer(trace.toList())
            }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xs)) {
            Text(stringResource(R.string.challenge_maze_title), style = MaterialTheme.typography.titleMedium)
            if (rejected) {
                Text(
                    stringResource(R.string.challenge_maze_rejected),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.small)
                        .padding(horizontal = ApexTheme.spacing.md, vertical = ApexTheme.spacing.sm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else {
                Text(
                    stringResource(R.string.challenge_maze_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xs + 2.dp)) {
                Box(Modifier.size(16.dp, 3.dp).background(MaterialTheme.colorScheme.tertiary))
                Text(
                    stringResource(R.string.challenge_maze_exit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            val size = MazeSize(maze.cols, maze.rows)
            val cellDp = MazeLayout.cellDp(maxWidth.value, AvailableHeightDp, size)
            val swipeThresholdPx = SwipeThresholdDp * density
            val description = stringResource(R.string.challenge_maze_area_description)

            val wallColor = MaterialTheme.colorScheme.onSurfaceVariant
            val tokenColor = MaterialTheme.colorScheme.primary
            val surface = MaterialTheme.colorScheme.surfaceContainerLow
            val exitColor = MaterialTheme.colorScheme.tertiary

            Box(
                Modifier
                    .size(Dp(cellDp * maze.cols), Dp(cellDp * maze.rows))
                    .clip(MaterialTheme.shapes.small)
                    .background(surface)
                    .semantics { contentDescription = description },
            ) {
                var downX = 0f
                var downY = 0f
                var swiping = false
                Canvas(
                    Modifier
                        .matchParentSize()
                        .pointerInteropFilter { event ->
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    val sample = TouchSample(
                                        tMillis = event.eventTime,
                                        action = TouchAction.PRESS,
                                        fromTouchscreen = event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN),
                                        obscured = event.flags and ObscuredFlags != 0,
                                    )
                                    rejected = plausibility.check(sample) is TouchVerdict.Rejected
                                    swiping = !rejected
                                    downX = event.x
                                    downY = event.y
                                }
                                // El swipe se resuelve en cuanto el dedo supera el umbral, una vez por gesto.
                                MotionEvent.ACTION_MOVE -> if (swiping && !solved && !moving) {
                                    val direction = swipeDirection(event.x - downX, event.y - downY, swipeThresholdPx)
                                    if (direction != null) {
                                        swiping = false
                                        swipe(direction)
                                    }
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> swiping = false
                            }
                            true
                        },
                ) {
                    val cellPx = cellDp * density
                    val wall = MazeLayout.WALL_DP * density
                    maze.walls.forEach { w ->
                        drawLine(
                            wallColor,
                            Offset(w.x1 * cellPx, w.y1 * cellPx),
                            Offset(w.x2 * cellPx, w.y2 * cellPx),
                            strokeWidth = wall,
                            cap = StrokeCap.Square,
                        )
                    }
                }
                CellIcon(R.drawable.ic_flag, maze.col(maze.exit).toFloat(), maze.row(maze.exit).toFloat(), cellDp, exitColor, R.string.challenge_maze_exit)
                CellIcon(R.drawable.ic_alarm, position.value.x, position.value.y, cellDp, tokenColor, R.string.challenge_maze_start)
            }
        }
    }
}

/** Dirección dominante de un desplazamiento del dedo, o null si no supera [threshold] píxeles. */
internal fun swipeDirection(dx: Float, dy: Float, threshold: Float): MazeDirection? {
    if (maxOf(abs(dx), abs(dy)) < threshold) return null
    return if (abs(dx) > abs(dy)) {
        if (dx > 0) MazeDirection.EAST else MazeDirection.WEST
    } else {
        if (dy > 0) MazeDirection.SOUTH else MazeDirection.NORTH
    }
}

@Composable
private fun CellIcon(
    drawable: Int,
    col: Float,
    row: Float,
    cellDp: Float,
    tint: androidx.compose.ui.graphics.Color,
    description: Int,
) {
    val offset = (cellDp - IconSize.value) / 2f
    Icon(
        painterResource(drawable),
        contentDescription = stringResource(description),
        tint = tint,
        modifier = Modifier.offset((cellDp * col + offset).dp, (cellDp * row + offset).dp).size(IconSize),
    )
}

private const val ObscuredFlags = MotionEvent.FLAG_WINDOW_IS_OBSCURED or MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED

/** Editor sin parámetros: el tamaño sale de la dificultad, aquí solo se muestra. */
class MazeParamsEditor @Inject constructor() : ChallengeParamsEditor {
    override val type: ChallengeType = MazeChallenge.TYPE

    @Composable
    override fun Content(params: String, difficulty: Int, onChange: (String) -> Unit) {
        val size = MazeLevels.sizeFor(difficulty)
        Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
            Text(stringResource(R.string.challenge_type_maze), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.challenge_maze_level_info, size.cols, size.rows),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
