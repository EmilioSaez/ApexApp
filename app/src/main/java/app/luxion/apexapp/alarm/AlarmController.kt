package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AlarmRepository
import app.luxion.apexapp.domain.FireStateRepository
import app.luxion.apexapp.domain.WakeEventRecorder
import app.luxion.apexapp.domain.WakeEventType
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/** Mantiene la programación sincronizada con lo guardado: guardar, activar/desactivar y borrar. */
class AlarmController @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val fireState: FireStateRepository,
    private val firingHandler: AlarmFiringHandler,
    private val recorder: WakeEventRecorder,
    private val clock: Clock,
    private val awakeVerification: AwakeVerificationCanceller,
) {
    /** Guarda la alarma y la programa (o cancela si está inactiva). */
    suspend fun save(alarm: Alarm): ScheduleResult {
        val id = repository.save(alarm)
        if (!alarm.awakeVerification) awakeVerification.cancel(id)
        return scheduler.schedule(alarm.copy(id = id))
    }

    suspend fun setEnabled(id: Long, enabled: Boolean): ScheduleResult? {
        val alarm = repository.get(id) ?: return null
        if (!enabled) awakeVerification.cancel(id)
        return save(alarm.copy(enabled = enabled))
    }

    suspend fun delete(id: Long) {
        scheduler.cancel(id)
        awakeVerification.cancel(id)
        fireState.clear(id)
        repository.delete(id)
    }

    /**
     * Reprograma todas las alarmas activas (reinicio, cambio de hora o zona, actualización...).
     * Las que debieron sonar mientras no estaban programadas suenan ya si caen dentro de
     * [MISSED_WINDOW_MILLIS]; si no, se registran como perdidas.
     */
    suspend fun rescheduleAll() {
        val now = clock.millis()
        repository.getEnabled().forEach { alarm ->
            val state = fireState.get(alarm.id)
            val due = state?.scheduledAt
            val missed = due != null && due <= now && (state.lastFiredAt ?: Long.MIN_VALUE) < due
            when {
                !missed -> scheduler.schedule(alarm)
                now - due!! <= MISSED_WINDOW_MILLIS ->
                    if (firingHandler.onFire(alarm.id) == null) scheduler.schedule(alarm)

                else -> {
                    recorder.record(
                        sessionId = UUID.randomUUID().toString(),
                        type = WakeEventType.MISSED,
                        alarmId = alarm.id,
                        scheduledAt = due,
                    )
                    if (alarm.repeatDays.isEmpty()) {
                        repository.save(alarm.copy(enabled = false))
                        scheduler.cancel(alarm.id)
                    } else {
                        scheduler.schedule(alarm)
                    }
                }
            }
        }
    }

    companion object {
        /** Tolerancia para hacer sonar una alarma perdida en vez de descartarla. */
        const val MISSED_WINDOW_MILLIS = 10 * 60 * 1000L
    }
}
