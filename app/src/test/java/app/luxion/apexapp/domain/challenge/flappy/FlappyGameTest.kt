package app.luxion.apexapp.domain.challenge.flappy

import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlappyGameTest {
    private val dt = 1f / 60f

    /** Salta cuando el pájaro queda por debajo del centro del próximo hueco. */
    private fun FlappyGame.autopilot(seconds: Float) {
        var t = 0f
        while (t < seconds && phase != FlappyPhase.WON && phase != FlappyPhase.CRASHED) {
            if (phase == FlappyPhase.READY || (velocity > 0f && offsetToNextGap() > BOT_MARGIN)) flap()
            step(dt)
            t += dt
        }
    }

    @Test
    fun `waits for the first tap and then starts`() {
        val game = FlappyGame(target = 3, random = Random(1))
        repeat(120) { game.step(dt) }
        assertEquals(FlappyPhase.READY, game.phase)
        assertEquals(0, game.score)
        game.flap()
        assertEquals(FlappyPhase.PLAYING, game.phase)
    }

    @Test
    fun `falling to the floor crashes`() {
        val game = FlappyGame(target = 3, random = Random(1))
        game.flap()
        repeat(240) { game.step(dt) }
        assertEquals(FlappyPhase.CRASHED, game.phase)
    }

    @Test
    fun `an autopilot reaching the target wins`() {
        listOf(1, 2, 3, 4, 5).forEach { seed ->
            val game = FlappyGame(target = 8, random = Random(seed))
            game.autopilot(seconds = 60f)
            assertEquals("seed $seed", FlappyPhase.WON, game.phase)
            assertEquals(8, game.score)
        }
    }

    @Test
    fun `crashing and restarting resets the score`() {
        val game = FlappyGame(target = 20, random = Random(2))
        // Se supera alguna tubería y luego se deja caer.
        var t = 0f
        game.flap()
        while (game.score < 1 && game.phase == FlappyPhase.PLAYING && t < 30f) {
            if (game.velocity > 0f && game.offsetToNextGap() > BOT_MARGIN) game.flap()
            game.step(dt)
            t += dt
        }
        assertTrue(game.score >= 1)
        while (game.phase == FlappyPhase.PLAYING) game.step(dt)
        assertEquals(FlappyPhase.CRASHED, game.phase)

        // Durante el bloqueo inicial un toque no reinicia.
        game.flap()
        assertEquals(FlappyPhase.CRASHED, game.phase)
        repeat(60) { game.step(dt) }
        game.flap()
        assertEquals(FlappyPhase.READY, game.phase)
        assertEquals(0, game.score)
        assertTrue(game.pipes.isEmpty())
    }

    @Test
    fun `gaps of consecutive pipes stay within reach`() {
        val game = FlappyGame(target = 50, random = Random(7))
        game.autopilot(seconds = 90f)
        val centers = game.pipes.map { it.gapCenter }
        centers.zipWithNext().forEach { (a, b) -> assertTrue(kotlin.math.abs(a - b) <= 110.001f) }
        centers.forEach {
            assertTrue(it - FlappyGame.GAP / 2 >= 0f && it + FlappyGame.GAP / 2 <= FlappyGame.HEIGHT)
        }
    }

    @Test
    fun `targets grow with difficulty and level is clamped`() {
        assertEquals(FlappyTargets.BY_LEVEL.sorted(), FlappyTargets.BY_LEVEL)
        assertEquals(FlappyTargets.BY_LEVEL.first(), FlappyTargets.targetFor(0))
        assertEquals(FlappyTargets.BY_LEVEL.last(), FlappyTargets.targetFor(99))
        assertEquals(5, FlappyTargets.BY_LEVEL.size)
    }

    @Test
    fun `the instance is solved only by a completed game of its type`() {
        val instance = FlappyChallenge().generate(3, "{}", Random(1)) as FlappyInstance
        assertEquals(FlappyTargets.targetFor(3), instance.target)
        assertTrue(instance.check(ChallengeAnswer.GameCompleted(FlappyChallenge.TYPE)))
        assertFalse(instance.check(ChallengeAnswer.GameCompleted(ChallengeType("other"))))
        assertFalse(instance.check(ChallengeAnswer.Text("1")))
    }

    private companion object {
        const val BOT_MARGIN = 12f
    }
}
