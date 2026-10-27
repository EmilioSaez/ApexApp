package app.luxion.apexapp.anticheat

import app.luxion.apexapp.alarm.AlarmResolver
import app.luxion.apexapp.alarm.EndReason
import app.luxion.apexapp.alarm.RingingSession
import app.luxion.apexapp.data.ConfigJson
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AntiCheatLevel
import app.luxion.apexapp.domain.AntiCheatPolicy
import app.luxion.apexapp.domain.SnoozePolicy
import app.luxion.apexapp.domain.WakeEventRecorder
import app.luxion.apexapp.domain.WakeEventType
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeCapabilities(var caps: Capabilities = Capabilities()) : CapabilityChecker {
    override fun current() = caps
}

private class FakeVolume(var volume: Int = 3, val maxVolume: Int = 7) : AlarmVolumeGateway {
    var onChanged: (() -> Unit)? = null
    override fun get() = volume
    override fun max() = maxVolume
    override fun set(volume: Int) {
        this.volume = volume
    }

    override fun observe(onChanged: () -> Unit): () -> Unit {
        this.onChanged = onChanged
        return { this.onChanged = null }
    }
}

private class FakeActive : ActiveSessionStore {
    var session: Pair<String, Long>? = null
    override fun put(sessionId: String, alarmId: Long) {
        session = sessionId to alarmId
    }

    override fun clear() {
        session = null
    }

    override fun current() = session
}

private class FakeCounts : SnoozeCounts {
    val map = HashMap<String, Int>()
    override fun get(sessionId: String) = map[sessionId] ?: 0
    override fun set(sessionId: String, count: Int) {
        map[sessionId] = count
    }

    override fun clear(sessionId: String) {
        map.remove(sessionId)
    }
}

private class FakeScheduler : SnoozeScheduler {
    val scheduled = mutableListOf<Triple<Long, String, Long>>()
    override fun schedule(alarmId: Long, sessionId: String, triggerAtMillis: Long) {
        scheduled += Triple(alarmId, sessionId, triggerAtMillis)
    }
}

private class FakeRecorder : WakeEventRecorder {
    val events = mutableListOf<Pair<String, WakeEventType>>()
    override suspend fun record(
        sessionId: String,
        type: WakeEventType,
        alarmId: Long?,
        timestamp: Long,
        scheduledAt: Long?,
        challengeType: String?,
        difficulty: Int?,
    ) {
        events += sessionId to type
    }
}

private class FakeResolver : AlarmResolver {
    val resolved = mutableListOf<Pair<String, EndReason>>()
    override fun resolve(sessionId: String, reason: EndReason) {
        resolved += sessionId to reason
    }
}

private fun session(level: AntiCheatLevel, snooze: SnoozePolicy = SnoozePolicy(), id: String = "s1") =
    RingingSession(id, Alarm(id = 1, hour = 7, minute = 0, antiCheat = AntiCheatPolicy(level, snooze)))

class AntiCheatPolicyJsonTest {
    @Test
    fun emptyJson_isPermissive() {
        val policy = ConfigJson.decodeFromString<AntiCheatPolicy>("{}")
        assertEquals(AntiCheatLevel.OFF, policy.level)
        assertFalse(policy.snooze.allowed)
    }

    @Test
    fun unknownKeys_areIgnored() {
        val policy = ConfigJson.decodeFromString<AntiCheatPolicy>("""{"level":"STANDARD","future":1}""")
        assertEquals(AntiCheatLevel.STANDARD, policy.level)
    }

    @Test
    fun removedStrictLevel_decodesAsStandard() {
        val policy = ConfigJson.decodeFromString<AntiCheatPolicy>("""{"level":"STRICT"}""")
        assertEquals(AntiCheatLevel.STANDARD, policy.level)
    }

    @Test
    fun unknownLevel_decodesAsOff() {
        val policy = ConfigJson.decodeFromString<AntiCheatPolicy>("""{"level":"FUTURE"}""")
        assertEquals(AntiCheatLevel.OFF, policy.level)
    }

    @Test
    fun missingSnoozeKeys_useDefaults() {
        val policy = ConfigJson.decodeFromString<AntiCheatPolicy>("""{"snooze":{"allowed":true}}""")
        assertEquals(SnoozePolicy(allowed = true, minutes = 5, maxCount = 3), policy.snooze)
    }
}

class AntiCheatResolverTest {
    private val all = Capabilities(overlay = true, deviceAdmin = true)

    @Test
    fun off_appliesNothing() {
        assertEquals(Protection.NONE, AntiCheatResolver.effective(AntiCheatLevel.OFF, all))
    }

    @Test
    fun standard_withoutOverlay_degradesRecovery() {
        val p = AntiCheatResolver.effective(AntiCheatLevel.STANDARD, Capabilities())
        assertEquals(AntiCheatLevel.STANDARD, p.effective)
        assertTrue(p.immersive && p.blockBack && p.volumeLock)
        assertFalse(p.foregroundRecovery)
        assertTrue(p.degraded)
    }

    @Test
    fun standard_withOverlay_isNotDegraded() {
        val p = AntiCheatResolver.effective(AntiCheatLevel.STANDARD, Capabilities(overlay = true))
        assertTrue(p.foregroundRecovery)
        assertFalse(p.degraded)
    }

}

class SessionIntegrityTrackerTest {
    @Test
    fun cleanSession_isNotSuspicious() {
        val t = SessionIntegrityTracker()
        t.begin("a")
        assertTrue(t.finish("a", timedOut = false).isEmpty())
    }

    @Test
    fun repeatedExits_areFlagged() {
        val t = SessionIntegrityTracker()
        repeat(3) { t.exitAttempt("a") }
        assertEquals(setOf(SuspicionReason.REPEATED_EXIT), t.finish("a", false))
    }

    @Test
    fun fewExits_areTolerated() {
        val t = SessionIntegrityTracker()
        repeat(2) { t.exitAttempt("a") }
        assertTrue(t.finish("a", false).isEmpty())
    }

    @Test
    fun volumeTamperAndTimeout_areFlagged() {
        val t = SessionIntegrityTracker()
        repeat(3) { t.volumeReset("a") }
        assertEquals(setOf(SuspicionReason.VOLUME_TAMPER, SuspicionReason.TIMED_OUT), t.finish("a", true))
    }

    @Test
    fun finish_clearsState() {
        val t = SessionIntegrityTracker()
        t.interrupted("a")
        t.finish("a", false)
        assertTrue(t.finish("a", false).isEmpty())
    }
}

class VolumeLockTest {
    @Test
    fun engage_setsMaxAndReleaseRestores() {
        val gw = FakeVolume(volume = 3)
        val lock = VolumeLock(gw)
        lock.engage("a")
        assertEquals(7, gw.volume)
        lock.release()
        assertEquals(3, gw.volume)
    }

    @Test
    fun externalDrop_isReappliedAndReported() {
        val gw = FakeVolume(volume = 3)
        val tampers = mutableListOf<String>()
        val lock = VolumeLock(gw) { tampers += it }
        lock.engage("a")
        gw.volume = 2
        gw.onChanged!!.invoke()
        assertEquals(7, gw.volume)
        assertEquals(listOf("a"), tampers)
    }

    @Test
    fun ownChange_isNotReportedAsTamper() {
        val gw = FakeVolume(volume = 3)
        val tampers = mutableListOf<String>()
        val lock = VolumeLock(gw) { tampers += it }
        lock.engage("a")
        gw.onChanged!!.invoke()
        assertTrue(tampers.isEmpty())
    }

    @Test
    fun keyPress_keepsMaxAndCounts() {
        val gw = FakeVolume(volume = 3)
        val tampers = mutableListOf<String>()
        val lock = VolumeLock(gw) { tampers += it }
        lock.engage("a")
        lock.keyPressed()
        assertEquals(7, gw.volume)
        assertEquals(1, tampers.size)
    }

    @Test
    fun release_withoutEngage_doesNothing() {
        val gw = FakeVolume(volume = 3)
        VolumeLock(gw).release()
        assertEquals(3, gw.volume)
    }
}

class SnoozeControllerTest {
    private val clock = Clock.fixed(Instant.ofEpochMilli(1_000_000), ZoneOffset.UTC)
    private val resolver = FakeResolver()
    private val scheduler = FakeScheduler()
    private val counts = FakeCounts()
    private val recorder = FakeRecorder()
    private val controller = SnoozeController(resolver, scheduler, counts, recorder, clock)
    private val allowed = SnoozePolicy(allowed = true, minutes = 5, maxCount = 2)

    @Test
    fun notAllowed_isRejected() = runBlocking {
        val s = session(AntiCheatLevel.STANDARD)
        assertFalse(controller.canSnooze(s))
        assertFalse(controller.snooze(s))
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun snooze_reschedulesSameSessionAndRecordsEvent() = runBlocking {
        val s = session(AntiCheatLevel.STANDARD, allowed)
        assertTrue(controller.snooze(s))
        assertEquals(listOf(Triple(1L, "s1", 1_000_000L + 5 * 60_000L)), scheduler.scheduled)
        assertEquals(listOf("s1" to EndReason.SNOOZED), resolver.resolved)
        assertEquals(listOf("s1" to WakeEventType.SNOOZED), recorder.events)
    }

    @Test
    fun maxCount_stopsOfferingSnooze() = runBlocking {
        val s = session(AntiCheatLevel.STANDARD, allowed)
        assertTrue(controller.snooze(s))
        assertTrue(controller.snooze(s))
        assertFalse(controller.canSnooze(s))
        assertFalse(controller.snooze(s))
        assertEquals(2, scheduler.scheduled.size)
    }

    @Test
    fun finished_clearsCounter() = runBlocking {
        val s = session(AntiCheatLevel.STANDARD, allowed)
        controller.snooze(s)
        controller.finished("s1")
        assertEquals(0, counts.get("s1"))
    }
}

class LockdownControllerTest {
    private val clock = Clock.fixed(Instant.ofEpochMilli(5), ZoneOffset.UTC)
    private val caps = FakeCapabilities(Capabilities(overlay = true))
    private val volume = FakeVolume()
    private val tracker = SessionIntegrityTracker()
    private val active = FakeActive()
    private val recorder = FakeRecorder()
    private val counts = FakeCounts()
    private val controller = LockdownController(
        caps,
        VolumeLock(volume, tracker::volumeReset),
        tracker,
        active,
        counts,
        recorder,
        clock,
        CoroutineScope(Dispatchers.Unconfined),
        showRingScreen = { shown += it.sessionId },
    )
    private val shown = mutableListOf<String>()

    @Test
    fun standard_withOverlay_opensRingScreen() {
        controller.onStarted(session(AntiCheatLevel.STANDARD))
        assertEquals(listOf("s1"), shown)
    }

    @Test
    fun standard_withoutOverlay_doesNotOpenRingScreen() {
        caps.caps = Capabilities()
        controller.onStarted(session(AntiCheatLevel.STANDARD))
        assertTrue(shown.isEmpty())
    }

    @Test
    fun off_doesNotOpenRingScreen() {
        controller.onStarted(session(AntiCheatLevel.OFF))
        assertTrue(shown.isEmpty())
    }

    @Test
    fun off_doesNotLockdown() {
        controller.onStarted(session(AntiCheatLevel.OFF))
        assertNull(controller.active.value)
        assertEquals(3, volume.volume)
    }

    @Test
    fun standard_locksAndReleasesOnResolve() {
        val s = session(AntiCheatLevel.STANDARD)
        controller.onStarted(s)
        assertEquals(AntiCheatLevel.STANDARD, controller.active.value!!.protection.effective)
        assertEquals(7, volume.volume)
        controller.onEnded(s, EndReason.RESOLVED)
        assertNull(controller.active.value)
        assertEquals(3, volume.volume)
        assertNull(active.session)
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun timedOut_flagsSessionSuspicious() {
        val s = session(AntiCheatLevel.STANDARD)
        controller.onStarted(s)
        controller.onEnded(s, EndReason.TIMED_OUT)
        assertEquals(listOf("s1" to WakeEventType.SUSPICIOUS), recorder.events)
    }

    @Test
    fun repeatedExits_flagOnResolve() {
        val s = session(AntiCheatLevel.STANDARD)
        controller.onStarted(s)
        repeat(3) { controller.exitAttempt("s1") }
        controller.onEnded(s, EndReason.RESOLVED)
        assertEquals(listOf("s1" to WakeEventType.SUSPICIOUS), recorder.events)
    }

    @Test
    fun snooze_releasesLockdownWithoutFlagging() {
        val s = session(AntiCheatLevel.STANDARD)
        controller.onStarted(s)
        controller.onEnded(s, EndReason.SNOOZED)
        assertNull(controller.active.value)
        assertEquals(3, volume.volume)
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun staleSession_isFlaggedAsInterrupted() {
        active.put("old", 9)
        controller.checkInterrupted()
        assertEquals(listOf("old" to WakeEventType.SUSPICIOUS), recorder.events)
        assertNull(active.session)
    }

    @Test
    fun noStaleSession_flagsNothing() {
        controller.checkInterrupted()
        assertTrue(recorder.events.isEmpty())
    }
}
