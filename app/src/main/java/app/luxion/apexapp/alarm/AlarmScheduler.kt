package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.FireStateRepository
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
    suspend fun schedule(alarm: Alarm): ScheduleResult
    suspend fun cancel(alarmId: Long)
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
    private val fireState: FireStateRepository,
) : AlarmScheduler {
    override suspend fun schedule(alarm: Alarm): ScheduleResult {
        val next = alarm.nextOccurrence(clock)
        if (next == null) {
            cancel(alarm.id)
            return ScheduleResult.NotScheduled
        }
        if (!gateway.canScheduleExact()) return ScheduleResult.PermissionMissing
        val at = next.toInstant().toEpochMilli()
        gateway.setAlarmClock(alarm.id, at)
        fireState.markScheduled(alarm.id, at)
        return ScheduleResult.Scheduled
    }

    override suspend fun cancel(alarmId: Long) {
        gateway.cancel(alarmId)
        fireState.markScheduled(alarmId, null)
    }
}

/** Alarma de prueba: sigue el camino real de disparo, sin guardarse en la base de datos. */
class AlarmTestScheduler @Inject constructor(
    private val gateway: AlarmClockGateway,
    private val clock: Clock,
) {
    fun schedule(delayMillis: Long = TEST_DELAY_MILLIS): ScheduleResult {
        if (!gateway.canScheduleExact()) return ScheduleResult.PermissionMissing
        gateway.setAlarmClock(TEST_ALARM_ID, clock.millis() + delayMillis)
        return ScheduleResult.Scheduled
    }

    companion object {
        const val TEST_DELAY_MILLIS = 10_000L
    }
}
