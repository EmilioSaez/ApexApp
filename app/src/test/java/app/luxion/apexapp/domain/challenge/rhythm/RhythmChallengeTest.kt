package app.luxion.apexapp.domain.challenge.rhythm

import app.luxion.apexapp.domain.challenge.Availability
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RhythmChallengeTest {
    private val challenge = RhythmChallenge()

    @Test
    fun `siempre esta disponible`() {
        assertEquals(Availability.Available, challenge.availability())
    }

    @Test
    fun `las rondas salen de los params y no de la dificultad`() {
        val params = RhythmParams.paramsFor(6)
        assertEquals(6, (challenge.generate(1, params, Random(1)) as RhythmInstance).rounds)
        assertEquals(6, (challenge.generate(5, params, Random(1)) as RhythmInstance).rounds)
        assertEquals(RhythmParams.DEFAULT_ROUNDS, (challenge.generate(3, "", Random(1)) as RhythmInstance).rounds)
    }

    @Test
    fun `cada sesion lleva una semilla distinta`() {
        val a = challenge.generate(3, "", Random(1)) as RhythmInstance
        val b = challenge.generate(3, "", Random(2)) as RhythmInstance
        assertNotEquals(a.seed, b.seed)
    }

    @Test
    fun `solo se supera con GameVerified de su tipo`() {
        val instance = challenge.generate(3, "", Random(1))
        assertTrue(instance.check(ChallengeAnswer.GameVerified(RhythmChallenge.TYPE)))
        assertFalse(instance.check(ChallengeAnswer.GameVerified(ChallengeType("other"))))
        assertFalse(instance.check(ChallengeAnswer.MotionVerified(RhythmChallenge.TYPE)))
        assertFalse(instance.check(ChallengeAnswer.Text("1")))
    }
}
