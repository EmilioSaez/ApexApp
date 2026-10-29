package app.luxion.apexapp.domain.challenge.shapes

import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.challenge.Availability
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeContext
import app.luxion.apexapp.domain.challenge.ChallengeEvent
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.ChallengeSession
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.DifficultyProvider
import app.luxion.apexapp.domain.challenge.MathChallenge
import app.luxion.apexapp.domain.challenge.SessionConfig
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeSequenceChallengeTest {
    private val challenge = ShapeSequenceChallenge()

    private fun generate(level: Int, params: String = "", seed: Int = 1) =
        challenge.generate(level, params, Random(seed)) as ShapeSequenceInstance

    private fun answer(instance: ShapeSequenceInstance, shapes: List<ShapeKind>) =
        ChallengeAnswer.Pattern(instance.type, shapes.map { it.ordinal })

    @Test
    fun `las tablas son crecientes y el nivel 5 es mas exigente que el 1`() {
        assertEquals(5, ShapeSequenceLevels.LENGTH_BY_LEVEL.size)
        assertEquals(5, ShapeSequenceLevels.DISTINCT_BY_LEVEL.size)
        assertEquals(5, ShapeSequenceLevels.SHAPE_MS_BY_LEVEL.size)
        assertEquals(ShapeSequenceLevels.LENGTH_BY_LEVEL.sorted(), ShapeSequenceLevels.LENGTH_BY_LEVEL)
        assertEquals(ShapeSequenceLevels.DISTINCT_BY_LEVEL.sorted(), ShapeSequenceLevels.DISTINCT_BY_LEVEL)
        assertEquals(ShapeSequenceLevels.SHAPE_MS_BY_LEVEL.sortedDescending(), ShapeSequenceLevels.SHAPE_MS_BY_LEVEL)
        assertTrue(generate(5).sequence.size > generate(1).sequence.size)
        assertEquals(3, generate(1).sequence.size)
        assertEquals(7, generate(5).sequence.size)
        assertTrue(ShapeSequenceLevels.DISTINCT_BY_LEVEL.all { it in 2..ShapeKind.entries.size })
    }

    @Test
    fun `un nivel fuera de rango se acota`() {
        assertEquals(generate(1).sequence.size, generate(-3).sequence.size)
        assertEquals(generate(5).sequence.size, generate(99).sequence.size)
        assertEquals(5, generate(99).level)
        assertEquals(1, generate(0).level)
    }

    @Test
    fun `los params guardados se ignoran y la secuencia usa solo la paleta`() {
        for (level in 1..5) {
            val legacy = generate(level, """{"length":2,"shapes":9}""")
            assertEquals(generate(level).sequence, legacy.sequence)
            assertEquals(ShapeSequenceLevels.lengthFor(level), legacy.sequence.size)
            assertEquals(ShapeSequenceLevels.distinctFor(level), legacy.palette.size)
            assertEquals(legacy.palette.distinct(), legacy.palette)
            assertTrue(legacy.sequence.all { it in legacy.palette })
        }
    }

    @Test
    fun `cada instancia genera una secuencia propia y siempre esta disponible`() {
        val random = Random(7)
        val sequences = List(5) { (challenge.generate(5, "", random) as ShapeSequenceInstance).sequence }
        assertTrue(sequences.toSet().size > 1)
        assertEquals(Availability.Available, challenge.availability())
    }

    @Test
    fun `solo el orden exacto de su tipo supera la instancia`() {
        val instance = generate(2)
        assertTrue(instance.check(answer(instance, instance.sequence)))
        val shifted = instance.sequence.drop(1) + instance.sequence.first()
        if (shifted != instance.sequence) assertFalse(instance.check(answer(instance, shifted)))
        assertFalse(instance.check(answer(instance, instance.sequence.dropLast(1))))
        assertFalse(instance.check(answer(instance, instance.sequence + instance.sequence.first())))
        assertFalse(instance.check(ChallengeAnswer.Pattern(ChallengeType.MATH, instance.sequence.map { it.ordinal })))
        assertFalse(instance.check(ChallengeAnswer.Text("1")))
    }

    @Test
    fun `un fallo en la sesion conserva la misma instancia y cuenta como fallo`() {
        val events = mutableListOf<ChallengeEvent>()
        val registry = ChallengeRegistry(listOf(challenge, MathChallenge()))
        val config = SessionConfig(1, 1, ChallengeMode.CHAINED, listOf(ChallengeSpec("shapes")))
        val provider = object : DifficultyProvider {
            override fun levelFor(config: SessionConfig, context: ChallengeContext) = 3
        }
        val session = ChallengeSession("s", 1L, config, registry, provider, { events += it }, Random(5))
        val first = session.state.value.current as ShapeSequenceInstance

        session.submit(answer(first, first.sequence.reversed() + first.sequence.first()))

        val after = session.state.value
        assertSame(first, after.current)
        assertTrue(after.lastAnswerWrong)
        assertEquals(1, events.count { it is ChallengeEvent.ChallengeFailed })

        session.submit(answer(first, first.sequence))
        assertTrue(session.state.value.passed)
    }

    @Test
    fun `una sesion con formas esta en el registro y termina`() {
        val registry = ChallengeRegistry(listOf(challenge, MathChallenge()))
        assertTrue(ChallengeType("shapes") in registry)
        val config = SessionConfig(2, 3, ChallengeMode.CHAINED, listOf(ChallengeSpec("shapes")))
        val provider = object : DifficultyProvider {
            override fun levelFor(config: SessionConfig, context: ChallengeContext) = 2
        }
        val session = ChallengeSession("s", 1L, config, registry, provider, { }, Random(3))
        var solved = 0
        while (!session.state.value.passed) {
            val current = session.state.value.current as ShapeSequenceInstance
            solved++
            session.submit(answer(current, current.sequence))
        }
        assertTrue(solved > 0)
    }
}
