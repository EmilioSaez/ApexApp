package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AlarmRepository
import javax.inject.Inject

interface AlarmNotifier {
    fun notify(alarm: Alarm)
}

/** Qué ocurre cuando llega la hora de una alarma: notificar y reprogramar o desactivar. */
class AlarmFiringHandler @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val notifier: AlarmNotifier,
) {
    suspend fun onFire(alarmId: Long) {
        val alarm = repository.get(alarmId)?.takeIf { it.enabled } ?: return
        notifier.notify(alarm)
        if (alarm.repeatDays.isEmpty()) {
            repository.save(alarm.copy(enabled = false))
        } else {
            scheduler.schedule(alarm)
        }
    }
}
