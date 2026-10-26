package app.luxion.apexapp.ui.challenge.motion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.sensor.MotionKind
import app.luxion.apexapp.domain.challenge.sensor.MotionProgress
import app.luxion.apexapp.domain.challenge.sensor.MotionRunner
import app.luxion.apexapp.domain.challenge.sensor.MotionSensors
import app.luxion.apexapp.ui.theme.ApexTheme

/**
 * Ejecución de un reto de movimiento. Mantiene la pantalla encendida y registra los sensores solo mientras
 * está visible. [onVerified] se invoca una sola vez; [onUnavailable] si los sensores dejan de dar datos,
 * para que el motor aplique la reserva.
 */
@Composable
fun MotionChallengeContent(
    kind: MotionKind,
    target: Int,
    sensors: MotionSensors,
    onVerified: () -> Unit,
    onUnavailable: (reason: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var progress by remember(kind, target) { mutableStateOf(MotionProgress(0f)) }

    val view = LocalView.current
    androidx.compose.runtime.DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Los sensores continuos no entregan datos con la app en segundo plano o la pantalla bloqueada: sin esta
    // pausa el vigilante daba el sensor por muerto a los 10 s y sustituía el reto por la reserva.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(kind, target, sensors, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            MotionRunner(kind, target, sensors).progress().collect {
                if (it.stalled) {
                    onUnavailable("sensor stopped reporting data")
                } else {
                    progress = it
                    if (it.done) onVerified()
                }
            }
        }
    }

    MotionChallengeBody(kind, target, progress, modifier)
}

@Composable
fun MotionChallengeBody(
    kind: MotionKind,
    target: Int,
    progress: MotionProgress,
    modifier: Modifier = Modifier,
) {
    val title = when (kind) {
        MotionKind.SHAKE -> pluralStringResource(R.plurals.challenge_shake_prompt, target, target)
        MotionKind.STEPS -> pluralStringResource(R.plurals.challenge_steps_prompt, target, target)
        MotionKind.STAND_UP -> pluralStringResource(R.plurals.challenge_standup_prompt, target, target)
    }
    val description = stringResource(R.string.challenge_motion_progress, (progress.fraction * 100).toInt())
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ApexTheme.spacing.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        LinearProgressIndicator(
            progress = { progress.fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .padding(top = ApexTheme.spacing.xl)
                .fillMaxWidth()
                .semantics { contentDescription = description },
        )
        if (progress.rejected) {
            Text(
                stringResource(R.string.challenge_motion_rejected),
                modifier = Modifier.padding(top = ApexTheme.spacing.md),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Campo de configuración del objetivo. Muestra el valor y permite ajustarlo dentro de los límites del tipo;
 * al salir de rango se acota y se avisa.
 */
@Composable
fun MotionTargetField(
    kind: MotionKind,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val step = if (kind == MotionKind.STAND_UP) 5 else 10
    var clamped by remember(kind) { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md),
        ) {
            OutlinedButton(onClick = {
                // clamp() trata 0 como "sin definir" y lo cambia por el valor por defecto, así que se acota antes.
                val next = kind.clamp(maxOf(value - step, kind.min))
                clamped = value - step < kind.min
                onValueChange(next)
            }) { Text(stringResource(R.string.challenge_target_decrease)) }
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = ApexTheme.spacing.sm))
            OutlinedButton(onClick = {
                val next = kind.clamp(value + step)
                clamped = value + step > kind.max
                onValueChange(next)
            }) { Text(stringResource(R.string.challenge_target_increase)) }
        }
        if (clamped) {
            Text(
                stringResource(R.string.challenge_target_clamped, kind.min, kind.max),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
