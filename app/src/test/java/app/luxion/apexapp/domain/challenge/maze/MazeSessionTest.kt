package app.luxion.apexapp.domain.challenge.maze

import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.challenge.ChallengeContext
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.ChallengeSession
import app.luxion.apexapp.domain.challenge.DifficultyProvider
import app.luxion.apexapp.domain.challenge.MathChallenge
import app.luxion.apexapp.domain.challenge.MazeTrace
import app.luxion.apexapp.domain.challenge.SessionConfig
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MazeSessionTest {
    private val registry = ChallengeRegistry(listOf(MathChallenge(), MazeChallenge()))
    private val provider = object : DifficultyProvider {
        override fun levelFor(config: SessionConfig, context: ChallengeContext) = 3
    }

    private fun session() = ChallengeSession(
        "s", 1L,
        SessionConfig(1, 1, ChallengeMode.CHAINED, listOf(ChallengeSpec("maze"))),
        registry, provider, { }, Random(5),
    )

    @Test
    fun registryHasMaze() {
        assertTrue(MazeChallenge.TYPE in registry)
    }

    @Test
    fun wallHitKeepsMazeAndSolutionPasses() {
        val session = session()
        val instance = session.state.value.current as MazeInstance
        session.submit(MazeTrace(MazeChallenge.TYPE, listOf(instance.maze.start)))
        assertTrue(session.state.value.lastAnswerWrong)
        assertSame(instance, session.state.value.current)
        session.submit(MazeTrace(MazeChallenge.TYPE, instance.maze.solution))
        assertTrue(session.state.value.passed)
        assertEquals(1, session.state.value.solved)
    }
}
