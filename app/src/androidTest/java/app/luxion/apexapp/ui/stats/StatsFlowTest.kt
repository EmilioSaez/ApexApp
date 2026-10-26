package app.luxion.apexapp.ui.stats

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.luxion.apexapp.R
import app.luxion.apexapp.data.RoomSleepRepository
import app.luxion.apexapp.data.RoomWakeEventRecorder
import app.luxion.apexapp.data.RoomWakeHistoryRepository
import app.luxion.apexapp.data.local.ApexDatabase
import app.luxion.apexapp.domain.BedtimeSource
import app.luxion.apexapp.domain.WakeEventType
import app.luxion.apexapp.ui.home.HomeContent
import app.luxion.apexapp.ui.home.HomeUiState
import app.luxion.apexapp.ui.theme.ApexAppTheme
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Pantalla de estadísticas con la navegación, el tema y el almacenamiento reales (Room en memoria). */
class StatsFlowTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var db: ApexDatabase
    private lateinit var recorder: RoomWakeEventRecorder
    private lateinit var viewModel: StatsViewModel

    // Mediodía UTC del 10-mar-2026: los despertares de prueba caen a las 07:00 de hoy y de los días previos.
    private val now = Instant.parse("2026-03-10T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val sevenAm = 7 * 3_600_000L
    private val day = 24 * 3_600_000L

    private fun str(id: Int, vararg args: Any) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ApexDatabase::class.java).build()
        recorder = RoomWakeEventRecorder(db, db.wakeEventDao())
        viewModel = StatsViewModel(
            RoomWakeHistoryRepository(db.wakeEventDao(), clock),
            RoomSleepRepository(db.sleepDao()),
            clock,
        )
    }

    @After
    fun tearDown() = db.close()

    /** Despertar a las 07:00 de [daysAgo] días atrás: sonó, un snooze, un fallo y completado a los 60 s. */
    private suspend fun morning(daysAgo: Int) {
        val at = LocalDate.of(2026, 3, 10).minusDays(daysAgo.toLong()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() + sevenAm
        val id = "s$daysAgo"
        recorder.record(id, WakeEventType.RANG, null, at, scheduledAt = at)
        recorder.record(id, WakeEventType.SNOOZED, null, at + 1)
        recorder.record(id, WakeEventType.CHALLENGE_FAILED, null, at + 2, challengeType = "math", difficulty = 3)
        recorder.record(id, WakeEventType.COMPLETED, null, at + 60_000)
    }

    private val chartTitles get() = listOf(
        R.string.stats_wake_time, R.string.stats_snoozes, R.string.stats_fails,
        R.string.stats_punctuality, R.string.stats_sleep,
    )

    // 5.1 ---------------------------------------------------------------------------------------

    @Test
    fun statsOpensFromHome_andBackReturnsToTheList() {
        compose.setContent {
            ApexAppTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "home") {
                    composable("home") {
                        HomeContent(
                            state = HomeUiState(isLoading = false),
                            onAddAlarm = {}, onToggle = { _, _ -> },
                            onRequestNotifications = {}, onOpenExactAlarmSettings = {},
                            onOpenStats = { nav.navigate("stats") },
                        )
                    }
                    composable("stats") {
                        StatsContent(
                            StatsUiState(isLoading = false, hasData = false),
                            onBack = { nav.popBackStack() },
                            onSelectRange = {}, onSetLastNightBedtime = {}, onSetUsualBedtime = {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithText(str(R.string.stats_empty)).assertDoesNotExist()

        compose.onNodeWithText(str(R.string.stats_open)).performClick()
        compose.onNodeWithText(str(R.string.stats_empty)).assertExists()

        compose.onNodeWithContentDescription(str(R.string.back)).performClick()
        compose.onNodeWithText(str(R.string.stats_empty)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.stats_open)).assertExists()
    }

    // 5.3 ---------------------------------------------------------------------------------------

    @Test
    fun chartsAndEmptyStateRenderInLightAndDarkThemes() {
        var dark by mutableStateOf(false)
        var hasData by mutableStateOf(true)
        val days = (6 downTo 0).map { DayPoint(LocalDate.of(2026, 3, 10).minusDays(it.toLong()), 60_000L, 1, 1, 1.0) }
        compose.setContent {
            ApexAppTheme(darkTheme = dark) {
                StatsContent(
                    StatsUiState(isLoading = false, hasData = hasData, days = days),
                    onBack = {}, onSelectRange = {}, onSetLastNightBedtime = {}, onSetUsualBedtime = {},
                )
            }
        }
        listOf(false, true).forEach { isDark ->
            dark = isDark
            hasData = true
            compose.waitForIdle()
            chartTitles.forEach {
                compose.onAllNodes(hasContentDescription(str(it), substring = true)).assertCountEquals(1)
            }
            compose.onNodeWithText(str(R.string.stats_empty)).assertDoesNotExist()

            hasData = false
            compose.waitForIdle()
            compose.onNodeWithText(str(R.string.stats_empty)).assertExists()
        }
    }

    // 5.4 ---------------------------------------------------------------------------------------

    @Test
    fun registeringBedtime_updatesTheSleepChart() {
        runBlocking { morning(0) }
        compose.setContent { ApexAppTheme { StatsScreen(onBack = {}, viewModel = viewModel) } }
        val withHours = hasContentDescription(str(R.string.stats_sleep), substring = true) and
            hasContentDescription("8 h", substring = true)

        compose.waitUntil(5_000) {
            compose.onAllNodes(hasContentDescription(str(R.string.stats_sleep), substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(withHours).assertCountEquals(0)

        // 23:00 es posterior a «ahora» (12:00), así que cuenta como la noche anterior: 8 h 1 min hasta las 07:01.
        compose.onNodeWithText(str(R.string.stats_set_bedtime)).performScrollTo().performClick()
        compose.onNodeWithText(str(R.string.stats_dialog_ok)).performClick()

        compose.waitUntil(5_000) { compose.onAllNodes(withHours).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(withHours).assertCountEquals(1)
    }

    // 6.1 ---------------------------------------------------------------------------------------

    @Test
    fun simulatedMornings_produceMetricsStreakAndSleepHours() = runBlocking {
        (0..2).forEach { morning(it) }

        val state = withTimeout(5_000) { viewModel.uiState.first { !it.isLoading && it.hasData } }
        assertEquals(3, state.stats.completed)
        assertEquals(3, state.stats.totalSnoozes)
        assertEquals(3, state.stats.totalFails)
        assertEquals(60_000L, state.stats.averageTimeToWakeMs)
        assertEquals(1.0, state.stats.punctualRate!!, 0.0)
        assertEquals(3, state.streak.current)
        assertEquals(7, state.days.size)
        assertEquals(3, state.days.count { it.snoozes == 1 && it.fails == 1 })

        // Hora habitual: las tres noches se estiman (23:30 → 07:01 = 7 h 31 min).
        viewModel.setUsualBedtime(LocalTime.of(23, 30))
        val estimated = withTimeout(5_000) { viewModel.uiState.first { it.nights.size == 3 } }
        assertEquals(setOf(BedtimeSource.ESTIMATED), estimated.nights.map { it.source }.toSet())
        assertEquals(setOf(451 * 60_000L), estimated.nights.map { it.sleepMs }.toSet())

        // El registro manual de esta mañana prevalece sobre la estimación: 23:00 → 07:01 = 8 h 1 min.
        viewModel.setLastNightBedtime(LocalTime.of(23, 0))
        val manual = withTimeout(5_000) {
            viewModel.uiState.first { s -> s.nights.any { it.source == BedtimeSource.MANUAL } }
        }
        val tonight = manual.nights.single { it.source == BedtimeSource.MANUAL }
        assertEquals(LocalDate.of(2026, 3, 10), tonight.wakeDate)
        assertEquals(481 * 60_000L, tonight.sleepMs)
        assertEquals(2, manual.nights.count { it.source == BedtimeSource.ESTIMATED })
        assertNotNull(manual.averageSleepMs)
    }
}
