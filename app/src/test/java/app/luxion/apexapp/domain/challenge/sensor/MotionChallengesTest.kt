package app.luxion.apexapp.domain.challenge.sensor

import app.luxion.apexapp.domain.challenge.Availability
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.DeviceCapabilities
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionChallengesTest {
    private class Caps(
        override val hasAccelerometer: Boolean = true,
        override val hasStepSensor: Boolean = true,
        override val hasCamera: Boolean = true,
        override val hasActivityRecognitionPermission: Boolean = true,
        override val hasCameraPermission: Boolean = true,
    ) : DeviceCapabilities

    @Test
    fun `objetivo fuera de rango se ajusta al limite`() {
        assertEquals(10, MotionKind.SHAKE.targetFrom("""{"target":1}"""))
        assertEquals(100, MotionKind.SHAKE.targetFrom("""{"target":5000}"""))
        assertEquals(50, MotionKind.STEPS.targetFrom("""{"target":50}"""))
    }

    @Test
    fun `params vacios o invalidos dan el valor por defecto`() {
        assertEquals(MotionKind.SHAKE.default, MotionKind.SHAKE.targetFrom(""))
        assertEquals(MotionKind.STEPS.default, MotionKind.STEPS.targetFrom("{}"))
        assertEquals(MotionKind.STAND_UP.default, MotionKind.STAND_UP.targetFrom("no es json"))
    }

    @Test
    fun `params se serializan acotados`() {
        assertEquals("""{"target":100}""", MotionKind.SHAKE.paramsFor(999))
    }

    @Test
    fun `pasos no disponibles sin sensor o sin permiso`() {
        assertTrue(StepsChallenge(Caps(hasStepSensor = false)).availability() is Availability.Unavailable)
        assertTrue(StepsChallenge(Caps(hasActivityRecognitionPermission = false)).availability() is Availability.Unavailable)
        assertEquals(Availability.Available, StepsChallenge(Caps()).availability())
    }

    @Test
    fun `agitar y levantarse requieren acelerometro pero no permisos`() {
        val caps = Caps(hasStepSensor = false, hasActivityRecognitionPermission = false)
        assertEquals(Availability.Available, ShakeChallenge(caps).availability())
        assertEquals(Availability.Available, StandUpChallenge(caps).availability())
        assertTrue(ShakeChallenge(Caps(hasAccelerometer = false)).availability() is Availability.Unavailable)
    }

    @Test
    fun `solo la verificacion de movimiento de su tipo supera la instancia`() {
        val challenge = ShakeChallenge(Caps())
        val instance = challenge.generate(3, """{"target":15}""", Random(1))
        assertEquals(15, (instance as MotionInstance).target)
        assertTrue(instance.check(ChallengeAnswer.MotionVerified(ChallengeType("shake"))))
        assertFalse(instance.check(ChallengeAnswer.MotionVerified(ChallengeType("steps"))))
        assertFalse(instance.check(ChallengeAnswer.Text("15")))
    }
}
