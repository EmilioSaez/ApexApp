package app.luxion.apexapp.ui.editor

import androidx.lifecycle.SavedStateHandle
import app.luxion.apexapp.alarm.FakeAlarmRepository
import app.luxion.apexapp.alarm.FakeGateway
import app.luxion.apexapp.alarm.ScheduleResult
import app.luxion.apexapp.alarm.testController
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AntiCheatLevel
import app.luxion.apexapp.domain.AntiCheatPolicy
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmEditorViewModelTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val clock = Clock.fixed(ZonedDateTime.of(2026, 10, 21, 8, 0, 0, 0, zone).toInstant(), zone)
    private val repository = FakeAlarmRepository()
    private val gateway = FakeGateway()
    private val controller = testController(repository, gateway, clock)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(alarmId: Long = 0L) =
        AlarmEditorViewModel(SavedStateHandle(mapOf("alarmId" to alarmId)), repository, controller, clock)

    @Test
    fun newAlarm_savesEnabledAndSchedules() = runTest {
        val vm = viewModel()
        vm.setLabel("  Gym ")
        vm.toggleDay(DayOfWeek.MONDAY)
        vm.save(9, 30)
        val alarm = repository.observeAll().first().single()
        assertEquals(9, alarm.hour)
        assertEquals(30, alarm.minute)
        assertEquals("Gym", alarm.label)
        assertEquals(setOf(DayOfWeek.MONDAY), alarm.repeatDays)
        assertTrue(alarm.enabled)
        assertTrue(alarm.id in gateway.scheduled)
        val saved = vm.uiState.first { it.saved != null }.saved!!
        assertEquals(ScheduleResult.Scheduled, saved.schedule)
        // Miércoles 08:00 → lunes 09:30: 5 días y 1 h 30 min de reloj, más 1 h por el fin del horario de verano (25 oct).
        assertEquals(Duration.ofHours(5 * 24 + 2).plusMinutes(30), saved.remaining)
    }

    @Test
    fun editExisting_changesTimeAndKeepsOtherFields() = runTest {
        val original = Alarm(
            hour = 7, minute = 0, label = "Old", antiCheat = AntiCheatPolicy(level = AntiCheatLevel.STANDARD),
        )
        val id = repository.save(original)
        val vm = viewModel(id)
        val state = vm.uiState.first { !it.isLoading }
        assertFalse(state.isNew)
        assertEquals("Old", state.label)
        vm.save(8, 15)
        val stored = repository.get(id)!!
        assertEquals(8, stored.hour)
        assertEquals(15, stored.minute)
        assertEquals(AntiCheatLevel.STANDARD, stored.antiCheat.level)
        assertEquals(1, repository.observeAll().first().size)
    }

    @Test
    fun editDisabledAlarm_staysDisabledAndUnscheduled() = runTest {
        val id = repository.save(Alarm(hour = 7, minute = 0, enabled = false))
        val vm = viewModel(id)
        vm.uiState.first { !it.isLoading }
        vm.save(8, 0)
        assertFalse(repository.get(id)!!.enabled)
        assertFalse(id in gateway.scheduled)
        assertNull(vm.uiState.first { it.saved != null }.saved!!.remaining)
    }

    @Test
    fun missingExactPermission_isReported() = runTest {
        gateway.canExact = false
        val vm = viewModel()
        vm.save(9, 0)
        assertEquals(ScheduleResult.PermissionMissing, vm.uiState.first { it.saved != null }.saved!!.schedule)
    }

    @Test
    fun remainingTime_roundsUpToMinutes() {
        val t = Duration.ofHours(26).plusMinutes(5).plusSeconds(1).toRemainingTime()
        assertEquals(RemainingTime(days = 1, hours = 2, minutes = 6), t)
        assertEquals(RemainingTime(0, 0, 45), Duration.ofMinutes(45).toRemainingTime())
    }
}
