package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.WakeEventType
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class StepClock(var now: Long) : Clock() {
    override fun getZone() = ZoneOffset.UTC
    override fun withZone(zone: java.time.ZoneId?) = this
    override fun instant(): Instant = Instant.ofEpochMilli(now)
}

private class FakeStore : AwakeVerificationStore {
    val map = mutableMapOf<Long, PendingVerification>()
    override fun get(alarmId: Long) = map[alarmId]
    override fun put(pending: PendingVerification) { map[pending.alarmId] = pending }
    override fun clear(alarmId: Long) { map.remove(alarmId) }
    override fun all() = map.values.toList()
}

private class FakeVerificationGateway : AwakeVerificationGateway {
    val scheduled = mutableMapOf<Pair<Long, VerificationPhase>, Long>()
    override fun schedule(alarmId: Long, phase: VerificationPhase, triggerAtMillis: Long) {
        scheduled[alarmId to phase] = triggerAtMillis
    }
    override fun cancel(alarmId: Long) {
        scheduled.keys.removeAll { it.first == alarmId }
    }
}

private class FakeVerificationNotifier(var canShow: Boolean = true) : AwakeVerificationNotifier {
    val shown = mutableMapOf<Long, Long>()
    override fun show(alarm: Alarm, deadlineAt: Long): Boolean {
        if (canShow) shown[alarm.id] = deadlineAt
        return canShow
    }
    override fun cancel(alarmId: Long) { shown.remove(alarmId) }
}

class AwakeVerifierTest {
    private val clock = StepClock(1_000_000L)
    private val repository = FakeAlarmRepository()
    private val store = FakeStore()
    private val gateway = FakeVerificationGateway()
    private val notifier = FakeVerificationNotifier()
    private val recorder = FakeWakeEventRecorder()
    private val rangAgain = mutableListOf<Long>()
    private val verifier = AwakeVerifier(
        repository, store, gateway, notifier,
        object : RingAgain {
            override suspend fun ringAgain(alarmId: Long): RingingSession? {
                rangAgain += alarmId
                return null
            }
        },
        recorder, clock,
    )

    private fun session(id: Long, verify: Boolean = true) =
        RingingSession("s$id", Alarm(id = id, hour = 7, minute = 0, awakeVerification = verify))

    private fun saved(verify: Boolean = true) = runBlocking {
        val id = repository.save(Alarm(hour = 7, minute = 0, awakeVerification = verify))
        id
    }

    private val prompt = AwakeVerificationPolicy.PROMPT_DELAY_MILLIS
    private val window = AwakeVerificationPolicy.CONFIRM_WINDOW_MILLIS

    @Test
    fun resolved_schedulesPromptFiveMinutesLater() {
        verifier.onResolved(session(1))
        assertEquals(clock.now + prompt, gateway.scheduled[1L to VerificationPhase.PROMPT])
        assertEquals(VerificationPhase.PROMPT, store.get(1)!!.phase)
        assertEquals("s1", store.get(1)!!.sessionId)
    }

    @Test
    fun resolved_withoutOption_orTestAlarm_doesNothing() {
        verifier.onResolved(session(1, verify = false))
        verifier.onResolved(session(TEST_ALARM_ID))
        assertTrue(gateway.scheduled.isEmpty())
        assertTrue(store.map.isEmpty())
    }

    @Test
    fun listener_onlyForResolvedReason() {
        val listener = AwakeVerificationListener(verifier)
        listener.onEnded(session(1), EndReason.SNOOZED)
        listener.onEnded(session(1), EndReason.TIMED_OUT)
        assertTrue(store.map.isEmpty())
        listener.onEnded(session(1), EndReason.RESOLVED)
        assertEquals(1, store.map.size)
    }

    @Test
    fun prompt_showsNotificationAndOpensConfirmWindow() = runBlocking {
        val id = saved()
        verifier.onResolved(session(id))
        clock.now += prompt
        verifier.onPrompt(id)
        assertEquals(clock.now + window, notifier.shown[id])
        assertEquals(clock.now + window, gateway.scheduled[id to VerificationPhase.CONFIRM])
        assertEquals(VerificationPhase.CONFIRM, store.get(id)!!.phase)
    }

    @Test
    fun prompt_withNotificationsBlocked_skipsVerificationWithoutRinging() = runBlocking {
        val id = saved()
        notifier.canShow = false
        verifier.onResolved(session(id))
        verifier.onPrompt(id)
        assertNull(store.get(id))
        assertTrue(gateway.scheduled.isEmpty())
        assertTrue(rangAgain.isEmpty())
    }

    @Test
    fun confirm_cancelsDeadlineAndNotification_andLogsVerified() = runBlocking {
        val id = saved()
        verifier.onResolved(session(id))
        verifier.onPrompt(id)
        verifier.onConfirm(id)
        assertNull(store.get(id))
        assertTrue(gateway.scheduled.isEmpty())
        assertTrue(notifier.shown.isEmpty())
        assertEquals(listOf(WakeEventType.VERIFIED), recorder.events.map { it.type })
        assertEquals("s$id", recorder.events.single().sessionId)
        // Un plazo que llegase tarde no hace sonar nada.
        verifier.onDeadline(id)
        assertTrue(rangAgain.isEmpty())
    }

    @Test
    fun confirm_withoutPending_logsNothing() = runBlocking {
        verifier.onConfirm(99)
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun deadline_withoutConfirmation_ringsAgainAndLogsFailure() = runBlocking {
        val id = saved()
        verifier.onResolved(session(id))
        verifier.onPrompt(id)
        verifier.onDeadline(id)
        assertEquals(listOf(id), rangAgain)
        assertTrue(notifier.shown.isEmpty())
        assertNull(store.get(id))
        assertEquals(listOf(WakeEventType.VERIFICATION_FAILED), recorder.events.map { it.type })
    }

    @Test
    fun deadline_beforePrompt_isIgnored() = runBlocking {
        val id = saved()
        verifier.onResolved(session(id))
        verifier.onDeadline(id)
        assertTrue(rangAgain.isEmpty())
        assertEquals(VerificationPhase.PROMPT, store.get(id)!!.phase)
    }

    @Test
    fun repeats_afterResolvingTheNewRing() = runBlocking {
        val id = saved()
        verifier.onResolved(session(id))
        verifier.onPrompt(id)
        verifier.onDeadline(id)
        clock.now += prompt + window
        verifier.onResolved(RingingSession("s-new", repository.get(id)!!))
        assertEquals(clock.now + prompt, gateway.scheduled[id to VerificationPhase.PROMPT])
        assertEquals("s-new", store.get(id)!!.sessionId)
    }

    @Test
    fun deletedAlarm_dropsAtPrompt_andAtDeadline() = runBlocking {
        val id = saved()
        verifier.onResolved(session(id))
        repository.delete(id)
        verifier.onPrompt(id)
        assertNull(store.get(id))

        val other = saved()
        verifier.onResolved(session(other))
        verifier.onPrompt(other)
        repository.delete(other)
        verifier.onDeadline(other)
        assertTrue(rangAgain.isEmpty())
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun optionRemoved_dropsAtPrompt() = runBlocking {
        val id = saved()
        verifier.onResolved(session(id))
        repository.save(repository.get(id)!!.copy(awakeVerification = false))
        verifier.onPrompt(id)
        assertNull(store.get(id))
        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun disabledOneShotAlarm_stillRingsAgain() = runBlocking {
        val id = saved()
        repository.save(repository.get(id)!!.copy(enabled = false))
        verifier.onResolved(session(id))
        verifier.onPrompt(id)
        verifier.onDeadline(id)
        assertEquals(listOf(id), rangAgain)
    }

    @Test
    fun cancel_clearsEverything() = runBlocking {
        val id = saved()
        verifier.onResolved(session(id))
        verifier.onPrompt(id)
        verifier.cancel(id)
        assertNull(store.get(id))
        assertTrue(gateway.scheduled.isEmpty())
        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun recorderFailure_doesNotPreventRingingAgain() = runBlocking {
        val id = saved()
        recorder.failWith = IllegalStateException("db")
        verifier.onResolved(session(id))
        verifier.onPrompt(id)
        verifier.onDeadline(id)
        assertEquals(listOf(id), rangAgain)
    }

    @Test
    fun restoreAll_reschedulesPendingAndDropsStale() {
        store.put(PendingVerification(1, "a", VerificationPhase.PROMPT, clock.now + 60_000))
        store.put(PendingVerification(2, "b", VerificationPhase.CONFIRM, clock.now - 30_000))
        store.put(PendingVerification(3, "c", VerificationPhase.PROMPT, clock.now - AwakeVerificationPolicy.STALE_AFTER_MILLIS - 1))
        verifier.restoreAll()
        assertEquals(clock.now + 60_000, gateway.scheduled[1L to VerificationPhase.PROMPT])
        assertEquals(clock.now, gateway.scheduled[2L to VerificationPhase.CONFIRM])
        assertNull(store.get(3))
        assertTrue((3L to VerificationPhase.PROMPT) !in gateway.scheduled)
    }
}

class AwakeVerificationPolicyTest {
    private val pending = PendingVerification(1, "s", VerificationPhase.PROMPT, dueAt = 10_000)

    @Test
    fun timings() {
        assertEquals(300_000L, AwakeVerificationPolicy.PROMPT_DELAY_MILLIS)
        assertEquals(120_000L, AwakeVerificationPolicy.CONFIRM_WINDOW_MILLIS)
        assertEquals(1_300_000L, AwakeVerificationPolicy.promptAt(1_000_000))
        assertEquals(1_120_000L, AwakeVerificationPolicy.deadlineAt(1_000_000))
    }

    @Test
    fun restore_futureKeepsInstant_pastRunsNow_staleDrops() {
        assertEquals(VerificationRestore.Schedule(10_000), AwakeVerificationPolicy.restore(pending, now = 5_000))
        assertEquals(VerificationRestore.Schedule(20_000), AwakeVerificationPolicy.restore(pending, now = 20_000))
        val stale = 10_000 + AwakeVerificationPolicy.STALE_AFTER_MILLIS
        assertEquals(VerificationRestore.Schedule(stale), AwakeVerificationPolicy.restore(pending, now = stale))
        assertEquals(VerificationRestore.Drop, AwakeVerificationPolicy.restore(pending, now = stale + 1))
    }
}
