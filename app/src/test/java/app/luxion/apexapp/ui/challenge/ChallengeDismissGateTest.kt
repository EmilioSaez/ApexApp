package app.luxion.apexapp.ui.challenge

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.ChallengeSessionFactory
import app.luxion.apexapp.domain.challenge.FixedDifficultyProvider
import app.luxion.apexapp.domain.challenge.MathChallenge
import app.luxion.apexapp.domain.challenge.NoOpChallengeEventSink
import app.luxion.apexapp.domain.challenge.PassedAlarms
import app.luxion.apexapp.domain.challenge.SessionConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeDismissGateTest {
    private val passed = PassedAlarms()
    private val gate = ChallengeDismissGate(passed)
    private val factory = ChallengeSessionFactory(
        ChallengeRegistry(listOf(MathChallenge())),
        FixedDifficultyProvider(),
        NoOpChallengeEventSink(),
        passed,
    )
    private val withChallenges = Alarm(
        id = 4,
        hour = 7,
        minute = 0,
        challenges = ChallengeConfig(challenges = listOf(ChallengeSpec("math"))),
    )

    @Test
    fun `alarm without challenges can be dismissed`() {
        assertTrue(gate.canDismiss(Alarm(id = 1, hour = 7, minute = 0)))
    }

    @Test
    fun `alarm with challenges cannot be dismissed before passing`() {
        assertFalse(gate.canDismiss(withChallenges))
    }

    @Test
    fun `alarm can be dismissed once its session is passed`() {
        val session = factory.create(
            SessionConfig(1, 1, ChallengeMode.CHAINED, withChallenges.challenges.challenges),
            sessionId = "s",
            alarmId = withChallenges.id,
        )
        session.submit(ChallengeAnswer.Text("wrong"))
        assertFalse(gate.canDismiss(withChallenges))
        val prompt = session.state.value.current!!.prompt.split(" ")
        val answer = if (prompt[1] == "+") prompt[0].toInt() + prompt[2].toInt() else prompt[0].toInt() - prompt[2].toInt()
        session.submit(ChallengeAnswer.Text(answer.toString()))
        assertTrue(gate.canDismiss(withChallenges))
    }

    @Test
    fun `clearing resets the gate for the next firing`() {
        passed.markPassed(withChallenges.id)
        passed.clear(withChallenges.id)
        assertFalse(gate.canDismiss(withChallenges))
    }
}
