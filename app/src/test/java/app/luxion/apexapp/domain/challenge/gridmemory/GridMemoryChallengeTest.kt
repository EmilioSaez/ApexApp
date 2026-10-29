package app.luxion.apexapp.domain.challenge.gridmemory

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

class GridMemoryChallengeTest {
    private val challenge = GridMemoryChallenge()

    private fun generate(level: Int, params: String = "", seed: Int = 1) =
        challenge.generate(level, params, Random(seed)) as GridMemoryInstance

    @Test
    fun `las tablas son crecientes y el nivel 5 es mayor que el 1`() {
        assertEquals(5, GridMemoryLevels.SIDE_BY_LEVEL.size)
        assertEquals(5, GridMemoryLevels.COUNT_BY_LEVEL.size)
        assertEquals(5, GridMemoryLevels.SHOW_MS_BY_LEVEL.size)
        assertEquals(GridMemoryLevels.SIDE_BY_LEVEL.sorted(), GridMemoryLevels.SIDE_BY_LEVEL)
        assertEquals(GridMemoryLevels.COUNT_BY_LEVEL.sorted(), GridMemoryLevels.COUNT_BY_LEVEL)
        assertEquals(GridMemoryLevels.SHOW_MS_BY_LEVEL.sortedDescending(), GridMemoryLevels.SHOW_MS_BY_LEVEL)
        assertEquals(3, generate(1).side)
        assertEquals(6, generate(5).side)
        assertTrue(generate(5).cells.size > generate(1).cells.size)
    }

    @Test
    fun `siempre hay menos casillas a recordar que casillas en el tablero`() {
        for (level in 1..5) {
            val instance = generate(level)
            assertTrue(instance.cells.size < instance.side * instance.side)
            assertEquals(GridMemoryLevels.countFor(level), instance.cells.size)
            assertTrue(instance.cells.all { it in 0 until instance.side * instance.side })
        }
    }

    @Test
    fun `un nivel fuera de rango se acota`() {
        assertEquals(generate(1).side, generate(-3).side)
        assertEquals(generate(5).cells.size, generate(99).cells.size)
        assertEquals(5, generate(99).level)
        assertEquals(1, generate(0).level)
    }

    @Test
    fun `los params guardados se ignoran`() {
        for (level in 1..5) {
            val legacy = generate(level, """{"count":2,"side":9}""")
            assertEquals(generate(level).cells, legacy.cells)
            assertEquals(GridMemoryLevels.sideFor(level), legacy.side)
        }
    }

    @Test
    fun `la misma semilla da el mismo conjunto y semillas distintas varian`() {
        assertEquals(generate(4, seed = 9).cells, generate(4, seed = 9).cells)
        val sets = (1..8).map { generate(4, seed = it).cells }.toSet()
        assertTrue(sets.size > 1)
        assertEquals(Availability.Available, challenge.availability())
    }

    @Test
    fun `el orden de los toques no importa pero el conjunto debe ser exacto`() {
        val instance = generate(3)
        val cells = instance.cells.toList()
        val extra = (0 until instance.side * instance.side).first { it !in instance.cells }
        assertTrue(instance.check(ChallengeAnswer.Pattern(instance.type, cells)))
        assertTrue(instance.check(ChallengeAnswer.Pattern(instance.type, cells.reversed())))
        assertFalse(instance.check(ChallengeAnswer.Pattern(instance.type, cells.dropLast(1))))
        assertFalse(instance.check(ChallengeAnswer.Pattern(instance.type, cells + extra)))
        assertFalse(instance.check(ChallengeAnswer.Pattern(instance.type, cells.dropLast(1) + extra)))
        assertFalse(instance.check(ChallengeAnswer.Pattern(instance.type, cells.dropLast(1) + cells.first())))
        assertFalse(instance.check(ChallengeAnswer.Pattern(ChallengeType.MATH, cells)))
        assertFalse(instance.check(ChallengeAnswer.Text("1")))
        assertTrue(instance.isTarget(cells.first()))
        assertFalse(instance.isTarget(extra))
    }

    @Test
    fun `un fallo en la sesion conserva la misma instancia y cuenta como fallo`() {
        val events = mutableListOf<ChallengeEvent>()
        val registry = ChallengeRegistry(listOf(challenge, MathChallenge()))
        val config = SessionConfig(1, 1, ChallengeMode.CHAINED, listOf(ChallengeSpec("grid_memory")))
        val provider = object : DifficultyProvider {
            override fun levelFor(config: SessionConfig, context: ChallengeContext) = 3
        }
        val session = ChallengeSession("s", 1L, config, registry, provider, { events += it }, Random(5))
        val first = session.state.value.current as GridMemoryInstance

        val wrong = (0 until first.side * first.side).first { it !in first.cells }
        session.submit(ChallengeAnswer.Pattern(first.type, listOf(wrong)))

        val after = session.state.value
        assertSame(first, after.current)
        assertTrue(after.lastAnswerWrong)
        assertEquals(1, events.count { it is ChallengeEvent.ChallengeFailed })

        session.submit(ChallengeAnswer.Pattern(first.type, first.cells.toList()))
        assertTrue(session.state.value.passed)
    }

    @Test
    fun `una sesion con el reto esta en el registro y se resuelve`() {
        val registry = ChallengeRegistry(listOf(challenge, MathChallenge()))
        assertTrue(ChallengeType("grid_memory") in registry)
        val config = SessionConfig(2, 2, ChallengeMode.CHAINED, listOf(ChallengeSpec("grid_memory")))
        val provider = object : DifficultyProvider {
            override fun levelFor(config: SessionConfig, context: ChallengeContext) = 2
        }
        val session = ChallengeSession("s", 1L, config, registry, provider, { }, Random(3))
        var solved = 0
        while (!session.state.value.passed) {
            val current = session.state.value.current as GridMemoryInstance
            solved++
            session.submit(ChallengeAnswer.Pattern(current.type, current.cells.toList()))
        }
        assertTrue(solved > 0)
    }
}
