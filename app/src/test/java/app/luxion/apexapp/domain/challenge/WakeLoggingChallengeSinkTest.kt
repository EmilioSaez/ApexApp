package app.luxion.apexapp.domain.challenge

import app.luxion.apexapp.alarm.EndReason
import app.luxion.apexapp.alarm.ResolvedWakeListener
import app.luxion.apexapp.alarm.RingingSession
import app.luxion.apexapp.alarm.TEST_ALARM_ID
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.WakeEventRecorder
import app.luxion.apexapp.domain.WakeEventType
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private data class Recorded(
    val sessionId: String,
    val type: WakeEventType,
    val alarmId: Long?,
    val timestamp: Long,
    val challengeType: String?,
    val difficulty: Int?,
)

private class FakeRecorder(private val failFirst: Boolean = false) : WakeEventRecorder {
    val events = mutableListOf<Recorded>()
    private var calls = 0

    override suspend fun record(
        sessionId: String,
        type: WakeEventType,
        alarmId: Long?,
        timestamp: Long,
        scheduledAt: Long?,
        challengeType: String?,
        difficulty: Int?,
    ) {
        if (failFirst && calls++ == 0) error("room down")
        events += Recorded(sessionId, type, alarmId, timestamp, challengeType, difficulty)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WakeLoggingChallengeSinkTest {
    private val scope = TestScope(UnconfinedTestDispatcher())
    private val math = ChallengeType.MATH

    @After
    fun tearDown() = scope.cancel()

    private fun sinkWith(recorder: WakeEventRecorder) = WakeLoggingChallengeSink(SequentialWakeRecorder(recorder, scope))

    @Test
    fun `failed challenge is recorded with type and level, finished session as completed`() {
        val recorder = FakeRecorder()
        val sink = sinkWith(recorder)
        sink.onEvent(ChallengeEvent.ChallengeFailed("s", 3L, 10L, math, 4, 0))
        sink.onEvent(ChallengeEvent.SessionFinished("s", 3L, 20L, 1))
        assertEquals(
            listOf(
                Recorded("s", WakeEventType.CHALLENGE_FAILED, 3L, 10L, "math", 4),
                Recorded("s", WakeEventType.COMPLETED, 3L, 20L, null, null),
            ),
            recorder.events,
        )
    }

    @Test
    fun `other events, null alarm and test alarm are ignored`() {
        val recorder = FakeRecorder()
        val sink = sinkWith(recorder)
        sink.onEvent(ChallengeEvent.SessionStarted("s", 3L, 1L, 1, 3))
        sink.onEvent(ChallengeEvent.ChallengeStarted("s", 3L, 2L, math, 1, 0))
        sink.onEvent(ChallengeEvent.ChallengeSolved("s", 3L, 3L, math, 1, 0))
        sink.onEvent(ChallengeEvent.ChallengeFellBack("s", 3L, 4L, ChallengeType("qr"), math, "no camera", 0))
        sink.onEvent(ChallengeEvent.ChallengeFailed("s", null, 10L, math, 1, 0))
        sink.onEvent(ChallengeEvent.SessionFinished("s", null, 20L, 1))
        sink.onEvent(ChallengeEvent.ChallengeFailed("s", TEST_ALARM_ID, 10L, math, 1, 0))
        sink.onEvent(ChallengeEvent.SessionFinished("s", TEST_ALARM_ID, 20L, 1))
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun `a failing recorder does not stop later events nor the caller`() {
        val recorder = FakeRecorder(failFirst = true)
        val sink = sinkWith(recorder)
        sink.onEvent(ChallengeEvent.ChallengeFailed("s", 3L, 10L, math, 1, 0))
        sink.onEvent(ChallengeEvent.SessionFinished("s", 3L, 20L, 1))
        assertEquals(listOf(WakeEventType.COMPLETED), recorder.events.map { it.type })
    }

    @Test
    fun `a real session records each failure and the completion once, in order`() {
        val recorder = FakeRecorder()
        val factory = ChallengeSessionFactory(
            ChallengeRegistry(listOf(MathChallenge())),
            FixedDifficultyProvider(),
            sinkWith(recorder),
            PassedAlarms(),
        )
        val config = SessionConfig(2, 1, ChallengeMode.CHAINED, listOf(ChallengeSpec("math")))
        val session = factory.create(config, "sess", 5L)
        session.submit(ChallengeAnswer.Text("x"))
        session.submit(ChallengeAnswer.Text("y"))
        val prompt = session.state.value.current!!.prompt.split(" ")
        val result = if (prompt[1] == "+") prompt[0].toInt() + prompt[2].toInt() else prompt[0].toInt() - prompt[2].toInt()
        session.submit(ChallengeAnswer.Text(result.toString()))
        assertEquals(
            listOf(WakeEventType.CHALLENGE_FAILED, WakeEventType.CHALLENGE_FAILED, WakeEventType.COMPLETED),
            recorder.events.map { it.type },
        )
        assertTrue(recorder.events.all { it.sessionId == "sess" && it.alarmId == 5L })
        assertEquals(2, recorder.events.first().difficulty)
    }

    @Test
    fun `resolved alarm without challenges is recorded as completed, other endings are not`() {
        val recorder = FakeRecorder()
        val clock = Clock.fixed(Instant.ofEpochMilli(777L), ZoneOffset.UTC)
        val listener = ResolvedWakeListener(SequentialWakeRecorder(recorder, scope), clock)
        val session = RingingSession("r1", Alarm(id = 8, hour = 7, minute = 0))
        listener.onEnded(session, EndReason.SNOOZED)
        listener.onEnded(session, EndReason.TIMED_OUT)
        listener.onEnded(RingingSession("t", Alarm(id = TEST_ALARM_ID, hour = 7, minute = 0)), EndReason.RESOLVED)
        assertTrue(recorder.events.isEmpty())
        listener.onEnded(session, EndReason.RESOLVED)
        assertEquals(listOf(Recorded("r1", WakeEventType.COMPLETED, 8L, 777L, null, null)), recorder.events)
    }
}
