package app.luxion.apexapp.ui.challenge

import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.holdrelease.HoldPhase
import app.luxion.apexapp.domain.challenge.holdrelease.HoldReleaseChallenge
import app.luxion.apexapp.domain.challenge.holdrelease.HoldReleaseInstance
import app.luxion.apexapp.domain.challenge.holdrelease.RoundOutcome
import app.luxion.apexapp.domain.challenge.touch.TouchAction
import app.luxion.apexapp.domain.challenge.touch.TouchSample
import app.luxion.apexapp.domain.challenge.touch.TouchVerdict
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject
import kotlinx.coroutines.delay

class HoldReleaseRenderer @Inject constructor() : ChallengeRenderer {
    override val type: ChallengeType = HoldReleaseChallenge.TYPE

    @Composable
    override fun label(): String = stringResource(R.string.challenge_type_holdrelease)

    @Composable
    override fun Render(
        instance: ChallengeInstance,
        wrong: Boolean,
        onAnswer: (ChallengeAnswer) -> Unit,
        onUnavailable: (reason: String) -> Unit,
    ) {
        HoldReleaseContent(instance as HoldReleaseInstance) { onAnswer(ChallengeAnswer.GameVerified(type)) }
    }
}

@Composable
private fun HoldReleaseContent(instance: HoldReleaseInstance, onVerified: () -> Unit) {
    val game = remember(instance) { instance.newGame() }
    val plausibility = rememberTouchPlausibility()
    // La máquina de rondas no es estado de Compose: este contador fuerza la recomposición tras cada cambio.
    var version by remember(game) { mutableIntStateOf(0) }
    var lastFailure by remember(game) { mutableStateOf<RoundOutcome?>(null) }

    fun handle(outcome: RoundOutcome?) {
        if (outcome == null) return
        lastFailure = outcome.takeIf { it != RoundOutcome.HIT }
        version++
        if (game.done) onVerified()
    }

    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Perder el foco con el dedo puesto da la ronda por fallada.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, game) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) handle(game.cancel())
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(game) {
        while (!game.done) {
            delay(TICK_MS)
            val before = game.phase
            val outcome = game.tick(SystemClock.elapsedRealtime())
            if (outcome != null) handle(outcome) else if (game.phase != before) version++
        }
    }

    val phase = game.phase.also { version }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg),
    ) {
        Text(
            stringResource(R.string.challenge_holdrelease_round, game.roundsDone.coerceAtMost(game.totalRounds - 1) + 1, game.totalRounds),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HoldPad(
            phase = phase,
            modifier = Modifier.pointerInput(game, plausibility) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pressEvent = currentEvent
                    if (plausibility.check(pressEvent.sample(SystemClock.elapsedRealtime(), TouchAction.PRESS, down.type)) is TouchVerdict.Accepted) {
                        game.press(SystemClock.elapsedRealtime())
                        lastFailure = null
                        version++
                    }
                    var event: PointerEvent
                    do {
                        event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    val accepted = plausibility.check(
                        event.sample(SystemClock.elapsedRealtime(), TouchAction.RELEASE, down.type),
                    ) is TouchVerdict.Accepted
                    handle(if (accepted) game.release(SystemClock.elapsedRealtime()) else game.cancel())
                }
            },
        )
        Text(
            lastFailure?.let { stringResource(failureText(it)) }.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

private const val TICK_MS = 16L

private fun failureText(outcome: RoundOutcome): Int = when (outcome) {
    RoundOutcome.EARLY -> R.string.challenge_holdrelease_early
    RoundOutcome.LATE -> R.string.challenge_holdrelease_late
    else -> R.string.challenge_holdrelease_cancelled
}

/** El control cambia de forma y de texto al llegar la señal, no solo de color. */
@Composable
private fun HoldPad(phase: HoldPhase, modifier: Modifier = Modifier) {
    val signal = phase == HoldPhase.SIGNAL
    val color = when {
        signal -> MaterialTheme.colorScheme.primary
        phase == HoldPhase.HOLDING -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    Box(
        modifier.size(240.dp).background(color, if (signal) RoundedCornerShape(32.dp) else CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(
                when (phase) {
                    HoldPhase.IDLE -> R.string.challenge_holdrelease_hold
                    HoldPhase.HOLDING -> R.string.challenge_holdrelease_wait
                    else -> R.string.challenge_holdrelease_release
                },
            ),
            style = MaterialTheme.typography.headlineMedium,
            color = if (signal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

private fun PointerEvent.sample(t: Long, action: TouchAction, type: PointerType): TouchSample {
    val flags = motionEvent?.flags ?: 0
    val obscured = flags and (MotionEvent.FLAG_WINDOW_IS_OBSCURED or MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED) != 0
    return TouchSample(t, action, fromTouchscreen = type == PointerType.Touch, obscured = obscured)
}
