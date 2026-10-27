package app.luxion.apexapp.domain.challenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeRegistryTest {
    private val math = MathChallenge()
    private val registry = ChallengeRegistry(listOf(math))

    @Test
    fun `registered type resolves`() {
        assertSame(math, registry[ChallengeType.MATH])
        assertTrue(ChallengeType.MATH in registry)
        assertEquals(setOf(ChallengeType.MATH), registry.types)
    }

    @Test
    fun `unknown type does not resolve`() {
        assertNull(registry[ChallengeType("qr")])
        assertFalse(ChallengeType("qr") in registry)
    }
}
