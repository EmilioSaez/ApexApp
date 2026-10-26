package app.luxion.apexapp.ui.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.DeviceCapabilities
import app.luxion.apexapp.domain.challenge.qr.QrChallenge
import app.luxion.apexapp.domain.challenge.qr.QrParams
import app.luxion.apexapp.domain.challenge.sensor.MotionKind
import app.luxion.apexapp.ui.challenge.motion.ActivityRecognitionGate
import app.luxion.apexapp.ui.challenge.motion.MotionTargetInfo
import app.luxion.apexapp.ui.challenge.qr.QrCodeRegistration
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject

/**
 * Editor de un reto de movimiento. El objetivo (sacudidas, pasos o segundos de marcha) no se elige: sale de la
 * dificultad, y aquí solo se muestra el valor resultante.
 */
private class MotionParamsEditor(
    private val kind: MotionKind,
    private val capabilities: DeviceCapabilities,
    private val title: @Composable () -> String,
    /** Texto del objetivo ya con su valor, p. ej. «Objetivo: 30 pasos». */
    private val unit: @Composable (Int) -> String,
) : ChallengeParamsEditor {
    override val type: ChallengeType = ChallengeType(kind.typeId)

    @Composable
    override fun Content(params: String, difficulty: Int, onChange: (String) -> Unit) {
        val body: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
                Text(title(), style = MaterialTheme.typography.titleSmall)
                MotionTargetInfo(unit(kind.targetFor(difficulty)))
            }
        }
        if (kind == MotionKind.STEPS) {
            ActivityRecognitionGate(hasStepSensor = capabilities.hasStepSensor, content = body)
        } else {
            body()
        }
    }

    /** Sin contador de pasos el reto de pasos no se puede configurar. */
    override fun isValid(params: String): Boolean = kind != MotionKind.STEPS || capabilities.hasStepSensor
}

class ShakeParamsEditor @Inject constructor(capabilities: DeviceCapabilities) :
    ChallengeParamsEditor by MotionParamsEditor(
        MotionKind.SHAKE,
        capabilities,
        title = { stringResource(R.string.challenge_type_shake) },
        unit = { pluralStringResource(R.plurals.challenge_target_info_shakes, it, it) },
    )

class StepsParamsEditor @Inject constructor(capabilities: DeviceCapabilities) :
    ChallengeParamsEditor by MotionParamsEditor(
        MotionKind.STEPS,
        capabilities,
        title = { stringResource(R.string.challenge_type_steps) },
        unit = { pluralStringResource(R.plurals.challenge_target_info_steps, it, it) },
    )

class StandUpParamsEditor @Inject constructor(capabilities: DeviceCapabilities) :
    ChallengeParamsEditor by MotionParamsEditor(
        MotionKind.STAND_UP,
        capabilities,
        title = { stringResource(R.string.challenge_type_standup) },
        unit = { pluralStringResource(R.plurals.challenge_target_info_seconds, it, it) },
    )

/** Registro del código del reto QR; no deja guardar la alarma mientras no haya uno. */
class QrParamsEditor @Inject constructor() : ChallengeParamsEditor {
    override val type: ChallengeType = QrChallenge.TYPE

    @Composable
    override fun Content(params: String, difficulty: Int, onChange: (String) -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
            Text(stringResource(R.string.challenge_type_qr), style = MaterialTheme.typography.titleSmall)
            QrCodeRegistration(currentParams = params, onParamsChange = onChange, modifier = Modifier)
        }
    }

    override fun isValid(params: String): Boolean = QrParams.isRegistered(params)
}
