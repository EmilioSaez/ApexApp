package app.luxion.apexapp.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.challenge.sensor.MotionKind

@StringRes
private fun challengeLabelRes(type: String): Int? = when (type) {
    "math" -> R.string.challenge_type_math
    MotionKind.SHAKE.typeId -> R.string.challenge_type_shake
    MotionKind.STEPS.typeId -> R.string.challenge_type_steps
    "qr" -> R.string.challenge_type_qr
    else -> null
}

/** Nombres de los tipos de reto de la alarma, sin repetir y en orden; `null` si no hay ninguno reconocible. */
@Composable
fun challengeTypesLabel(config: ChallengeConfig): String? {
    val names = config.challenges.mapNotNull { challengeLabelRes(it.type) }.distinct().map { stringResource(it) }
    return names.takeIf { it.isNotEmpty() }?.joinToString(", ")
}
