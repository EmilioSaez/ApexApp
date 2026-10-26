package app.luxion.apexapp.domain.challenge

import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reto de pruebas: la respuesta correcta es siempre su propio id de tipo. */
private class FakeChallenge(
    id: String,
    var available: Availability = Availability.Available,
) : Challenge {
    override val type = ChallengeType(id)
    val receivedParams = mutableListOf<String>()
    val receivedLevels = mutableListOf<Int>()

    override fun availability() = available

    override fun generate(level: Int, params: String, random: Random): ChallengeInstance {
        receivedParams += params
        receivedLevels += level
        val owner = type
        return object : ChallengeInstance {
            override val type = owner
            override val level = level
            override val prompt = owner.id
            override fun check(answer: ChallengeAnswer) = (answer as ChallengeAnswer.Text).value == owner.id
        }
    }
}

private class RecordingSink : ChallengeEventSink {
    val events = mutableListOf<ChallengeEvent>()
    override fun onEvent(event: ChallengeEvent) {
        events += event
    }
}

class ChallengeSessionTest {
    private val a = FakeChallenge("a")
    private val b = FakeChallenge("b")
    private val math = FakeChallenge(ChallengeType.MATH.id)
    private val sink = RecordingSink()

    private fun session(
        specs: List<ChallengeSpec>,
        count: Int,
        mode: ChallengeMode = ChallengeMode.CHAINED,
        difficulty: Int = 3,
        provider: DifficultyProvider = FixedDifficultyProvider(),
    ) = ChallengeSession(
        sessionId = "s1",
        alarmId = 7L,
        config = SessionConfig(difficulty, count, mode, specs),
        registry = ChallengeRegistry(listOf(a, b, math)),
        difficultyProvider = provider,
        eventSink = sink,
        random = Random(1),
        clock = { 1000L },
    )

    private fun ChallengeSession.solveCurrent() =
        submit(ChallengeAnswer.Text(state.value.current!!.type.id))

    @Test
    fun `chained mode cycles through types in order`() {
        val s = session(listOf(ChallengeSpec("a"), ChallengeSpec("b")), count = 3)
        val order = mutableListOf<String>()
        while (!s.state.value.passed) {
            order += s.state.value.current!!.type.id
            s.solveCurrent()
        }
        assertEquals(listOf("a", "b", "a"), order)
    }

    @Test
    fun `random mode only picks enabled types`() {
        val s = session(listOf(ChallengeSpec("a"), ChallengeSpec("b")), count = 10, mode = ChallengeMode.RANDOM)
        val seen = mutableSetOf<String>()
        while (!s.state.value.passed) {
            seen += s.state.value.current!!.type.id
            s.solveCurrent()
        }
        assertTrue(seen.all { it in setOf("a", "b") })
    }

    @Test
    fun `wrong answer does not advance and regenerates the same type`() {
        val s = session(listOf(ChallengeSpec("a")), count = 2)
        s.submit(ChallengeAnswer.Text("nope"))
        assertEquals(0, s.state.value.solved)
        assertTrue(s.state.value.lastAnswerWrong)
        assertEquals("a", s.state.value.current!!.type.id)
        assertEquals(2, a.receivedLevels.size)
    }

    @Test
    fun `passed only after the last challenge`() {
        val s = session(listOf(ChallengeSpec("a")), count = 3)
        s.solveCurrent()
        s.solveCurrent()
        assertFalse(s.state.value.passed)
        assertEquals(2, s.state.value.solved)
        s.solveCurrent()
        assertTrue(s.state.value.passed)
        assertNull(s.state.value.current)
    }

    @Test
    fun `unknown types are ignored and math is the default`() {
        val s = session(listOf(ChallengeSpec("ghost")), count = 1)
        assertEquals(ChallengeType.MATH, s.state.value.current!!.type)
        val s2 = session(listOf(ChallengeSpec("ghost"), ChallengeSpec("b")), count = 2)
        assertEquals("b", s2.state.value.current!!.type.id)
    }

    @Test
    fun `params only reach their own type`() {
        val s = session(listOf(ChallengeSpec("a", """{"x":1}"""), ChallengeSpec("b", """{"y":2}""")), count = 2)
        s.solveCurrent()
        assertEquals(listOf("""{"x":1}"""), a.receivedParams)
        assertEquals(listOf("""{"y":2}"""), b.receivedParams)
    }

    @Test
    fun `events carry the received session id in order`() {
        val s = session(listOf(ChallengeSpec("a")), count = 1)
        s.submit(ChallengeAnswer.Text("nope"))
        s.solveCurrent()
        assertEquals(
            listOf(
                ChallengeEvent.SessionStarted::class,
                ChallengeEvent.ChallengeStarted::class,
                ChallengeEvent.ChallengeFailed::class,
                ChallengeEvent.ChallengeSolved::class,
                ChallengeEvent.SessionFinished::class,
            ),
            sink.events.map { it::class },
        )
        assertTrue(sink.events.all { it.sessionId == "s1" && it.alarmId == 7L })
    }

    @Test
    fun `unavailable challenge falls back to another enabled type`() {
        a.available = Availability.Unavailable("no camera")
        val s = session(listOf(ChallengeSpec("a"), ChallengeSpec("b")), count = 1)
        assertEquals("b", s.state.value.current!!.type.id)
        val fell = sink.events.filterIsInstance<ChallengeEvent.ChallengeFellBack>().single()
        assertEquals(ChallengeType("a"), fell.from)
        assertEquals(ChallengeType("b"), fell.to)
    }

    @Test
    fun `unavailable challenge without alternative falls back to math`() {
        a.available = Availability.Unavailable("no camera")
        val s = session(listOf(ChallengeSpec("a")), count = 1)
        assertEquals(ChallengeType.MATH, s.state.value.current!!.type)
        assertNotNull(sink.events.filterIsInstance<ChallengeEvent.ChallengeFellBack>().singleOrNull())
    }

    @Test
    fun `challenge that becomes impossible mid session is replaced`() {
        val s = session(listOf(ChallengeSpec("a")), count = 1)
        a.available = Availability.Unavailable("permission revoked")
        s.reportUnavailable("permission revoked")
        assertEquals(ChallengeType.MATH, s.state.value.current!!.type)
        assertEquals(1, sink.events.filterIsInstance<ChallengeEvent.ChallengeFellBack>().size)
    }

    @Test
    fun `difficulty provider decides the level and is clamped`() {
        val provider = object : DifficultyProvider {
            override fun levelFor(config: SessionConfig, context: ChallengeContext) = 99
        }
        val s = session(listOf(ChallengeSpec("a")), count = 1, provider = provider)
        assertEquals(5, s.state.value.current!!.level)
    }

    @Test
    fun `default provider returns the base level`() {
        val s = session(listOf(ChallengeSpec("a")), count = 1, difficulty = 2)
        assertEquals(2, s.state.value.current!!.level)
    }

    @Test
    fun `count lower than the chosen types is raised to one of each`() {
        val s = session(listOf(ChallengeSpec("a"), ChallengeSpec("b")), count = 1)
        assertEquals(2, s.state.value.total)
        val order = mutableListOf<String>()
        while (!s.state.value.passed) {
            order += s.state.value.current!!.type.id
            s.solveCurrent()
        }
        assertEquals(listOf("a", "b"), order)
    }

    @Test
    fun `random mode does one of each type before repeating`() {
        val s = session(listOf(ChallengeSpec("a"), ChallengeSpec("b")), count = 2, mode = ChallengeMode.RANDOM)
        val order = mutableListOf<String>()
        while (!s.state.value.passed) {
            order += s.state.value.current!!.type.id
            s.solveCurrent()
        }
        assertEquals(setOf("a", "b"), order.toSet())
    }
}
