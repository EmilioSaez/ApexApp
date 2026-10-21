package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.nextOccurrence
import java.time.Clock
import javax.inject.Inject

enum class ScheduleResult {
    Scheduled,

    /** La alarma está inactiva: no hay nada programado. */
    NotScheduled,

    /** Falta el permiso de alarmas exactas. */
    PermissionMissing,
}

interface AlarmScheduler {
    fun schedule(alarm: Alarm): ScheduleResult
    fun cancel(alarmId: Long)
}

/** Acceso mínimo a `AlarmManager`, tras una interfaz para poder testear el scheduler sin Android. */
interface AlarmClockGateway {
    fun canScheduleExact(): Boolean
    fun setAlarmClock(alarmId: Long, triggerAtMillis: Long)
    fun cancel(alarmId: Long)
}

class AlarmClockScheduler @Inject constructor(
    private val gateway: AlarmClockGateway,
    private val clock: Clock,
) : AlarmScheduler {
    override fun schedule(alarm: Alarm): ScheduleResult {
        val next = alarm.nextOccurrence(clock)
        if (next == null) {
            gateway.cancel(alarm.id)
            return ScheduleResult.NotScheduled
        }
        if (!gateway.canScheduleExact()) return ScheduleResult.PermissionMissing
        gateway.setAlarmClock(alarm.id, next.toInstant().toEpochMilli())
        return ScheduleResult.Scheduled
    }

    override fun cancel(alarmId: Long) = gateway.cancel(alarmId)
}
