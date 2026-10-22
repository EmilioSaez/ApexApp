package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.WakeEventType
import app.luxion.apexapp.domain.challenge.SequentialWakeRecorder
import java.time.Clock
import javax.inject.Inject

/**
 * Registra COMPLETED cuando una alarma sonando se resuelve (no snooze ni fin por tiempo). Cubre las alarmas
 * sin retos, que se descartan directamente; en las que tienen retos duplica el COMPLETED del fin de sesión,
 * que es idempotente por sesión. La alarma de prueba no es historial.
 */
class ResolvedWakeListener @Inject constructor(
    private val recorder: SequentialWakeRecorder,
    private val clock: Clock,
) : RingingLifecycleListener {
    override fun onEnded(session: RingingSession, reason: EndReason) {
        if (reason != EndReason.RESOLVED || session.alarm.id == TEST_ALARM_ID) return
        recorder.record(session.sessionId, WakeEventType.COMPLETED, session.alarm.id, clock.millis())
    }
}
