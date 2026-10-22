package app.luxion.apexapp.domain.adaptive

import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.challenge.ChallengeContext
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.SessionConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdaptiveSessionListenerTest {
    private class Settings : AdaptiveSettingsRepository {
        val rows = MutableStateFlow<Map<Long, AdaptiveSettings>>(emptyMap())
        override fun observe(alarmId: Long): Flow<AdaptiveSettings?> = rows.map { it[alarmId] }
        override suspend fun get(alarmId: Long) = rows.value[alarmId]
        override suspend fun save(settings: AdaptiveSettings) {
            rows.value = rows.value + (settings.alarmId to settings)
        }
        override suspend fun delete(alarmId: Long) {
            rows.value = rows.value - alarmId
        }
    }

    private val sleepy = listOf(
        SessionSummary("s3", 1, 0, 60_000),
        SessionSummary("s2", 1, 0, 60_000),
        SessionSummary("s1", 0, 0, 60_000),
    )
    private val settings = Settings()
    private val engine = AdaptiveDifficultyEngine(
        settings,
        object : WakeHistorySource {
            override suspend fun recent(alarmId: Long, limit: Int) = sleepy
        },
        AdaptiveDifficultyCalculator(),
        AdaptiveDifficultyConfig(),
        { 1_000L },
    )
    private val provider = AdaptiveDifficultyProvider(engine)

    private fun TestScope.listener() =
        AdaptiveSessionListener(engine, provider, CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler)))

    @Test
    fun finishingASessionEvaluatesTheAlarmAndReleasesTheLevel() = runTest {
        engine.setEnabled(1, true, 3)
        val config = SessionConfig(3, 1, ChallengeMode.CHAINED, emptyList())
        val ctx = ChallengeContext("s3", 1, ChallengeType("math"), 0, 0)
        assertEquals(3, provider.levelFor(config, ctx))

        listener().onFinished("s3", 1)

        assertEquals(4, settings.get(1)?.currentLevel)
        // El nivel de la sesión terminada se liberó: una sesión con el mismo id lo recalcula.
        assertEquals(4, provider.levelFor(config, ctx))
    }

    @Test
    fun sessionWithoutAlarmOnlyReleases() = runTest {
        engine.setEnabled(1, true, 3)
        listener().onFinished("s3", null)
        assertEquals(3, settings.get(1)?.currentLevel)
        assertNull(settings.get(2))
    }
}
