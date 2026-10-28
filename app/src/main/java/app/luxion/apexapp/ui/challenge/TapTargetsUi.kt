package app.luxion.apexapp.ui.challenge

import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.touch.TapResult
import app.luxion.apexapp.domain.challenge.touch.TapRound
import app.luxion.apexapp.domain.challenge.touch.TapTargetsChallenge
import app.luxion.apexapp.domain.challenge.touch.TapTargetsInstance
import app.luxion.apexapp.domain.challenge.touch.TapTargetsTuning
import app.luxion.apexapp.domain.challenge.touch.TouchAction
import app.luxion.apexapp.domain.challenge.touch.TouchSample
import app.luxion.apexapp.domain.challenge.touch.TouchVerdict
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class TapTargetsRenderer @Inject constructor() : ChallengeRenderer {
    override val type: ChallengeType = TapTargetsChallenge.TYPE

    @Composable
    override fun label(): String = stringResource(R.string.challenge_type_tap_targets)

    @Composable
    override fun Render(
        instance: ChallengeInstance,
        wrong: Boolean,
        onAnswer: (ChallengeAnswer) -> Unit,
        onUnavailable: (reason: String) -> Unit,
    ) {
        TapTargetsContent(instance as TapTargetsInstance) { onAnswer(ChallengeAnswer.GameVerified(type)) }
    }
}

/** Estado visible de la ronda: la ronda en curso (null durante la pausa) y los círculos ya tocados. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TapTargetsContent(instance: TapTargetsInstance, onVerified: () -> Unit) {
    val game = remember(instance) { instance.newGame() }
    val plausibility = rememberTouchPlausibility()
    val density = LocalDensity.current
    val verified by rememberUpdatedState(onVerified)

    var round by remember(instance) { mutableStateOf<TapRound?>(null) }
    var hits by remember(instance) { mutableStateOf(emptySet<Int>()) }
    var message by remember(instance) { mutableStateOf<Int?>(null) }
    val timeLeft = remember(instance) { Animatable(1f) }
    val cleared = remember(instance) { Channel<Unit>(Channel.CONFLATED) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md)) {
        Text(
            stringResource(message ?: R.string.tap_targets_hint),
            style = MaterialTheme.typography.labelMedium,
            color = if (message == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        )
        LinearProgressIndicator(progress = { timeLeft.value }, modifier = Modifier.fillMaxWidth())
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(AreaHeight)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            val widthDp = maxWidth.value
            val heightDp = maxHeight.value
            val active = round
            val left = (active?.circles?.size ?: 0) - hits.size
            val color = MaterialTheme.colorScheme.primary
            val description = stringResource(R.string.tap_targets_area_description, left.coerceAtLeast(0))

            LaunchedEffect(instance, widthDp, heightDp) {
                while (!game.done) {
                    val next = game.nextRound(widthDp, heightDp)
                    round = null
                    hits = emptySet()
                    timeLeft.snapTo(1f)
                    delay(next.gapMs)
                    round = next
                    val ok = coroutineScope {
                        val countdown = launch { timeLeft.animateTo(0f, tween(next.timeLimitMs.toInt(), easing = LinearEasing)) }
                        val result = withTimeoutOrNull(next.timeLimitMs) { cleared.receive() }
                        countdown.cancel()
                        result != null
                    }
                    if (ok) {
                        message = null
                    } else {
                        game.expire()
                        message = R.string.tap_targets_timeout
                    }
                }
                round = null
                verified()
            }

            Canvas(
                Modifier
                    .matchParentSize()
                    .semantics { contentDescription = description }
                    .pointerInteropFilter { event ->
                        if (event.actionMasked != MotionEvent.ACTION_DOWN) return@pointerInteropFilter true
                        val sample = TouchSample(
                            tMillis = event.eventTime,
                            action = TouchAction.PRESS,
                            fromTouchscreen = event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN),
                            obscured = event.flags and ObscuredFlags != 0,
                        )
                        if (plausibility.check(sample) is TouchVerdict.Rejected) {
                            message = R.string.tap_targets_rejected
                            return@pointerInteropFilter true
                        }
                        if (message == R.string.tap_targets_rejected) message = null
                        val result = game.tap(event.x / density.density, event.y / density.density)
                        hits = game.hitCircles.toSet()
                        if (result == TapResult.ROUND_CLEARED) cleared.trySend(Unit)
                        true
                    },
            ) {
                active?.let { r ->
                    r.circles.forEachIndexed { i, circle ->
                        if (i !in hits) {
                            drawCircle(
                                color,
                                radius = r.diameterDp / 2f * density.density,
                                center = Offset(circle.xDp * density.density, circle.yDp * density.density),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val AreaHeight = 360.dp
private const val ObscuredFlags = MotionEvent.FLAG_WINDOW_IS_OBSCURED or MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED

/** Editor del reto: la ronda única sale de la dificultad, aquí solo se muestra lo que exige. */
class TapTargetsParamsEditor @Inject constructor() : ChallengeParamsEditor {
    override val type: ChallengeType = TapTargetsChallenge.TYPE

    @Composable
    override fun Content(params: String, difficulty: Int, onChange: (String) -> Unit) {
        val level = TapTargetsTuning.forLevel(difficulty)
        Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
            Text(stringResource(R.string.challenge_type_tap_targets), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.tap_targets_level_info, level.circles, level.roundMs / 1000f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
