package app.luxion.apexapp.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.challenge.flappy.FlappyChallenge
import app.luxion.apexapp.domain.challenge.sensor.MotionKind
import app.luxion.apexapp.domain.challenge.touch.TapTargetsChallenge

@StringRes
private fun challengeLabelRes(type: String): Int? = when (type) {
    "math" -> R.string.challenge_type_math
    MotionKind.SHAKE.typeId -> R.string.challenge_type_shake
    MotionKind.STEPS.typeId -> R.string.challenge_type_steps
    "memory" -> R.string.challenge_type_memory
    "shapes" -> R.string.challenge_type_shapes
    "qr" -> R.string.challenge_type_qr
    "holdrelease" -> R.string.challenge_type_holdrelease
    FlappyChallenge.TYPE.id -> R.string.challenge_type_flappy
    else -> null
}

/** Nombres de los tipos de reto de la alarma, sin repetir y en orden; `null` si no hay ninguno reconocible. */
@Composable
fun challengeTypesLabel(config: ChallengeConfig): String? {
    val names = config.challenges.mapNotNull { challengeLabelRes(it.type) }.distinct().map { stringResource(it) }
    return names.takeIf { it.isNotEmpty() }?.joinToString(", ")
}
