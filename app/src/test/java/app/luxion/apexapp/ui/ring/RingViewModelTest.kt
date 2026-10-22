package app.luxion.apexapp.ui.ring

import androidx.lifecycle.SavedStateHandle
import app.luxion.apexapp.alarm.AlarmNotifier
import app.luxion.apexapp.alarm.AlarmResolver
import app.luxion.apexapp.alarm.EndReason
import app.luxion.apexapp.alarm.FakeAlarmRepository
import app.luxion.apexapp.domain.Alarm
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
class RingViewModelTest {
    private val repository = FakeAlarmRepository()
    private val cancelled = mutableListOf<Long>()
    private val notifier = object : AlarmNotifier {
        override fun notify(alarm: Alarm, sessionId: String) = Unit
        override fun cancel(alarmId: Long) {
            cancelled += alarmId
        }
    }

    private val resolved = mutableListOf<String>()
    private val resolver = object : AlarmResolver {
        override fun resolve(sessionId: String, reason: EndReason) {
            resolved += sessionId
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(alarmId: Long, gate: DismissGate = DefaultDismissGate()) = RingViewModel(
        SavedStateHandle(mapOf(RingViewModel.EXTRA_ALARM_ID to alarmId, RingViewModel.EXTRA_SESSION_ID to "s-1")),
        repository,
        gate,
        notifier,
        resolver,
    )

    @Test
    fun loadsAlarmAndSession_andAllowsDismissByDefault() = runTest {
        val id = repository.save(Alarm(hour = 7, minute = 0, label = "Gym"))
        val state = viewModel(id).uiState.first { it.alarm != null }
        assertEquals("Gym", state.alarm!!.label)
        assertEquals("s-1", state.sessionId)
        assertTrue(state.canDismiss)
    }

    @Test
    fun dismiss_cancelsNotificationAndFinishes() = runTest {
        val id = repository.save(Alarm(hour = 7, minute = 0))
        val vm = viewModel(id)
        vm.dismiss()
        assertEquals(listOf(id), cancelled)
        assertEquals(listOf("s-1"), resolved)
        assertTrue(vm.uiState.value.finished)
    }

    @Test
    fun gateThatDenies_blocksDismiss() = runTest {
        val id = repository.save(Alarm(hour = 7, minute = 0))
        val vm = viewModel(id, gate = object : DismissGate {
            override fun canDismiss(alarm: Alarm) = false
        })
        assertFalse(vm.uiState.first { it.alarm != null }.canDismiss)
        vm.dismiss()
        assertTrue(cancelled.isEmpty())
        assertFalse(vm.uiState.value.finished)
    }

    @Test
    fun deletedAlarm_finishesWithoutShowingAnything() = runTest {
        val vm = viewModel(alarmId = 99)
        assertTrue(vm.uiState.first { it.finished }.alarm == null)
    }
}
