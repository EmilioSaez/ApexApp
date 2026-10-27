package app.luxion.apexapp.domain.challenge.sensor

import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.challenge.Availability
import app.luxion.apexapp.domain.challenge.ChallengeContext
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.DifficultyProvider
import app.luxion.apexapp.domain.challenge.ChallengeSession
import app.luxion.apexapp.domain.challenge.SessionConfig
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
        override val hasActivityRecognitionPermission: Boolean = true,
    ) : DeviceCapabilities

    @Test
    fun `la tabla fija los extremos de cada reto`() {
        assertEquals(10, MotionKind.SHAKE.targetFor(1))
        assertEquals(60, MotionKind.SHAKE.targetFor(5))
        assertEquals(15, MotionKind.STEPS.targetFor(1))
        assertEquals(120, MotionKind.STEPS.targetFor(5))
        assertEquals(5, MotionKind.STAND_UP.targetFor(1))
        assertEquals(40, MotionKind.STAND_UP.targetFor(5))
    }

    @Test
    fun `la tabla es creciente y cabe en los limites de cada reto`() {
        MotionKind.entries.forEach { kind ->
            val row = MotionTargets.BY_LEVEL.getValue(kind)
            assertEquals(5, row.size)
            assertEquals(row.sorted(), row)
            assertEquals(row.size, row.toSet().size)
            row.forEach { assertTrue("$kind $it", it in kind.min..kind.max) }
        }
    }

    @Test
    fun `un nivel fuera de rango se acota`() {
        assertEquals(MotionKind.STEPS.targetFor(1), MotionKind.STEPS.targetFor(-3))
        assertEquals(MotionKind.STEPS.targetFor(5), MotionKind.STEPS.targetFor(99))
    }

    @Test
    fun `el nivel decide el objetivo de cada reto y los params guardados se ignoran`() {
        val caps = Caps()
        val challenges = listOf(
            MotionKind.SHAKE to ShakeChallenge(caps),
            MotionKind.STEPS to StepsChallenge(caps),
            MotionKind.STAND_UP to StandUpChallenge(caps),
        )
        for ((kind, challenge) in challenges) {
            for (level in 1..5) {
                val legacy = challenge.generate(level, """{"target":7}""", Random(1)) as MotionInstance
                assertEquals(kind.targetFor(level), legacy.target)
                assertEquals(kind.targetFor(level), (challenge.generate(level, "", Random(1)) as MotionInstance).target)
            }
        }
    }

    private val adaptiveFive = object : DifficultyProvider {
        override fun levelFor(config: SessionConfig, context: ChallengeContext) = 5
    }

    @Test
    fun `la sesion aplica el nivel que decide el proveedor de dificultad`() {
        val registry = ChallengeRegistry(listOf(ShakeChallenge(Caps()), StepsChallenge(Caps()), StandUpChallenge(Caps())))
        val config = SessionConfig(
            1, 3, ChallengeMode.CHAINED,
            listOf(ChallengeSpec("shake"), ChallengeSpec("steps"), ChallengeSpec("standup")),
        )
        // El proveedor adaptativo sube a 5 aunque la alarma diga 1.
        val session = ChallengeSession("s", 1L, config, registry, adaptiveFive, { })
        val targets = mutableListOf<Int>()
        while (!session.state.value.passed) {
            val current = session.state.value.current as MotionInstance
            targets += current.target
            session.submit(ChallengeAnswer.MotionVerified(current.type))
        }
        assertEquals(listOf(60, 120, 40), targets)
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
        val instance = challenge.generate(3, "", Random(1))
        assertEquals(30, (instance as MotionInstance).target)
        assertTrue(instance.check(ChallengeAnswer.MotionVerified(ChallengeType("shake"))))
        assertFalse(instance.check(ChallengeAnswer.MotionVerified(ChallengeType("steps"))))
        assertFalse(instance.check(ChallengeAnswer.Text("15")))
    }
}
