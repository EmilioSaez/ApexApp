package app.luxion.apexapp.ui.home

import android.content.Intent
import app.luxion.apexapp.alarm.AlarmClockScheduler
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

    private fun viewModel() = HomeViewModel(
        repository,
        AlarmController(repository, AlarmClockScheduler(gateway, Clock.systemDefaultZone())),
        permissions,
    )

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
    fun setEnabled_updatesAlarmAndSchedule() = runTest {
        val id = repository.save(Alarm(hour = 8, minute = 0, enabled = false))
        viewModel().setEnabled(id, true)
        assertTrue(repository.get(id)!!.enabled)
        assertTrue(id in gateway.scheduled)
    }
}
