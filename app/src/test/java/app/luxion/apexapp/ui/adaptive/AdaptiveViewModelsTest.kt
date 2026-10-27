package app.luxion.apexapp.ui.adaptive

import androidx.lifecycle.SavedStateHandle
import app.luxion.apexapp.alarm.FakeAlarmRepository
import app.luxion.apexapp.domain.adaptive.AdaptiveDifficultyCalculator
import app.luxion.apexapp.domain.adaptive.AdaptiveDifficultyConfig
import app.luxion.apexapp.domain.adaptive.AdaptiveDifficultyEngine
import app.luxion.apexapp.domain.adaptive.AdaptiveSettings
import app.luxion.apexapp.domain.adaptive.AdaptiveSettingsRepository
import app.luxion.apexapp.domain.adaptive.ChangeReason
import app.luxion.apexapp.domain.adaptive.SessionSummary
import app.luxion.apexapp.domain.adaptive.WakeHistorySource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeSettings : AdaptiveSettingsRepository {
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

private object EmptyHistory : WakeHistorySource {
    override suspend fun recent(alarmId: Long, limit: Int): List<SessionSummary> = emptyList()
}

@OptIn(ExperimentalCoroutinesApi::class)
class AdaptiveViewModelsTest {
    private val settings = FakeSettings()
    private val engine = AdaptiveDifficultyEngine(
        settings,
        EmptyHistory,
        AdaptiveDifficultyCalculator(),
        AdaptiveDifficultyConfig(),
        now = { 1_000L },
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun editor(alarmId: Long) =
        AdaptiveDifficultyViewModel(SavedStateHandle(mapOf("alarmId" to alarmId)), FakeAlarmRepository(), settings, engine)

    @Test
    fun switchPersistsAndIsReflectedInTheState() = runTest {
        val vm = editor(1)
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()
        assertTrue(vm.state.value.available)
        assertFalse(vm.state.value.enabled)

        vm.setEnabled(true)

        runCurrent()
        assertTrue(settings.get(1)?.enabled == true)
        assertTrue(vm.state.value.enabled)

        vm.setEnabled(false)

        runCurrent()
        assertFalse(settings.get(1)?.enabled == true)
        assertFalse(vm.state.value.enabled)
    }

    @Test
    fun validRangeIsSavedAndInvalidOneIsRejected() = runTest {
        val vm = editor(1)
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()
        vm.setEnabled(true)
        runCurrent()

        vm.setRange(2, 4)

        runCurrent()
        assertEquals(2 to 4, vm.state.value.min to vm.state.value.max)

        vm.setRange(4, 2)

        runCurrent()
        vm.setRange(0, 4)
        runCurrent()
        vm.setRange(2, 6)
        runCurrent()
        assertEquals(2 to 4, vm.state.value.min to vm.state.value.max)
        assertEquals(2 to 4, settings.get(1)?.minLevel to settings.get(1)?.maxLevel)
    }

    @Test
    fun resetClearsTheLastChange() = runTest {
        settings.save(
            AdaptiveSettings(1, enabled = true, currentLevel = 4, lastChangeReason = ChangeReason.SNOOZES, lastChangeHits = 2),
        )
        val vm = editor(1)
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()
        assertEquals(ChangeReason.SNOOZES, vm.state.value.reason)

        vm.reset()

        runCurrent()
        assertNull(vm.state.value.reason)
    }

    @Test
    fun newAlarmCannotEnableItAndStoresNothing() = runTest {
        val vm = editor(0)
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()
        vm.setEnabled(true)
        runCurrent()
        vm.setRange(2, 4)
        runCurrent()
        assertFalse(vm.state.value.available)
        assertTrue(settings.rows.value.isEmpty())
    }

    @Test
    fun noticeIsNullWithoutAChange() = runTest {
        settings.save(AdaptiveSettings(1, enabled = true, currentLevel = 3))
        val vm = AdaptiveNoticeViewModel(engine)
        vm.load(1)
        assertNull(vm.notice.value)
    }

    @Test
    fun noticeIsShownOnceAndConsumed() = runTest {
        settings.save(
            AdaptiveSettings(1, enabled = true, currentLevel = 4, lastChangeReason = ChangeReason.SNOOZES,
                lastChangeHits = 2, noticePending = true),
        )
        val vm = AdaptiveNoticeViewModel(engine)
        vm.load(1)
        assertNotNull(vm.notice.value)
        assertEquals(AdaptiveNotice(4, ChangeReason.SNOOZES, 2), vm.notice.value)
        assertFalse(settings.get(1)?.noticePending == true)

        // Una segunda pantalla del reto para la misma alarma ya no lo ve.
        val another = AdaptiveNoticeViewModel(engine)
        another.load(1)
        assertNull(another.notice.value)
    }

    @Test
    fun noticeIgnoresDisabledSettingsAndMissingAlarm() = runTest {
        settings.save(
            AdaptiveSettings(1, enabled = false, currentLevel = 4, lastChangeReason = ChangeReason.EASY, noticePending = true),
        )
        val disabled = AdaptiveNoticeViewModel(engine)
        disabled.load(1)
        assertNull(disabled.notice.value)

        val noAlarm = AdaptiveNoticeViewModel(engine)
        noAlarm.load(null)
        assertNull(noAlarm.notice.value)
    }
}
