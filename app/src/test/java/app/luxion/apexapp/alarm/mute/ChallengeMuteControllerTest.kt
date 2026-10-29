package app.luxion.apexapp.alarm.mute

import app.luxion.apexapp.alarm.EndReason
import app.luxion.apexapp.alarm.RingingRegistry
import app.luxion.apexapp.alarm.RingingSession
import app.luxion.apexapp.domain.Alarm
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeMuteControllerTest {
    private class Recorder : RingMuter {
        val calls = mutableListOf<Boolean>()
        override fun setMuted(muted: Boolean) {
            calls += muted
        }
    }

    private val registry = RingingRegistry()
    private val muter = Recorder()

    /** Reloj ligado al tiempo virtual de la prueba, igual que `delay`. */
    private fun TestScope.virtualClock() = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = Instant.ofEpochMilli(testScheduler.currentTime)
    }

    private fun TestScope.controller() = ChallengeMuteController(registry, muter, virtualClock(), backgroundScope)

    private fun begin(id: String, alarmId: Long = 1, mute: Boolean = true, seconds: Int = 30): RingingSession {
        val session = RingingSession(
            id,
            Alarm(id = alarmId, hour = 7, minute = 0, muteDuringChallenges = mute, muteInactivitySeconds = seconds),
        )
        registry.tryBegin(session)
        return session
    }

    @Test
    fun withoutInteraction_neverMutes() = runTest {
        val controller = controller()
        begin("s1")
        advanceTimeBy(5 * 60_000)
        runCurrent()
        assertEquals(emptyList<Boolean>(), muter.calls)
        controller.onInteraction("unknown")
        assertEquals(emptyList<Boolean>(), muter.calls)
    }

    @Test
    fun optionOff_ignoresInteraction() = runTest {
        val controller = controller()
        begin("s1", mute = false)
        controller.onInteraction("s1")
        assertEquals(emptyList<Boolean>(), muter.calls)
    }

    @Test
    fun interaction_mutes_andInactivityRingsAgain() = runTest {
        val controller = controller()
        begin("s1")
        controller.onInteraction("s1")
        assertEquals(listOf(true), muter.calls)

        advanceTimeBy(29_999)
        runCurrent()
        assertEquals(listOf(true), muter.calls)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(true, false), muter.calls)
    }

    @Test
    fun interactionsKeepItMuted_andOnlyTheLastOneCounts() = runTest {
        val controller = controller()
        begin("s1")
        controller.onInteraction("s1")
        advanceTimeBy(20_000)
        controller.onInteraction("s1")
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(listOf(true), muter.calls)

        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(listOf(true, false), muter.calls)
    }

    @Test
    fun interactionAfterRingingAgain_mutesAgain() = runTest {
        val controller = controller()
        begin("s1", seconds = 15)
        controller.onInteraction("s1")
        advanceTimeBy(15_000)
        runCurrent()
        controller.onInteraction("s1")
        assertEquals(listOf(true, false, true), muter.calls)

        advanceTimeBy(15_000)
        runCurrent()
        assertEquals(listOf(true, false, true, false), muter.calls)
    }

    @Test
    fun endingTheSession_cancelsThePendingUnmute() = runTest {
        val controller = controller()
        val session = begin("s1")
        controller.onInteraction("s1")
        registry.end("s1")
        controller.onEnded(session, EndReason.RESOLVED)

        advanceTimeBy(5 * 60_000)
        runCurrent()
        assertEquals(listOf(true), muter.calls)
    }

    @Test
    fun endingAnotherSession_keepsTheMutedOneSilenced() = runTest {
        val controller = controller()
        val other = begin("other", alarmId = 2, mute = false)
        begin("s1", alarmId = 1)
        controller.onInteraction("s1")
        registry.end("other")
        controller.onEnded(other, EndReason.SNOOZED)
        assertEquals(listOf(true, true), muter.calls)
    }
}
