package app.luxion.apexapp.ui.challenge

import android.view.InputDevice
import android.view.MotionEvent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import app.luxion.apexapp.domain.challenge.maze.MazeInstance
import app.luxion.apexapp.domain.challenge.maze.MazeLayout
import app.luxion.apexapp.domain.challenge.maze.MazeLevels
import app.luxion.apexapp.domain.challenge.maze.MazeSize
import app.luxion.apexapp.domain.challenge.maze.MazeTracker
import app.luxion.apexapp.domain.challenge.maze.TraceStatus
import app.luxion.apexapp.domain.challenge.touch.TouchAction
import app.luxion.apexapp.domain.challenge.touch.TouchSample
import app.luxion.apexapp.domain.challenge.touch.TouchVerdict
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject
import kotlinx.coroutines.delay

/** Alto máximo que se concede al calcular la celda; el laberinto mayor (9 filas × 40 dp) cabe de sobra. */
private const val AvailableHeightDp = 440f
private const val FailFlashMs = 450L
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
        MazeContent(instance as MazeInstance, wrong) { onAnswer(MazeTrace(type, it)) }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MazeContent(instance: MazeInstance, wrong: Boolean, onTrace: (List<Int>) -> Unit) {
    val maze = instance.maze
    val plausibility = rememberTouchPlausibility()
    val density = LocalDensity.current.density
    val answer by rememberUpdatedState(onTrace)

    var points by remember(instance) { mutableStateOf(emptyList<Offset>()) }
    var failedFlash by remember(instance) { mutableStateOf(false) }
    var rejected by remember(instance) { mutableStateOf(false) }
    var solved by remember(instance) { mutableStateOf(false) }
    var attempt by remember(instance) { mutableIntStateOf(1) }

    LaunchedEffect(failedFlash) {
        if (failedFlash) {
            val shown = points.size
            delay(FailFlashMs)
            failedFlash = false
            // Si el usuario ya empezó otro trazo no se toca.
            if (points.size == shown) points = emptyList()
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xs)) {
            Text(stringResource(R.string.challenge_maze_title), style = MaterialTheme.typography.titleMedium)
            if (wrong || rejected) {
                Text(
                    stringResource(if (rejected) R.string.challenge_maze_rejected else R.string.challenge_maze_wrong),
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.challenge_maze_attempt, attempt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            val cellPx = cellDp * density
            val tracker = remember(instance, cellDp) { MazeTracker(maze, MazeLayout.wallThickness(cellDp)) }
            val description = stringResource(R.string.challenge_maze_area_description)

            val wallColor = MaterialTheme.colorScheme.onSurfaceVariant
            val traceColor = MaterialTheme.colorScheme.primary
            val failColor = MaterialTheme.colorScheme.error
            val surface = MaterialTheme.colorScheme.surfaceContainerLow
            val exitColor = MaterialTheme.colorScheme.tertiary

            Box(
                Modifier
                    .size(Dp(cellDp * maze.cols), Dp(cellDp * maze.rows))
                    .clip(MaterialTheme.shapes.small)
                    .background(surface)
                    .semantics { contentDescription = description },
            ) {
                Canvas(
                    Modifier
                        .matchParentSize()
                        .pointerInteropFilter { event ->
                            if (solved) return@pointerInteropFilter true
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    val sample = TouchSample(
                                        tMillis = event.eventTime,
                                        action = TouchAction.PRESS,
                                        fromTouchscreen = event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN),
                                        obscured = event.flags and ObscuredFlags != 0,
                                    )
                                    if (plausibility.check(sample) is TouchVerdict.Rejected) {
                                        rejected = true
                                    } else {
                                        rejected = false
                                        if (tracker.begin(event.x / cellPx, event.y / cellPx)) {
                                            points = listOf(Offset(event.x, event.y))
                                        }
                                    }
                                }
                                MotionEvent.ACTION_MOVE -> if (tracker.active) {
                                    // Los puntos intermedios agrupados por Android evitan saltos entre eventos.
                                    var status: TraceStatus? = tracker.status
                                    for (i in 0 until event.historySize) {
                                        status = tracker.move(event.getHistoricalX(i) / cellPx, event.getHistoricalY(i) / cellPx)
                                        if (status != TraceStatus.TRACING) break
                                    }
                                    if (status == TraceStatus.TRACING) {
                                        status = tracker.move(event.x / cellPx, event.y / cellPx)
                                    }
                                    points = points + Offset(event.x, event.y)
                                    when (status) {
                                        TraceStatus.HIT_WALL -> {
                                            val cells = tracker.trace
                                            tracker.reset()
                                            failedFlash = true
                                            attempt++
                                            answer(cells)
                                        }
                                        TraceStatus.REACHED_EXIT -> {
                                            solved = true
                                            answer(tracker.trace)
                                        }
                                        else -> Unit
                                    }
                                }
                                // Soltar antes de llegar borra el trazo sin contar como fallo.
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    if (tracker.active) tracker.reset()
                                    points = emptyList()
                                }
                            }
                            true
                        },
                ) {
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
                    if (points.size > 1) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points.first().x, points.first().y)
                            points.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(
                            path,
                            if (failedFlash) failColor else traceColor,
                            style = Stroke(wall * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }
                CellIcon(R.drawable.ic_alarm, maze.col(maze.start), maze.row(maze.start), cellDp, traceColor, R.string.challenge_maze_start)
                CellIcon(R.drawable.ic_flag, maze.col(maze.exit), maze.row(maze.exit), cellDp, exitColor, R.string.challenge_maze_exit)
            }
        }
    }
}

@Composable
private fun CellIcon(
    drawable: Int,
    col: Int,
    row: Int,
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
