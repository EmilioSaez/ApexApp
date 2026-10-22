package app.luxion.apexapp.domain.adaptive

import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.challenge.ChallengeContext
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.SessionConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeSettingsRepository : AdaptiveSettingsRepository {
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

private class FakeHistory(var sessions: List<SessionSummary> = emptyList(), var failing: Boolean = false) : WakeHistorySource {
    override suspend fun recent(alarmId: Long, limit: Int): List<SessionSummary> {
        if (failing) error("wake-stats no disponible")
        return sessions.take(limit)
    }
}

class AdaptiveDifficultyEngineTest {
    private val settings = FakeSettingsRepository()
    private val history = FakeHistory()
    private val engine = AdaptiveDifficultyEngine(
        settings,
        history,
        AdaptiveDifficultyCalculator(),
        AdaptiveDifficultyConfig(),
        now = { 1_000L },
    )

    private fun s(id: Int, snoozes: Int = 0) = SessionSummary("s$id", snoozes, 0, 60_000)

    private val sleepy = listOf(s(3, 1), s(2, 1), s(1))

    @Test
    fun withoutSettingsTheBaseLevelIsUsed() = runTest {
        assertEquals(3, engine.levelFor(1, 3))
        assertEquals(3, engine.levelFor(null, 3))
    }

    @Test
    fun disabledAlarmUsesBaseLevelWhateverTheHistory() = runTest {
        engine.setEnabled(1, true, 3)
        history.sessions = sleepy
        engine.onSessionFinished(1)
        engine.setEnabled(1, false, 3)
        assertEquals(2, engine.levelFor(1, 2))
    }

    @Test
    fun enablingStartsFromTheBaseLevelWithinRange() = runTest {
        engine.setRange(1, 2, 4, 5)
        engine.setEnabled(1, true, 5)
        assertEquals(4, engine.levelFor(1, 5))
    }

    @Test
    fun invalidRangeIsRejectedAndKeepsThePreviousOne() = runTest {
        engine.setEnabled(1, true, 3)
        assertTrue(engine.setRange(1, 2, 4, 3))
        assertFalse(engine.setRange(1, 4, 2, 3))
        assertFalse(engine.setRange(1, 0, 4, 3))
        assertFalse(engine.setRange(1, 2, 6, 3))
        assertEquals(2, settings.get(1)?.minLevel)
        assertEquals(4, settings.get(1)?.maxLevel)
    }

    @Test
    fun sessionFinishedRaisesLevelAndStoresTheReason() = runTest {
        engine.setEnabled(1, true, 3)
        history.sessions = sleepy
        val d = engine.onSessionFinished(1)
        assertEquals(4, d?.level)
        val row = settings.get(1)!!
        assertEquals(4, row.currentLevel)
        assertEquals(ChangeReason.SNOOZES, row.lastChangeReason)
        assertEquals(2, row.lastChangeHits)
        assertEquals(3, row.lastChangeFrom)
        assertEquals(1_000L, row.lastChangeAt)
        assertEquals("s3", row.lastChangeSessionId)
        assertTrue(row.noticePending)
        assertEquals(4, engine.levelFor(1, 3))
    }

    @Test
    fun noSecondChangeWithoutNewSessions() = runTest {
        engine.setEnabled(1, true, 3)
        history.sessions = sleepy
        engine.onSessionFinished(1)
        assertFalse(engine.onSessionFinished(1)!!.changed)
        assertEquals(4, settings.get(1)?.currentLevel)
    }

    @Test
    fun disabledAlarmIsNotEvaluated() = runTest {
        history.sessions = sleepy
        assertNull(engine.onSessionFinished(1))
        engine.setEnabled(1, true, 3)
        engine.setEnabled(1, false, 3)
        assertNull(engine.onSessionFinished(1))
    }

    @Test
    fun historyFailureKeepsLastLevel() = runTest {
        engine.setEnabled(1, true, 3)
        history.sessions = sleepy
        engine.onSessionFinished(1)
        history.failing = true
        assertNull(engine.onSessionFinished(1))
        assertEquals(4, engine.levelFor(1, 3))
    }

    @Test
    fun resetGoesBackToBaseAndIgnoresPastSessions() = runTest {
        engine.setEnabled(1, true, 3)
        history.sessions = sleepy
        engine.onSessionFinished(1)
        engine.reset(1, 3)
        val row = settings.get(1)!!
        assertEquals(3, row.currentLevel)
        assertNull(row.lastChangeReason)
        assertFalse(row.noticePending)
        assertEquals("s3", row.lastChangeSessionId)
        assertFalse(engine.onSessionFinished(1)!!.changed)
    }

    @Test
    fun noticeIsConsumedOnce() = runTest {
        engine.setEnabled(1, true, 3)
        history.sessions = sleepy
        engine.onSessionFinished(1)
        assertEquals(4, engine.consumeNotice(1)?.currentLevel)
        assertNull(engine.consumeNotice(1))
    }

    @Test
    fun providerFixesTheLevelForTheWholeSession() = runTest {
        engine.setEnabled(1, true, 3)
        val provider = AdaptiveDifficultyProvider(engine)
        val config = SessionConfig(3, 1, ChallengeMode.CHAINED, emptyList())
        val ctx = ChallengeContext("a", 1, ChallengeType("math"), 0, 0)
        assertEquals(3, provider.levelFor(config, ctx))
        history.sessions = sleepy
        engine.onSessionFinished(1)
        assertEquals(3, provider.levelFor(config, ctx.copy(index = 1, failures = 2)))
        assertEquals(4, provider.levelFor(config, ctx.copy(sessionId = "b")))
    }

    @Test
    fun providerUsesBaseLevelForAlarmWithoutAdaptive() = runTest {
        val provider = AdaptiveDifficultyProvider(engine)
        val config = SessionConfig(2, 1, ChallengeMode.CHAINED, emptyList())
        assertEquals(2, provider.levelFor(config, ChallengeContext("a", 7, ChallengeType("math"), 0, 0)))
    }
}
