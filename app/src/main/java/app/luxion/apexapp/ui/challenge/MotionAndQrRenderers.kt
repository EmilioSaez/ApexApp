package app.luxion.apexapp.ui.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.qr.FrameLivenessChecker
import app.luxion.apexapp.domain.challenge.qr.LivenessVerdict
import app.luxion.apexapp.domain.challenge.qr.QrChallenge
import app.luxion.apexapp.domain.challenge.qr.QrParams
import app.luxion.apexapp.domain.challenge.sensor.MotionInstance
import app.luxion.apexapp.domain.challenge.sensor.MotionKind
import app.luxion.apexapp.domain.challenge.sensor.MotionSensors
import app.luxion.apexapp.scanner.BarcodeScanner
import app.luxion.apexapp.scanner.CameraPermissionGate
import app.luxion.apexapp.ui.challenge.motion.MotionChallengeContent
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject

private class MotionRenderer(
    private val kind: MotionKind,
    private val sensors: MotionSensors,
) : ChallengeRenderer {
    override val type: ChallengeType = ChallengeType(kind.typeId)

    @Composable
    override fun label(): String = stringResource(
        when (kind) {
            MotionKind.SHAKE -> R.string.challenge_type_shake
            MotionKind.STEPS -> R.string.challenge_type_steps
            MotionKind.STAND_UP -> R.string.challenge_type_standup
        },
    )

    @Composable
    override fun Render(
        instance: ChallengeInstance,
        wrong: Boolean,
        onAnswer: (ChallengeAnswer) -> Unit,
        onUnavailable: (reason: String) -> Unit,
    ) {
        val target = (instance as MotionInstance).target
        MotionChallengeContent(
            kind = kind,
            target = target,
            sensors = sensors,
            onVerified = { onAnswer(ChallengeAnswer.MotionVerified(type)) },
            onUnavailable = onUnavailable,
        )
    }
}

class ShakeRenderer @Inject constructor(sensors: MotionSensors) :
    ChallengeRenderer by MotionRenderer(MotionKind.SHAKE, sensors)

class StepsRenderer @Inject constructor(sensors: MotionSensors) :
    ChallengeRenderer by MotionRenderer(MotionKind.STEPS, sensors)

class StandUpRenderer @Inject constructor(sensors: MotionSensors) :
    ChallengeRenderer by MotionRenderer(MotionKind.STAND_UP, sensors)

/**
 * Reto QR: escanea con la cámara y solo entrega [ChallengeAnswer.ScanVerified] cuando la lectura pasa la
 * verificación de "vida". Una foto o pantalla del código se rechaza con un aviso.
 */
class QrRenderer @Inject constructor() : ChallengeRenderer {
    override val type: ChallengeType = QrChallenge.TYPE

    @Composable
    override fun label(): String = stringResource(R.string.challenge_type_qr)

    @Composable
    override fun Render(
        instance: ChallengeInstance,
        wrong: Boolean,
        onAnswer: (ChallengeAnswer) -> Unit,
        onUnavailable: (reason: String) -> Unit,
    ) {
        // Una instancia nueva (tras un fallo) reinicia la verificación.
        val checker = remember(instance) { FrameLivenessChecker() }
        var rejection by remember(instance) { mutableStateOf<LivenessVerdict.Reason?>(null) }
        var progress by remember(instance) { mutableStateOf(0f) }

        val view = LocalView.current
        DisposableEffect(view) {
            view.keepScreenOn = true
            onDispose { view.keepScreenOn = false }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md),
        ) {
            Text(
                stringResource(R.string.challenge_qr_prompt),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            CameraPermissionGate(modifier = Modifier.fillMaxWidth().height(ApexTheme.spacing.xxxl * 7)) {
                BarcodeScanner(
                    onScanned = {},
                    onFrame = { frame ->
                        when (val verdict = checker.onFrame(frame)) {
                            is LivenessVerdict.Collecting -> progress = verdict.progress
                            is LivenessVerdict.Rejected -> rejection = verdict.reason
                            is LivenessVerdict.Accepted -> {
                                rejection = null
                                onAnswer(ChallengeAnswer.ScanVerified(QrParams.hashOf(verdict.value)))
                            }
                        }
                    },
                    onError = { onUnavailable("camera error: ${it.message}") },
                    modifier = Modifier.fillMaxWidth().height(ApexTheme.spacing.xxxl * 7),
                )
            }
            val message = when {
                wrong -> R.string.challenge_qr_wrong
                rejection == LivenessVerdict.Reason.SCREEN -> R.string.challenge_qr_rejected_screen
                rejection == LivenessVerdict.Reason.STATIC_IMAGE -> R.string.challenge_qr_rejected_static
                progress > 0f -> R.string.challenge_qr_hold
                else -> null
            }
            if (message != null) {
                Text(
                    stringResource(message),
                    modifier = Modifier.padding(horizontal = ApexTheme.spacing.screen),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message == R.string.challenge_qr_hold) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }
    }
}
