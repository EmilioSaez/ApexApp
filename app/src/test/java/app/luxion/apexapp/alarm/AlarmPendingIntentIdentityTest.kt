package app.luxion.apexapp.alarm

import org.junit.Assert.assertNotEquals
import org.junit.Test

class AlarmPendingIntentIdentityTest {
    @Test
    fun snoozeDoesNotShareThePendingIntentOfTheRegularFire() {
        // Mismo data = mismo PendingIntent: el snooze pisaría la alarma ya reprogramada para mañana.
        assertNotEquals(
            AndroidAlarmClockGateway.fireData(1),
            AndroidAlarmClockGateway.snoozeData(1),
        )
    }
}
