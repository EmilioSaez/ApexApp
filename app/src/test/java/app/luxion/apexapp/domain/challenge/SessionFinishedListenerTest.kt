package app.luxion.apexapp.domain.challenge

import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionFinishedListenerTest {
    private val calls = mutableListOf<String>()

    private val sink = ChallengeEventSink { event ->
        if (event is ChallengeEvent.SessionFinished) calls += "sink"
    }

    private fun factory(vararg listeners: SessionFinishedListener) = ChallengeSessionFactory(
        ChallengeRegistry(listOf(MathChallenge())),
        FixedDifficultyProvider(),
        sink,
        PassedAlarms(),
        listeners.toSet(),
    )

    private val config = SessionConfig(1, 1, ChallengeMode.CHAINED, listOf(ChallengeSpec("math")))

    private fun ChallengeSession.solve() {
        val prompt = state.value.current!!.prompt.split(" ")
        val result = if (prompt[1] == "+") prompt[0].toInt() + prompt[2].toInt() else prompt[0].toInt() - prompt[2].toInt()
        submit(ChallengeAnswer.Text(result.toString()))
    }

    @Test
    fun `listener runs after the sink with session and alarm ids`() {
        val received = mutableListOf<Pair<String, Long?>>()
        val listener = SessionFinishedListener { sessionId, alarmId ->
            calls += "listener"
            received += sessionId to alarmId
        }
        factory(listener).create(config, "s1", 9L).solve()
        assertEquals(listOf("sink", "listener"), calls)
        assertEquals(listOf("s1" to 9L), received)
    }

    @Test
    fun `a failing listener does not stop the others or the session`() {
        val failing = SessionFinishedListener { _, _ -> error("boom") }
        val ok = SessionFinishedListener { _, _ -> calls += "ok" }
        val session = factory(failing, ok).create(config, "s1", 1L)
        session.solve()
        assertTrue(session.state.value.passed)
        assertTrue("ok" in calls)
    }

    @Test
    fun `listeners are not notified when the session is not passed`() {
        val listener = SessionFinishedListener { _, _ -> calls += "listener" }
        val session = factory(listener).create(config, "s1", 1L)
        session.submit(ChallengeAnswer.Text("wrong"))
        assertTrue(calls.isEmpty())
    }
}
