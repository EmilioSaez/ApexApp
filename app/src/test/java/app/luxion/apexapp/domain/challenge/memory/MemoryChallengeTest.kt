package app.luxion.apexapp.domain.challenge.memory

import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.challenge.Availability
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.Challenge
import app.luxion.apexapp.domain.challenge.ChallengeContext
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.ChallengeEvent
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.ChallengeSession
import app.luxion.apexapp.domain.challenge.DifficultyProvider
import app.luxion.apexapp.domain.challenge.MathChallenge
import app.luxion.apexapp.domain.challenge.SessionConfig
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

private object OkChallenge : Challenge {
    override val type = ChallengeType("ok")

    override fun generate(level: Int, params: String, random: Random): ChallengeInstance =
        object : ChallengeInstance {
            override val type = OkChallenge.type
            override val level = level
            override val prompt = "ok"
            override fun check(answer: ChallengeAnswer) = answer == ChallengeAnswer.Text("ok")
        }
}

class MemoryChallengeTest {
    private val challenge = MemoryChallenge()

    private fun generate(level: Int, params: String = "", seed: Int = 1) =
        challenge.generate(level, params, Random(seed)) as MemoryInstance

    @Test
    fun `la tabla es creciente y el nivel 5 es mas largo que el 1`() {
        assertEquals(5, MemoryLevels.LENGTH_BY_LEVEL.size)
        assertEquals(MemoryLevels.LENGTH_BY_LEVEL.sorted(), MemoryLevels.LENGTH_BY_LEVEL)
        assertEquals(MemoryLevels.STEP_MS_BY_LEVEL.sortedDescending(), MemoryLevels.STEP_MS_BY_LEVEL)
        assertTrue(generate(5).sequence.size > generate(1).sequence.size)
        assertEquals(3, generate(1).sequence.size)
        assertEquals(7, generate(5).sequence.size)
    }

    @Test
    fun `un nivel fuera de rango se acota`() {
        assertEquals(generate(1).sequence.size, generate(-3).sequence.size)
        assertEquals(generate(5).sequence.size, generate(99).sequence.size)
        assertEquals(5, generate(99).level)
    }

    @Test
    fun `los params guardados se ignoran y las casillas caben en la malla`() {
        for (level in 1..5) {
            val legacy = generate(level, """{"length":2}""")
            assertEquals(MemoryLevels.lengthFor(level), legacy.sequence.size)
            assertEquals(generate(level).sequence, legacy.sequence)
            assertTrue(legacy.sequence.all { it in 0 until MemoryLevels.GRID_SIZE })
        }
    }

    @Test
    fun `cada instancia genera una secuencia propia y siempre esta disponible`() {
        val random = Random(7)
        val sequences = List(5) { (challenge.generate(5, "", random) as MemoryInstance).sequence }
        assertTrue(sequences.toSet().size > 1)
        assertEquals(Availability.Available, challenge.availability())
    }

    @Test
    fun `solo el patron exacto de su tipo supera la instancia`() {
        val instance = generate(2)
        assertTrue(instance.check(ChallengeAnswer.Pattern(instance.type, instance.sequence)))
        assertFalse(instance.check(ChallengeAnswer.Pattern(instance.type, instance.sequence.reversed().takeIf { it != instance.sequence } ?: emptyList())))
        assertFalse(instance.check(ChallengeAnswer.Pattern(instance.type, instance.sequence.dropLast(1))))
        assertFalse(instance.check(ChallengeAnswer.Pattern(instance.type, instance.sequence + 0)))
        assertFalse(instance.check(ChallengeAnswer.Pattern(ChallengeType.MATH, instance.sequence)))
        assertFalse(instance.check(ChallengeAnswer.Text("1")))
    }

    @Test
    fun `un fallo en la sesion conserva la misma instancia y cuenta como fallo`() {
        val events = mutableListOf<ChallengeEvent>()
        val registry = ChallengeRegistry(listOf(challenge, MathChallenge()))
        val config = SessionConfig(1, 1, ChallengeMode.CHAINED, listOf(ChallengeSpec("memory")))
        val provider = object : DifficultyProvider {
            override fun levelFor(config: SessionConfig, context: ChallengeContext) = 3
        }
        val session = ChallengeSession("s", 1L, config, registry, provider, { events += it }, Random(5))
        val first = session.state.value.current as MemoryInstance

        session.submit(ChallengeAnswer.Pattern(first.type, first.sequence.reversed() + 8))

        val after = session.state.value
        assertSame(first, after.current)
        assertTrue(after.lastAnswerWrong)
        assertEquals(1, events.count { it is ChallengeEvent.ChallengeFailed })

        session.submit(ChallengeAnswer.Pattern(first.type, first.sequence))
        assertTrue(session.state.value.passed)
    }

    @Test
    fun `los retos de otros tipos siguen regenerandose tras un fallo`() {
        val registry = ChallengeRegistry(listOf(MathChallenge()))
        val config = SessionConfig(1, 1, ChallengeMode.CHAINED, listOf(ChallengeSpec("math")))
        val provider = object : DifficultyProvider {
            override fun levelFor(config: SessionConfig, context: ChallengeContext) = 1
        }
        val session = ChallengeSession("s", 1L, config, registry, provider, { }, Random(5))
        val first = session.state.value.current!!
        session.submit(ChallengeAnswer.Text("not-a-number"))
        assertNotEquals(first, session.state.value.current)
    }

    @Test
    fun `una sesion mixta con memoria esta en el registro y termina`() {
        val registry = ChallengeRegistry(listOf(challenge, MathChallenge(), OkChallenge))
        assertTrue(ChallengeType("memory") in registry)
        val config = SessionConfig(
            2, 3, ChallengeMode.CHAINED,
            listOf(ChallengeSpec("ok"), ChallengeSpec("memory")),
        )
        val provider = object : DifficultyProvider {
            override fun levelFor(config: SessionConfig, context: ChallengeContext) = 2
        }
        val session = ChallengeSession("s", 1L, config, registry, provider, { }, Random(3))
        var memorySolved = 0
        while (!session.state.value.passed) {
            val current = session.state.value.current!!
            if (current is MemoryInstance) {
                memorySolved++
                session.submit(ChallengeAnswer.Pattern(current.type, current.sequence))
            } else {
                session.submit(ChallengeAnswer.Text("ok"))
            }
        }
        assertTrue(memorySolved > 0)
    }
}
