package app.luxion.apexapp.domain.challenge.sensor

import app.luxion.apexapp.domain.challenge.Availability
import app.luxion.apexapp.domain.challenge.Challenge
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.DeviceCapabilities
import javax.inject.Inject
import kotlin.random.Random

/** Instancia de un reto de movimiento: se supera con [ChallengeAnswer.MotionVerified] de su tipo. */
class MotionInstance(
    override val type: ChallengeType,
    override val level: Int,
    override val prompt: String,
    /** Objetivo ya acotado: sacudidas, pasos o segundos de marcha según el tipo. */
    val target: Int,
) : ChallengeInstance {
    override fun check(answer: ChallengeAnswer): Boolean =
        answer is ChallengeAnswer.MotionVerified && answer.type == type
}

abstract class MotionChallenge(
    private val kind: MotionKind,
    private val capabilities: DeviceCapabilities,
) : Challenge {
    override val type: ChallengeType = ChallengeType(kind.typeId)

    override fun availability(): Availability = unavailableReason()?.let(Availability::Unavailable)
        ?: Availability.Available

    private fun unavailableReason(): String? = when {
        !capabilities.hasAccelerometer -> "no accelerometer"
        kind == MotionKind.STEPS && !capabilities.hasStepSensor -> "no step sensor"
        kind == MotionKind.STEPS && !capabilities.hasActivityRecognitionPermission ->
            "activity recognition permission missing"
        else -> null
    }

    override fun generate(level: Int, params: String, random: Random): ChallengeInstance {
        val target = kind.targetFrom(params)
        return MotionInstance(type, level, "${kind.typeId}:$target", target)
    }
}

class ShakeChallenge @Inject constructor(capabilities: DeviceCapabilities) :
    MotionChallenge(MotionKind.SHAKE, capabilities)

class StepsChallenge @Inject constructor(capabilities: DeviceCapabilities) :
    MotionChallenge(MotionKind.STEPS, capabilities)

class StandUpChallenge @Inject constructor(capabilities: DeviceCapabilities) :
    MotionChallenge(MotionKind.STAND_UP, capabilities)
