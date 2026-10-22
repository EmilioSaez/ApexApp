package app.luxion.apexapp.ui.stats

import app.luxion.apexapp.domain.SleepRepository
import app.luxion.apexapp.domain.Streak
import app.luxion.apexapp.domain.WakeHistoryRepository
import app.luxion.apexapp.domain.WakeSession
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    private class FakeHistory : WakeHistoryRepository {
        val requested = mutableListOf<Pair<Instant, Instant>>()

        override fun observeSessions(from: Instant, to: Instant, challengeType: String?): Flow<List<WakeSession>> {
            requested += from to to
            return flowOf(emptyList())
        }

        override suspend fun sessions(from: Instant, to: Instant, challengeType: String?) = emptyList<WakeSession>()
        override suspend fun recentSessions(alarmId: Long, limit: Int, excludeSuspicious: Boolean) = emptyList<WakeSession>()
        override fun observeStreak(): Flow<Streak> = emptyFlow()
    }

    private class FakeSleep : SleepRepository {
        val usual = MutableStateFlow<LocalTime?>(null)
        val manual = MutableStateFlow<Map<LocalDate, Long>>(emptyMap())

        override fun observeManualBedtimes() = manual
        override suspend fun setBedtime(wakeDate: LocalDate, bedtimeAt: Long) {
            manual.value = manual.value + (wakeDate to bedtimeAt)
        }

        override suspend fun clearBedtime(wakeDate: LocalDate) {
            manual.value = manual.value - wakeDate
        }

        override fun observeUsualBedtime() = usual
        override suspend fun setUsualBedtime(time: LocalTime?) {
            usual.value = time
        }
    }

    private val clock = Clock.fixed(Instant.parse("2026-03-10T12:00:00Z"), ZoneOffset.UTC)
    private val history = FakeHistory()
    private val sleep = FakeSleep()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun emptyHistory_showsEmptyState() = runTest {
        val state = StatsViewModel(history, sleep, clock).uiState.first { !it.isLoading }
        assertFalse(state.hasData)
        assertEquals(7, state.days.size)
    }

    @Test
    fun selectingRange_recomputesForThatManyDays() = runTest {
        val vm = StatsViewModel(history, sleep, clock)
        assertEquals(7, vm.uiState.first { !it.isLoading }.days.size)
        vm.selectRange(StatsRange.MONTH)
        val state = vm.uiState.first { it.range == StatsRange.MONTH }
        assertEquals(30, state.days.size)
        assertEquals(LocalDate.of(2026, 3, 10), state.days.last().date)
    }

    @Test
    fun lastNightBedtime_afterNowUsesPreviousDay() = runTest {
        val vm = StatsViewModel(history, sleep, clock)
        vm.setLastNightBedtime(LocalTime.of(23, 30))
        val expected = Instant.parse("2026-03-09T23:30:00Z").toEpochMilli()
        assertEquals(expected, sleep.manual.value[LocalDate.of(2026, 3, 10)])
    }

    @Test
    fun usualBedtime_isReflectedInState() = runTest {
        val vm = StatsViewModel(history, sleep, clock)
        vm.setUsualBedtime(LocalTime.of(23, 0))
        assertEquals(LocalTime.of(23, 0), vm.uiState.first { it.usualBedtime != null }.usualBedtime)
    }
}
