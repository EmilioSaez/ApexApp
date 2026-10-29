package app.luxion.apexapp.anticheat

import app.luxion.apexapp.alarm.AlarmResolver
import app.luxion.apexapp.alarm.EndReason
import app.luxion.apexapp.alarm.RingingSession
import app.luxion.apexapp.domain.WakeEventRecorder
import app.luxion.apexapp.domain.WakeEventType
import java.time.Clock

/** Programa el re-disparo de la misma sesión; la implementación real usa `AlarmManager`. */
interface SnoozeScheduler {
    fun schedule(alarmId: Long, sessionId: String, triggerAtMillis: Long)
}

/** Número de snoozes por sesión; sobrevive a la muerte del proceso entre sonados. */
interface SnoozeCounts {
    fun get(sessionId: String): Int
    fun set(sessionId: String, count: Int)
    fun clear(sessionId: String)
}

class SnoozeController(
    private val resolver: AlarmResolver,
    private val scheduler: SnoozeScheduler,
    private val counts: SnoozeCounts,
    private val recorder: WakeEventRecorder,
    private val clock: Clock,
) {
    fun canSnooze(session: RingingSession): Boolean =
        session.alarm.antiCheat.snooze.canSnooze(counts.get(session.sessionId))

    /** Posposiciones que quedan en la sesión; `null` si no hay límite. */
    fun remaining(session: RingingSession): Int? =
        session.alarm.antiCheat.snooze.remaining(counts.get(session.sessionId))

    /** Pospone la sesión; devuelve `false` si la política no lo permite. */
    suspend fun snooze(session: RingingSession): Boolean {
        val policy = session.alarm.antiCheat.snooze
        val done = counts.get(session.sessionId)
        if (!policy.canSnooze(done)) return false
        val now = clock.millis()
        counts.set(session.sessionId, done + 1)
        scheduler.schedule(session.alarm.id, session.sessionId, now + policy.minutes * 60_000L)
        resolver.resolve(session.sessionId, EndReason.SNOOZED)
        recorder.record(
            sessionId = session.sessionId,
            type = WakeEventType.SNOOZED,
            alarmId = session.alarm.id,
            timestamp = now,
        )
        return true
    }

    /** Fin definitivo de la sesión: limpia el contador. */
    fun finished(sessionId: String) = counts.clear(sessionId)
}
