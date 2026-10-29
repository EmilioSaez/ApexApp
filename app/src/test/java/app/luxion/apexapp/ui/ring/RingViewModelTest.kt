package app.luxion.apexapp.ui.ring

import androidx.lifecycle.SavedStateHandle
import app.luxion.apexapp.alarm.AlarmNotifier
import app.luxion.apexapp.alarm.AlarmResolver
import app.luxion.apexapp.alarm.EndReason
import app.luxion.apexapp.alarm.FakeAlarmRepository
import app.luxion.apexapp.alarm.RingingRegistry
import app.luxion.apexapp.alarm.RingingSession
import app.luxion.apexapp.alarm.mute.ChallengeMuteController
import app.luxion.apexapp.alarm.mute.RingMuter
import app.luxion.apexapp.anticheat.SnoozeController
import app.luxion.apexapp.anticheat.SnoozeCounts
import app.luxion.apexapp.anticheat.SnoozeScheduler
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AntiCheatLevel
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.challenge.PassedAlarms
import app.luxion.apexapp.domain.challenge.SessionConfig
import app.luxion.apexapp.ui.challenge.ChallengeDismissGate
import app.luxion.apexapp.domain.AntiCheatPolicy
import app.luxion.apexapp.domain.SnoozePolicy
import app.luxion.apexapp.domain.WakeEventRecorder
import app.luxion.apexapp.domain.WakeEventType
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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

    private val muteRegistry = RingingRegistry()
    private val muteCalls = mutableListOf<Boolean>()
    private val muteController = ChallengeMuteController(
        muteRegistry,
        RingMuter { muteCalls += it },
        Clock.systemUTC(),
        CoroutineScope(Job()),
    )
    private val snoozeCalls = mutableListOf<Triple<Long, String, Long>>()
    private val counts = HashMap<String, Int>()
    private val snoozeController = SnoozeController(
        resolver,
        object : SnoozeScheduler {
            override fun schedule(alarmId: Long, sessionId: String, triggerAtMillis: Long) {
                snoozeCalls += Triple(alarmId, sessionId, triggerAtMillis)
            }
        },
        object : SnoozeCounts {
            override fun get(sessionId: String) = counts[sessionId] ?: 0
            override fun set(sessionId: String, count: Int) {
                counts[sessionId] = count
            }

            override fun clear(sessionId: String) {
                counts.remove(sessionId)
            }
        },
        object : WakeEventRecorder {
            override suspend fun record(
                sessionId: String,
                type: WakeEventType,
                alarmId: Long?,
                timestamp: Long,
                scheduledAt: Long?,
                challengeType: String?,
                difficulty: Int?,
            ) = Unit
        },
        Clock.systemUTC(),
    )

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
        snoozeController,
        muteController,
    )

    @Test
    fun challengeInteraction_mutesWhenTheAlarmHasTheOptionOn() = runTest {
        val id = repository.save(Alarm(hour = 7, minute = 0, muteDuringChallenges = true))
        muteRegistry.tryBegin(RingingSession("s-1", repository.get(id)!!))
        val viewModel = viewModel(id)
        viewModel.uiState.first { it.alarm != null }
        assertTrue(muteCalls.isEmpty())

        viewModel.onChallengeInteraction()
        assertEquals(listOf(true), muteCalls)
    }

    @Test
    fun challengeInteraction_doesNothingWhenTheOptionIsOff() = runTest {
        val id = repository.save(Alarm(hour = 7, minute = 0))
        muteRegistry.tryBegin(RingingSession("s-1", repository.get(id)!!))
        val viewModel = viewModel(id)
        viewModel.uiState.first { it.alarm != null }

        viewModel.onChallengeInteraction()
        assertTrue(muteCalls.isEmpty())
    }

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

    private val snoozable = AntiCheatPolicy(AntiCheatLevel.STANDARD, SnoozePolicy(allowed = true, minutes = 5, maxCount = 1))

    @Test
    fun snooze_isOfferedOnlyWhenPolicyAllowsIt() = runTest {
        val plain = repository.save(Alarm(hour = 7, minute = 0))
        val allowed = repository.save(Alarm(hour = 7, minute = 0, antiCheat = snoozable))
        assertFalse(viewModel(plain).uiState.first { it.alarm != null }.canSnooze)
        assertTrue(viewModel(allowed).uiState.first { it.alarm != null }.canSnooze)
    }

    @Test
    fun snooze_reschedulesSameSessionAndFinishes() = runTest {
        val id = repository.save(Alarm(hour = 7, minute = 0, antiCheat = snoozable))
        val vm = viewModel(id)
        vm.uiState.first { it.alarm != null }
        vm.snooze()
        assertEquals(listOf(id), cancelled)
        assertEquals(listOf("s-1"), snoozeCalls.map { it.second })
        assertTrue(vm.uiState.value.finished)
    }

    @Test
    fun snoozesLeft_reflectsLimitAndHidesSnoozeAtZero() = runTest {
        val id = repository.save(Alarm(hour = 7, minute = 0, antiCheat = snoozable))
        val vm = viewModel(id)
        assertEquals(1, vm.uiState.first { it.alarm != null }.snoozesLeft)
        counts["s-1"] = 1
        val exhausted = viewModel(id).uiState.first { it.alarm != null }
        assertEquals(0, exhausted.snoozesLeft)
        assertFalse(exhausted.canSnooze)
    }

    @Test
    fun snoozesLeft_isNullWhenUnlimited() = runTest {
        val unlimited = snoozable.copy(snooze = snoozable.snooze.copy(maxCount = SnoozePolicy.UNLIMITED))
        val id = repository.save(Alarm(hour = 7, minute = 0, antiCheat = unlimited))
        counts["s-1"] = 50
        val state = viewModel(id).uiState.first { it.alarm != null }
        assertEquals(null, state.snoozesLeft)
        assertTrue(state.canSnooze)
    }

    private val withChallenges = ChallengeConfig(count = 1, challenges = listOf(ChallengeSpec("math")))
    private val passedAlarms = PassedAlarms()

    private fun realGateViewModel(id: Long) = viewModel(id, gate = ChallengeDismissGate(passedAlarms))

    @Test
    fun alarmWithChallenges_cannotBeDismissedWithoutPassingThem() = runTest {
        val id = repository.save(Alarm(hour = 7, minute = 0, challenges = withChallenges))
        val vm = realGateViewModel(id)
        val state = vm.uiState.first { it.alarm != null }
        assertFalse(state.canDismiss)
        assertEquals(SessionConfig.from(withChallenges), state.challenges)
        vm.dismiss()
        assertTrue(cancelled.isEmpty())
        assertTrue(resolved.isEmpty())
        assertFalse(vm.uiState.value.finished)
    }

    @Test
    fun alarmWithChallenges_dismissesOncePassed() = runTest {
        val id = repository.save(Alarm(hour = 7, minute = 0, challenges = withChallenges))
        val vm = realGateViewModel(id)
        vm.uiState.first { it.alarm != null }
        passedAlarms.markPassed(id)
        vm.dismiss()
        assertEquals(listOf("s-1"), resolved)
        assertTrue(vm.uiState.value.finished)
    }

    @Test
    fun alarmWithoutChallenges_dismissesImmediatelyWithRealGate() = runTest {
        val id = repository.save(Alarm(hour = 7, minute = 0))
        val vm = realGateViewModel(id)
        val state = vm.uiState.first { it.alarm != null }
        assertTrue(state.canDismiss)
        assertNull(state.challenges)
        vm.dismiss()
        assertTrue(vm.uiState.value.finished)
    }

    @Test
    fun alarmWithChallenges_keepsSnoozeAvailable() = runTest {
        val id = repository.save(Alarm(hour = 7, minute = 0, challenges = withChallenges, antiCheat = snoozable))
        val vm = realGateViewModel(id)
        assertTrue(vm.uiState.first { it.alarm != null }.canSnooze)
        vm.snooze()
        assertEquals(listOf("s-1"), snoozeCalls.map { it.second })
    }

    @Test
    fun reopenedAfterProcessDeath_isNotPassed() = runTest {
        val id = repository.save(Alarm(hour = 7, minute = 0, challenges = withChallenges))
        // PassedAlarms es memoria de proceso: tras morir llega vacío y la pantalla vuelve a pedir los retos.
        val state = viewModel(id, gate = ChallengeDismissGate(PassedAlarms())).uiState.first { it.alarm != null }
        assertFalse(state.canDismiss)
        assertTrue(state.challenges != null)
    }
}
