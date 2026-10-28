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
    private fun game(level: Int = 3, seed: Int = 1) = TapTargetsGame(level, Random(seed))

    private fun clear(game: TapTargetsGame, round: TapRound): TapResult =
        round.circles.map { game.tap(it.xDp, it.yDp) }.last()

    @Test
    fun tuning_higherLevelIsFasterAndLongerOrEqual() {
        val easy = TapTargetsTuning.forLevel(1)
        val hard = TapTargetsTuning.forLevel(5)
        assertTrue(hard.timePerTargetMs < easy.timePerTargetMs)
        assertTrue(TapTargetsTuning.BY_LEVEL.zipWithNext().all { (a, b) -> b.timePerTargetMs < a.timePerTargetMs })
        assertTrue(TapTargetsTuning.BY_LEVEL.zipWithNext().all { (a, b) -> b.circles > a.circles })
        assertEquals(hard, TapTargetsTuning.forLevel(9))
        assertEquals(easy, TapTargetsTuning.forLevel(-3))
    }

    @Test
    fun round_matchesLevelTuning() {
        (1..5).forEach { level ->
            val tuning = TapTargetsTuning.forLevel(level)
            val round = game(level = level).nextRound(360f, 520f)
            assertEquals(tuning.circles, round.circles.size)
            assertEquals(tuning.circles * tuning.timePerTargetMs, round.timeLimitMs)
            assertEquals(TapTargetsTuning.DIAMETER_DP, round.diameterDp)
        }
    }

    @Test
    fun round_hasCirclesInsideAreaWithoutOverlap() {
        repeat(50) { seed ->
            val level = seed % 5 + 1
            val round = game(level = level, seed = seed).nextRound(360f, 520f)
            assertEquals(TapTargetsTuning.forLevel(level).circles, round.circles.size)
            val r = TapTargetsTuning.DIAMETER_DP / 2f
            round.circles.forEach {
                assertTrue(it.xDp in r..(360f - r) && it.yDp in r..(520f - r))
            }
            round.circles.forEachIndexed { i, a ->
                round.circles.drop(i + 1).forEach { b ->
                    assertTrue(hypot(a.xDp - b.xDp, a.yDp - b.yDp) >= TapTargetsTuning.DIAMETER_DP)
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
        assertEquals(TapTargetsTuning.forLevel(5).circles, round.circles.size)
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
    fun game_isDoneAfterClearingTheSingleRound() {
        val g = game()
        assertFalse(g.done)
        clear(g, g.nextRound(360f, 520f))
        assertTrue(g.done)
        assertEquals(1, g.solved)
    }

    @Test
    fun failure_repeatsTheSameRoundUntilCleared() {
        val g = game()
        repeat(10) {
            g.nextRound(360f, 520f)
            g.expire()
            assertFalse(g.done)
        }
        assertEquals(0, g.solved)
        clear(g, g.nextRound(360f, 520f))
        assertTrue(g.done)
    }

    @Test
    fun expire_withoutActiveRoundDoesNothing() {
        val g = game()
        g.expire()
        assertFalse(g.done)
    }

    @Test
    fun challenge_generatesInstanceFromLevelIgnoringLegacyParams() {
        val instance = TapTargetsChallenge().generate(4, """{"rounds":11}""", Random(1)) as TapTargetsInstance
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
