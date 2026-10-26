package app.luxion.apexapp.ui.challenge

import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.challenge.ChallengeType
import org.junit.Assert.assertEquals
import org.junit.Test

class ChallengeConfigEditsTest {
    private val base = ChallengeConfig(count = 1, challenges = listOf(ChallengeSpec("math")))

    @Test
    fun `adding types raises the count only when needed`() {
        val two = base.toggled(ChallengeType("shake"))
        assertEquals(2, two.count)
        val four = two.toggled(ChallengeType("steps")).toggled(ChallengeType("standup"))
        assertEquals(4, four.count)
        assertEquals(6, four.copy(count = 6).toggled(ChallengeType("qr")).count)
    }

    @Test
    fun `removing a type does not lower the count`() {
        val four = base.copy(count = 4, challenges = listOf("math", "shake", "steps", "standup").map { ChallengeSpec(it) })
        assertEquals(4, four.toggled(ChallengeType("steps")).count)
    }

    @Test
    fun `count cannot go below the number of types nor above 10`() {
        val four = base.copy(count = 4, challenges = listOf("math", "shake", "steps", "standup").map { ChallengeSpec(it) })
        assertEquals(4, four.withCount(2).count)
        assertEquals(10, four.withCount(30).count)
        assertEquals(4, four.minCount)
    }

    @Test
    fun `saved count lower than types displays the minimum`() {
        val saved = base.copy(count = 1, challenges = listOf("math", "shake", "steps").map { ChallengeSpec(it) })
        assertEquals(3, saved.displayCount)
    }
}
