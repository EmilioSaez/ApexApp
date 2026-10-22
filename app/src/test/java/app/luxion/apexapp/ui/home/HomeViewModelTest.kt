package app.luxion.apexapp.ui.home

import android.content.Intent
import app.luxion.apexapp.alarm.AlarmClockScheduler
import app.luxion.apexapp.alarm.AlarmFiringHandler
import app.luxion.apexapp.alarm.AlarmNotifier
import app.luxion.apexapp.alarm.FakeFireStateRepository
import app.luxion.apexapp.alarm.FakeRingingLauncher
import app.luxion.apexapp.alarm.NoOpWakeEventRecorder
import app.luxion.apexapp.alarm.FakeDeviceProbe
import app.luxion.apexapp.alarm.FakeGuideStore
import app.luxion.apexapp.alarm.ReliabilityChecker
import app.luxion.apexapp.alarm.RingingRegistry
import app.luxion.apexapp.alarm.AlarmController
import app.luxion.apexapp.alarm.AlarmPermissions
import app.luxion.apexapp.alarm.FakeAlarmRepository
import app.luxion.apexapp.alarm.FakeGateway
import app.luxion.apexapp.alarm.PermissionState
import app.luxion.apexapp.domain.Alarm
import java.time.Clock
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
class HomeViewModelTest {
    private class FakePermissions(var state: PermissionState = PermissionState()) : AlarmPermissions {
        override fun current() = state
        override fun exactAlarmSettingsIntent(): Intent? = null
    }

    private val repository = FakeAlarmRepository()
    private val permissions = FakePermissions()
    private val gateway = FakeGateway()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(): HomeViewModel {
        val clock = Clock.systemDefaultZone()
        val fireState = FakeFireStateRepository()
        val scheduler = AlarmClockScheduler(gateway, clock, fireState)
        val handler = AlarmFiringHandler(
            repository, scheduler, RingingRegistry(), FakeRingingLauncher(),
            object : AlarmNotifier {
                override fun notify(alarm: Alarm) = Unit
            },
            fireState, clock,
        )
        return HomeViewModel(
            repository,
            AlarmController(repository, scheduler, fireState, handler, NoOpWakeEventRecorder(), clock),
            permissions,
            ReliabilityChecker(FakeDeviceProbe(), FakeGuideStore()),
        )
    }

    @Test
    fun emptyRepository_emitsLoadedEmptyState() = runTest {
        val vm = viewModel()
        val state = vm.uiState.first { !it.isLoading }
        assertTrue(state.alarms.isEmpty())
    }

    @Test
    fun savedAlarms_appearInState() = runTest {
        repository.save(Alarm(hour = 8, minute = 0))
        val state = viewModel().uiState.first { !it.isLoading }
        assertEquals(1, state.alarms.size)
    }

    @Test
    fun refreshPermissions_updatesState() = runTest {
        val vm = viewModel()
        assertTrue(vm.uiState.first { !it.isLoading }.permissions.notificationsGranted)
        permissions.state = PermissionState(notificationsGranted = false)
        vm.refreshPermissions()
        assertFalse(vm.uiState.first { !it.permissions.notificationsGranted }.permissions.notificationsGranted)
    }

    @Test
    fun delete_removesAlarmCancelsScheduleAndOffersUndo() = runTest {
        val id = repository.save(Alarm(hour = 8, minute = 0))
        val vm = viewModel()
        vm.setEnabled(id, true)
        vm.delete(id)
        assertNull(repository.get(id))
        assertFalse(id in gateway.scheduled)
        assertEquals(id, vm.uiState.first { it.deleted != null }.deleted!!.id)
    }

    @Test
    fun undoDelete_restoresSameAlarmAndReschedules() = runTest {
        val id = repository.save(Alarm(hour = 8, minute = 0, label = "Gym"))
        val vm = viewModel()
        vm.delete(id)
        vm.undoDelete()
        assertEquals("Gym", repository.get(id)!!.label)
        assertEquals(id, repository.get(id)!!.id)
        assertTrue(id in gateway.scheduled)
        assertNull(vm.uiState.first { !it.isLoading }.deleted)
    }

    @Test
    fun setEnabled_updatesAlarmAndSchedule() = runTest {
        val id = repository.save(Alarm(hour = 8, minute = 0, enabled = false))
        viewModel().setEnabled(id, true)
        assertTrue(repository.get(id)!!.enabled)
        assertTrue(id in gateway.scheduled)
    }
}
