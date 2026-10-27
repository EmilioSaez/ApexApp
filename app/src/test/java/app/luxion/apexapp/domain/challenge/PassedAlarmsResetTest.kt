package app.luxion.apexapp.domain.challenge

import app.luxion.apexapp.alarm.EndReason
import app.luxion.apexapp.alarm.RingingSession
import app.luxion.apexapp.domain.Alarm
import org.junit.Assert.assertFalse
import org.junit.Test

class PassedAlarmsResetTest {
    private val passed = PassedAlarms()
    private val reset = PassedAlarmsReset(passed)
    private val session = RingingSession("s-1", Alarm(id = 4, hour = 7, minute = 0))

    @Test
    fun startingAlarm_forgetsPreviousPass() {
        passed.markPassed(4)
        reset.onStarted(session)
        assertFalse(passed.isPassed(4))
    }

    @Test
    fun endingAlarm_forgetsPass_evenWhenSnoozed() {
        for (reason in EndReason.entries) {
            passed.markPassed(4)
            reset.onEnded(session, reason)
            assertFalse(passed.isPassed(4))
        }
    }
}
