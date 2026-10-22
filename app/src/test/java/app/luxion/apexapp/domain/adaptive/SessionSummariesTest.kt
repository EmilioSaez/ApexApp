package app.luxion.apexapp.domain.adaptive

import app.luxion.apexapp.domain.WakeSession
import app.luxion.apexapp.domain.WakeSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionSummariesTest {
    private fun session(
        id: String,
        alarmId: Long? = 1,
        status: WakeSessionStatus = WakeSessionStatus.COMPLETED,
        suspicious: Boolean = false,
    ) = WakeSession(
        sessionId = id,
        alarmId = alarmId,
        scheduledAt = 0,
        firedAt = 1_000,
        snoozeCount = 1,
        failCount = 2,
        completedAt = if (status == WakeSessionStatus.COMPLETED) 41_000 else null,
        suspicious = suspicious,
        status = status,
        challengeTypes = emptySet(),
    )

    @Test
    fun completedSessionKeepsSignals() {
        assertEquals(SessionSummary("a", 1, 2, 40_000), session("a").toSummaryOrNull(1))
    }

    @Test
    fun unreliableSessionsAreIgnored() {
        assertNull(session("a", status = WakeSessionStatus.NOT_COMPLETED).toSummaryOrNull(1))
        assertNull(session("a", status = WakeSessionStatus.PENDING).toSummaryOrNull(1))
        assertNull(session("a", suspicious = true).toSummaryOrNull(1))
        assertNull(session("a", alarmId = 2).toSummaryOrNull(1))
        assertNull(session("a", alarmId = null).toSummaryOrNull(1))
    }

    @Test
    fun listKeepsOrderAndDropsUnreliable() {
        val list = listOf(session("c"), session("b", suspicious = true), session("a"))
        assertEquals(listOf("c", "a"), list.toSummaries(1).map { it.sessionId })
    }
}
