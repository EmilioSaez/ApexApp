package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AlarmRepository
import java.util.UUID
import javax.inject.Inject

interface AlarmNotifier {
    /** Presenta la alarma que suena; `sessionId` identifica este disparo concreto. */
    fun notify(alarm: Alarm, sessionId: String)

    /** Retira la notificación de la alarma. */
    fun cancel(alarmId: Long)
}

/** Identificador único de cada disparo de alarma. */
interface SessionIdGenerator {
    fun next(): String
}

class UuidSessionIdGenerator @Inject constructor() : SessionIdGenerator {
    override fun next(): String = UUID.randomUUID().toString()
}

/** Qué ocurre cuando llega la hora de una alarma: notificar y reprogramar o desactivar. */
class AlarmFiringHandler @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val notifier: AlarmNotifier,
    private val sessionIds: SessionIdGenerator,
) {
    suspend fun onFire(alarmId: Long) {
        val alarm = repository.get(alarmId)?.takeIf { it.enabled } ?: return
        notifier.notify(alarm, sessionIds.next())
        if (alarm.repeatDays.isEmpty()) {
            repository.save(alarm.copy(enabled = false))
        } else {
            scheduler.schedule(alarm)
        }
    }
}
