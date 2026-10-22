package app.luxion.apexapp.domain.challenge

import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SessionConfigTest {
    private val specs = listOf(ChallengeSpec("math"))

    @Test
    fun `difficulty out of range is clamped`() {
        assertEquals(5, SessionConfig.normalizeDifficulty(9))
        assertEquals(1, SessionConfig.normalizeDifficulty(-2))
        assertEquals(4, SessionConfig.normalizeDifficulty(4))
    }

    @Test
    fun `undefined difficulty means 3`() {
        assertEquals(3, SessionConfig.from(ChallengeConfig(difficulty = 0, challenges = specs)).difficulty)
    }

    @Test
    fun `count must be between 1 and 10`() {
        listOf(0, 11).forEach {
            assertThrows(IllegalArgumentException::class.java) {
                SessionConfig(3, it, ChallengeMode.CHAINED, specs)
            }
        }
        SessionConfig(3, 1, ChallengeMode.CHAINED, specs)
        SessionConfig(3, 10, ChallengeMode.CHAINED, specs)
    }

    @Test
    fun `config with only types and difficulty gives one chained challenge`() {
        val session = SessionConfig.from(ChallengeConfig(difficulty = 2, challenges = specs))
        assertEquals(1, session.count)
        assertEquals(ChallengeMode.CHAINED, session.mode)
        assertEquals(specs, session.challenges)
    }

    @Test
    fun `old json without count and mode deserializes with defaults`() {
        val json = Json { ignoreUnknownKeys = true }
        val config = json.decodeFromString<ChallengeConfig>(
            """{"difficulty":2,"challenges":[{"type":"math"}],"future":1}""",
        )
        assertEquals(1, config.count)
        assertEquals(ChallengeMode.CHAINED, config.mode)
    }
}
