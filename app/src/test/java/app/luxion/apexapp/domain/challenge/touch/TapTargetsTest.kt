package app.luxion.apexapp.domain.challenge.touch

import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeType
import kotlin.math.hypot
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TapTargetsTest {
    private fun game(rounds: Int = 5, level: Int = 3, seed: Int = 1) = TapTargetsGame(rounds, level, Random(seed))

    private fun clear(game: TapTargetsGame, round: TapRound): TapResult =
        round.circles.map { game.tap(it.xDp, it.yDp) }.last()

    @Test
    fun params_defaultAndClamp() {
        assertEquals(8, TapTargetsParams.rounds("{}"))
        assertEquals(8, TapTargetsParams.rounds("no json"))
        assertEquals(12, TapTargetsParams.rounds("""{"rounds":12}"""))
        assertEquals(15, TapTargetsParams.rounds("""{"rounds":40}"""))
        assertEquals(3, TapTargetsParams.rounds("""{"rounds":-2}"""))
        assertEquals(10, TapTargetsParams.rounds(TapTargetsParams.paramsFor(10)))
    }

    @Test
    fun tuning_level5IsHarderThanLevel1() {
        val easy = TapTargetsTuning.forLevel(1)
        val hard = TapTargetsTuning.forLevel(5)
        assertTrue(hard.circles > easy.circles)
        assertTrue(hard.diameterDp < easy.diameterDp)
        assertTrue(hard.timeLimitMs < easy.timeLimitMs)
        assertEquals(hard, TapTargetsTuning.forLevel(9))
        assertEquals(easy, TapTargetsTuning.forLevel(-3))
    }

    @Test
    fun round_hasLevelCirclesInsideAreaWithoutOverlap() {
        repeat(50) { seed ->
            val level = seed % 5 + 1
            val round = game(level = level, seed = seed).nextRound(360f, 520f)
            val tuning = TapTargetsTuning.forLevel(level)
            assertEquals(tuning.circles, round.circles.size)
            val r = tuning.diameterDp / 2f
            round.circles.forEach {
                assertTrue(it.xDp in r..(360f - r) && it.yDp in r..(520f - r))
            }
            round.circles.forEachIndexed { i, a ->
                round.circles.drop(i + 1).forEach { b ->
                    assertTrue(hypot(a.xDp - b.xDp, a.yDp - b.yDp) >= tuning.diameterDp)
                }
            }
            assertTrue(round.gapMs in TapTargetsTuning.GAP_MIN_MS..TapTargetsTuning.GAP_MAX_MS)
        }
    }

    @Test
    fun round_positionsDifferBetweenRoundsAndSessions() {
        val g = game(seed = 1)
        val a = g.nextRound(360f, 520f)
        val b = g.nextRound(360f, 520f)
        val other = game(seed = 2).nextRound(360f, 520f)
        assertNotEquals(a.circles, b.circles)
        assertNotEquals(a.circles, other.circles)
    }

    @Test
    fun tinyArea_doesNotHang() {
        val round = game(level = 5).nextRound(40f, 40f)
        assertEquals(5, round.circles.size)
    }

    @Test
    fun tap_missIsIgnoredAndRoundContinues() {
        val g = game()
        val round = g.nextRound(360f, 520f)
        assertEquals(TapResult.MISS, g.tap(-500f, -500f))
        assertEquals(TapResult.ROUND_CLEARED, clear(g, round))
        assertEquals(1, g.solved)
    }

    @Test
    fun tap_sameCircleTwiceDoesNotClearRound() {
        val g = game(level = 3)
        val round = g.nextRound(360f, 520f)
        val first = round.circles.first()
        assertEquals(TapResult.HIT, g.tap(first.xDp, first.yDp))
        assertEquals(TapResult.MISS, g.tap(first.xDp, first.yDp))
        assertEquals(0, g.solved)
    }

    @Test
    fun failure_addsExtraRoundAndKeepsProgress() {
        val g = game(rounds = 8)
        repeat(3) { clear(g, g.nextRound(360f, 520f)) }
        g.nextRound(360f, 520f)
        g.expire()
        assertEquals(3, g.solved)
        assertEquals(9, g.total)
        assertEquals(6, g.total - g.solved)
    }

    @Test
    fun failure_extrasAreCappedAtConfiguredRounds() {
        val g = game(rounds = 3)
        repeat(10) {
            g.nextRound(360f, 520f)
            g.expire()
        }
        assertEquals(3, g.extras)
        assertEquals(6, g.total)
        assertFalse(g.done)
    }

    @Test
    fun game_isDoneWhenAllPendingRoundsAreCleared() {
        val g = game(rounds = 3)
        g.nextRound(360f, 520f)
        g.expire()
        var rounds = 0
        while (!g.done) {
            clear(g, g.nextRound(360f, 520f))
            rounds++
        }
        assertEquals(4, rounds)
        assertEquals(4, g.solved)
    }

    @Test
    fun expire_withoutActiveRoundDoesNothing() {
        val g = game(rounds = 3)
        g.expire()
        assertEquals(0, g.extras)
    }

    @Test
    fun challenge_generatesInstanceAccordingToParams() {
        val instance = TapTargetsChallenge().generate(4, """{"rounds":11}""", Random(1)) as TapTargetsInstance
        assertEquals(11, instance.rounds)
        assertEquals(4, instance.level)
        assertEquals(TapTargetsChallenge.TYPE, instance.type)
    }

    @Test
    fun check_acceptsOnlyGameVerifiedOfItsType() {
        val instance = TapTargetsChallenge().generate(3, "{}", Random(1))
        assertTrue(instance.check(ChallengeAnswer.GameVerified(TapTargetsChallenge.TYPE)))
        assertFalse(instance.check(ChallengeAnswer.GameVerified(ChallengeType("rhythm"))))
        assertFalse(instance.check(ChallengeAnswer.Text("1")))
    }
}
