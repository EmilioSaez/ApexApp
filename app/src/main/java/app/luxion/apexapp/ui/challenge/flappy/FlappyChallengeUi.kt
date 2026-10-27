package app.luxion.apexapp.ui.challenge.flappy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.flappy.FlappyGame
import app.luxion.apexapp.domain.challenge.flappy.FlappyPhase
import app.luxion.apexapp.ui.theme.ApexTheme

/**
 * Ejecución del reto Flappy: un toque hace saltar al pájaro y hay que sumar [target] tuberías sin chocar
 * (al chocar se vuelve a empezar de cero). Mantiene la pantalla encendida y se pausa fuera de primer plano.
 * [onCompleted] se invoca una sola vez al llegar al objetivo.
 */
@Composable
fun FlappyChallengeContent(
    target: Int,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = remember(target) { FlappyGame(target) }
    // Cambia en cada fotograma para invalidar el dibujo: el juego guarda su estado fuera de Compose.
    var frame by remember(game) { mutableLongStateOf(0L) }
    // Estado que sí recompone el texto: solo cambia al puntuar o al cambiar de fase, no en cada fotograma.
    var score by remember(game) { mutableIntStateOf(0) }
    var phase by remember(game) { mutableStateOf(FlappyPhase.READY) }
    val currentOnCompleted = rememberUpdatedState(onCompleted)

    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(game, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var last = 0L
            while (game.phase != FlappyPhase.WON) {
                withFrameNanos { now ->
                    if (last != 0L) game.step((now - last) / NanosPerSecond)
                    last = now
                    frame = now
                    if (score != game.score) score = game.score
                    if (phase != game.phase) phase = game.phase
                }
            }
            currentOnCompleted.value()
        }
    }

    val bird = MaterialTheme.colorScheme.primary
    val pipe = MaterialTheme.colorScheme.tertiary
    val sky = MaterialTheme.colorScheme.surfaceContainerLow
    val ground = MaterialTheme.colorScheme.outlineVariant
    val scoreText = stringResource(R.string.challenge_flappy_score, score, target)

    Column(
        modifier = modifier.fillMaxWidth().padding(ApexTheme.spacing.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md),
    ) {
        Text(
            pluralStringResource(R.plurals.challenge_flappy_prompt, target, target),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            scoreText,
            modifier = Modifier.semantics { contentDescription = scoreText },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(FlappyGame.WIDTH / FlappyGame.HEIGHT)
                .pointerInput(game) { detectTapGestures(onPress = { game.flap() }) },
        ) {
            frame.hashCode() // lee el estado de fotograma para que el dibujo se invalide
            drawGame(game, sky, pipe, bird, ground)
        }
        val hint = when (phase) {
            FlappyPhase.READY -> R.string.challenge_flappy_tap_to_start
            FlappyPhase.CRASHED -> R.string.challenge_flappy_crashed
            else -> null
        }
        if (hint != null) {
            Text(
                stringResource(hint),
                style = MaterialTheme.typography.bodyMedium,
                color = if (hint == R.string.challenge_flappy_crashed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun DrawScope.drawGame(game: FlappyGame, sky: Color, pipe: Color, bird: Color, ground: Color) {
    val scale = size.width / FlappyGame.WIDTH
    drawRoundRect(sky, size = size, cornerRadius = CornerRadius(12f * scale))
    game.pipes.forEach { p ->
        val top = p.gapCenter - FlappyGame.GAP / 2
        val bottom = p.gapCenter + FlappyGame.GAP / 2
        drawRect(pipe, Offset(p.x * scale, 0f), Size(FlappyGame.PIPE_WIDTH * scale, top * scale))
        drawRect(pipe, Offset(p.x * scale, bottom * scale), Size(FlappyGame.PIPE_WIDTH * scale, (FlappyGame.HEIGHT - bottom) * scale))
    }
    drawRect(ground, Offset(0f, (FlappyGame.HEIGHT - GroundHeight) * scale), Size(size.width, GroundHeight * scale))
    drawCircle(bird, FlappyGame.BIRD_RADIUS * scale, Offset(FlappyGame.BIRD_X * scale, game.birdY * scale))
}

private const val NanosPerSecond = 1_000_000_000f
private const val GroundHeight = 6f
