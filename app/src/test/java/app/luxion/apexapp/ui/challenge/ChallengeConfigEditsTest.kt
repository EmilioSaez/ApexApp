package app.luxion.apexapp.ui.challenge

import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.challenge.ChallengeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeConfigEditsTest {
    private val base = ChallengeConfig(count = 1, challenges = listOf(ChallengeSpec("math")))

    @Test
    fun `adding types raises the count only when needed`() {
        val two = base.toggled(ChallengeType("shake"))
        assertEquals(2, two.count)
        val four = two.toggled(ChallengeType("steps")).toggled(ChallengeType("qr"))
        assertEquals(4, four.count)
        assertEquals(6, four.copy(count = 6).toggled(ChallengeType("extra")).count)
    }

    @Test
    fun `removing a type does not lower the count`() {
        val four = base.copy(count = 4, challenges = listOf("math", "shake", "steps", "qr").map { ChallengeSpec(it) })
        assertEquals(4, four.toggled(ChallengeType("steps")).count)
    }

    @Test
    fun `count cannot go below the number of types nor above 10`() {
        val four = base.copy(count = 4, challenges = listOf("math", "shake", "steps", "qr").map { ChallengeSpec(it) })
        assertEquals(4, four.withCount(2).count)
        assertEquals(10, four.withCount(30).count)
        assertEquals(4, four.minCount)
    }

    @Test
    fun `saved count lower than types displays the minimum`() {
        val saved = base.copy(count = 1, challenges = listOf("math", "shake", "steps").map { ChallengeSpec(it) })
        assertEquals(3, saved.displayCount)
    }

    @Test
    fun `a config without types is normal, with types it is not`() {
        assertTrue(ChallengeConfig().isNormal)
        assertFalse(base.isNormal)
        assertTrue(base.toggled(ChallengeType.MATH).isNormal)
        assertFalse(ChallengeConfig(challengesEnabled = true).isNormal)
    }

    @Test
    fun `switching to normal clears every type`() {
        val many = base.toggled(ChallengeType("qr"))
        assertTrue(many.asNormal().isNormal)
    }

    @Test
    fun `switching from normal to challenges preselects nothing and keeps existing types`() {
        val empty = ChallengeConfig().withChallenges()
        assertTrue(empty.challenges.isEmpty())
        assertFalse(empty.isNormal)
        assertTrue(empty.needsType)
        assertEquals(base.copy(challengesEnabled = true), base.withChallenges())
        assertFalse(base.withChallenges().needsType)
    }

    @Test
    fun `removing the last type keeps challenges mode and requires picking one`() {
        val none = base.withChallenges().toggled(ChallengeType.MATH)
        assertFalse(none.isNormal)
        assertTrue(none.needsType)
    }

    @Test
    fun `switching to normal clears the challenges mode`() {
        assertTrue(base.withChallenges().asNormal().isNormal)
    }

    @Test
    fun `entering challenges mode keeps the count at its minimum`() {
        assertEquals(1, ChallengeConfig(count = 1).withChallenges().count)
    }
}
