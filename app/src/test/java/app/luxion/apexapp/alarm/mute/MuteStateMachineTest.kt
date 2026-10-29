package app.luxion.apexapp.alarm.mute

import app.luxion.apexapp.alarm.mute.MuteStateMachine.State
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

internal class MutableClock(var nowMillis: Long = 1_000L) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = Instant.ofEpochMilli(nowMillis)
    fun advance(millis: Long) {
        nowMillis += millis
    }
}

class MuteStateMachineTest {
    private val clock = MutableClock()
    private val machine = MuteStateMachine(timeoutMillis = 30_000, clock = clock)

    @Test
    fun startsRingingAndWithoutInteractionNeverMutes() {
        assertEquals(State.RINGING, machine.state)
        clock.advance(10 * 60_000)
        assertFalse(machine.poll())
        assertEquals(State.RINGING, machine.state)
        assertNull(machine.millisUntilRing())
    }

    @Test
    fun firstInteractionMutes() {
        assertTrue(machine.onInteraction())
        assertEquals(State.MUTED, machine.state)
        assertEquals(30_000L, machine.millisUntilRing())
    }

    @Test
    fun interactionWhileMutedDoesNotReportAChange() {
        machine.onInteraction()
        assertFalse(machine.onInteraction())
        assertEquals(State.MUTED, machine.state)
    }

    @Test
    fun staysMutedBeforeTimeoutAndRingsAtTimeout() {
        machine.onInteraction()
        clock.advance(29_999)
        assertFalse(machine.poll())
        assertEquals(State.MUTED, machine.state)
        assertEquals(1L, machine.millisUntilRing())

        clock.advance(1)
        assertTrue(machine.poll())
        assertEquals(State.RINGING, machine.state)
        assertNull(machine.millisUntilRing())
    }

    @Test
    fun interactionRestartsTheCountdown() {
        machine.onInteraction()
        clock.advance(20_000)
        machine.onInteraction()
        clock.advance(20_000)
        assertFalse(machine.poll())
        assertEquals(10_000L, machine.millisUntilRing())

        clock.advance(10_000)
        assertTrue(machine.poll())
    }

    @Test
    fun mutesAgainAfterRingingAgain() {
        machine.onInteraction()
        clock.advance(30_000)
        assertTrue(machine.poll())

        assertTrue(machine.onInteraction())
        assertEquals(State.MUTED, machine.state)
        assertEquals(30_000L, machine.millisUntilRing())
    }

    @Test
    fun pollWhileRingingDoesNothing() {
        assertFalse(machine.poll())
        assertEquals(State.RINGING, machine.state)
    }

    @Test
    fun nonPositiveTimeoutIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { MuteStateMachine(0, clock) }
    }
}
