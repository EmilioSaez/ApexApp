package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AlarmRepository
import javax.inject.Inject

/** Mantiene la programación sincronizada con lo guardado: guardar, activar/desactivar y borrar. */
class AlarmController @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
) {
    /** Guarda la alarma y la programa (o cancela si está inactiva). */
    suspend fun save(alarm: Alarm): ScheduleResult {
        val id = repository.save(alarm)
        return scheduler.schedule(alarm.copy(id = id))
    }

    suspend fun setEnabled(id: Long, enabled: Boolean): ScheduleResult? {
        val alarm = repository.get(id) ?: return null
        return save(alarm.copy(enabled = enabled))
    }

    suspend fun delete(id: Long) {
        scheduler.cancel(id)
        repository.delete(id)
    }

    /** Reprograma todas las alarmas activas, p. ej. tras reiniciar el dispositivo. */
    suspend fun rescheduleAll() {
        repository.getEnabled().forEach { scheduler.schedule(it) }
    }
}
