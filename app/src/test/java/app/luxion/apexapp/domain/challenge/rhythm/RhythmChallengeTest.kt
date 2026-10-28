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
    fun `la dificultad cambia la velocidad y la duracion de la ronda unica`() {
        val easy = challenge.generate(1, "", Random(1)) as RhythmInstance
        val hard = challenge.generate(5, "", Random(1)) as RhythmInstance
        assertTrue(hard.pace.bpm > easy.pace.bpm)
        assertTrue(hard.pace.roundMs > easy.pace.roundMs)
    }

    @Test
    fun `los params antiguos con rondas se ignoran`() {
        val old = challenge.generate(3, """{"rounds":5}""", Random(1)) as RhythmInstance
        assertEquals(RhythmTuning.pace(3), old.pace)
        assertEquals("rhythm", old.prompt)
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
