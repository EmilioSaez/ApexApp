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
import app.luxion.apexapp.domain.challenge.sensor.MotionInstance
import app.luxion.apexapp.domain.challenge.sensor.MotionKind
import app.luxion.apexapp.domain.challenge.sensor.MotionSensors
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
