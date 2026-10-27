package app.luxion.apexapp.ui.challenge.rhythm

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.rhythm.RhythmChallenge
import app.luxion.apexapp.domain.challenge.rhythm.RhythmInstance
import app.luxion.apexapp.domain.challenge.rhythm.RhythmParams
import app.luxion.apexapp.domain.challenge.rhythm.RhythmPattern
import app.luxion.apexapp.domain.challenge.rhythm.RhythmRound
import app.luxion.apexapp.domain.challenge.rhythm.RhythmTuning
import app.luxion.apexapp.domain.challenge.touch.TouchAction
import app.luxion.apexapp.domain.challenge.touch.TouchSample
import app.luxion.apexapp.domain.challenge.touch.TouchVerdict
import app.luxion.apexapp.ui.challenge.ChallengeParamsEditor
import app.luxion.apexapp.ui.challenge.ChallengeRenderer
import app.luxion.apexapp.ui.challenge.rememberTouchPlausibility
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject
import kotlin.random.Random

class RhythmRenderer @Inject constructor() : ChallengeRenderer {
    override val type: ChallengeType = RhythmChallenge.TYPE

    @Composable
    override fun label(): String = stringResource(R.string.challenge_type_rhythm)

    @Composable
    override fun Render(
        instance: ChallengeInstance,
        wrong: Boolean,
        onAnswer: (ChallengeAnswer) -> Unit,
        onUnavailable: (reason: String) -> Unit,
    ) {
        RhythmGame(instance as RhythmInstance) { onAnswer(ChallengeAnswer.GameVerified(type)) }
    }
}

/** Selector del número de rondas, entre [RhythmParams.MIN_ROUNDS] y [RhythmParams.MAX_ROUNDS]. */
class RhythmParamsEditor @Inject constructor() : ChallengeParamsEditor {
    override val type: ChallengeType = RhythmChallenge.TYPE

    @Composable
    override fun Content(params: String, difficulty: Int, onChange: (String) -> Unit) {
        val rounds = RhythmParams.rounds(params)
        Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
            Text(stringResource(R.string.challenge_type_rhythm), style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg)) {
                OutlinedIconButton(
                    onClick = { onChange(RhythmParams.paramsFor(rounds - 1)) },
                    enabled = rounds > RhythmParams.MIN_ROUNDS,
                ) { Text("-") }
                Text(stringResource(R.string.challenge_rhythm_rounds, rounds), style = MaterialTheme.typography.titleMedium)
                OutlinedIconButton(
                    onClick = { onChange(RhythmParams.paramsFor(rounds + 1)) },
                    enabled = rounds < RhythmParams.MAX_ROUNDS,
                ) { Text("+") }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RhythmGame(instance: RhythmInstance, onCleared: () -> Unit) {
    val plausibility = rememberTouchPlausibility()
    val random = remember(instance) { Random(instance.seed) }
    var roundNumber by remember(instance) { mutableIntStateOf(1) }
    var attempt by remember(instance) { mutableIntStateOf(0) }
    var restartedRound by remember(instance) { mutableIntStateOf(0) }
    val round = remember(instance, roundNumber, attempt) { RhythmRound(RhythmPattern.generate(random)) }
    var startMs by remember(round) { mutableLongStateOf(0L) }
    // Se lee al dibujar: cada fotograma y cada toque lo actualizan y provocan la recomposición.
    var elapsed by remember(round) { mutableLongStateOf(0L) }
    var widthPx by remember { mutableIntStateOf(0) }

    val music = remember { RhythmMusic() }
    DisposableEffect(music) { onDispose { music.release() } }

    LaunchedEffect(round) {
        music.restart()
        val start = SystemClock.uptimeMillis()
        startMs = start
        while (round.status == RhythmRound.Status.PLAYING) {
            withFrameMillis {
                elapsed = SystemClock.uptimeMillis() - start
                round.advance(elapsed)
            }
        }
        when (round.status) {
            RhythmRound.Status.CLEARED -> if (roundNumber >= instance.rounds) onCleared() else roundNumber++
            RhythmRound.Status.RESTART -> {
                restartedRound = roundNumber
                attempt++
            }
            RhythmRound.Status.PLAYING -> Unit
        }
    }

    val laneColor = MaterialTheme.colorScheme.surfaceContainerLow
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val noteColor = MaterialTheme.colorScheme.primary
    val lineColor = MaterialTheme.colorScheme.outline

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(R.string.challenge_rhythm_round, roundNumber, instance.rounds),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.challenge_rhythm_failures, round.failures, RhythmTuning.MAX_FAILURES),
                style = MaterialTheme.typography.titleMedium,
                color = if (round.failures > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            stringResource(
                if (restartedRound == roundNumber) R.string.challenge_rhythm_restarted else R.string.challenge_rhythm_hint,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(380.dp)
                .onSizeChanged { widthPx = it.width }
                .pointerInteropFilter { motion ->
                    val action = motion.actionMasked
                    if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_POINTER_DOWN) return@pointerInteropFilter true
                    val index = motion.actionIndex
                    val sample = TouchSample(
                        tMillis = motion.eventTime,
                        action = TouchAction.PRESS,
                        fromTouchscreen = motion.isFromSource(InputDevice.SOURCE_TOUCHSCREEN),
                        obscured = motion.flags and (MotionEvent.FLAG_WINDOW_IS_OBSCURED or MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED) != 0,
                    )
                    if (plausibility.check(sample) is TouchVerdict.Accepted && widthPx > 0) {
                        val lane = (motion.getX(index) / widthPx * RhythmTuning.LANES).toInt().coerceIn(0, RhythmTuning.LANES - 1)
                        val t = motion.eventTime - startMs
                        round.tap(lane, t)
                        elapsed = t
                    }
                    true
                },
        ) {
            val laneWidth = size.width / RhythmTuning.LANES
            val hitY = size.height * HIT_LINE
            val noteHeight = size.height * 0.06f
            repeat(RhythmTuning.LANES) { lane ->
                drawRect(laneColor, Offset(lane * laneWidth, 0f), Size(laneWidth, size.height))
                if (lane > 0) drawLine(dividerColor, Offset(lane * laneWidth, 0f), Offset(lane * laneWidth, size.height), 2f)
            }
            drawLine(lineColor, Offset(0f, hitY), Offset(size.width, hitY), 4f)
            round.notes.forEachIndexed { i, note ->
                if (round.isResolved(i)) return@forEachIndexed
                val progress = 1f - (note.timeMs - elapsed).toFloat() / RhythmTuning.FALL_MS
                if (progress < 0f) return@forEachIndexed
                drawRoundRect(
                    noteColor,
                    Offset(note.lane * laneWidth + laneWidth * 0.12f, hitY * progress - noteHeight / 2),
                    Size(laneWidth * 0.76f, noteHeight),
                    cornerRadius = CornerRadius(12f, 12f),
                )
            }
            drawRect(lineColor, Offset.Zero, size, style = Stroke(2f))
        }
    }
}

/** La línea de impacto está a esta fracción de la altura; las notas caen hasta ella. */
private const val HIT_LINE = 0.85f
