package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AlarmRepository
import app.luxion.apexapp.domain.FireStateRepository
import app.luxion.apexapp.domain.WakeEventRecorder
import app.luxion.apexapp.domain.WakeEventType
import java.time.Clock
import java.time.ZonedDateTime
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

/**
 * Qué ocurre cuando llega la hora de una alarma: crear la sesión, iniciar el sonado y
 * reprogramar o desactivar la alarma, una sola vez por disparo.
 */
class AlarmFiringHandler @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val registry: RingingRegistry,
    private val launcher: RingingLauncher,
    private val notifier: AlarmNotifier,
    private val sessionIds: SessionIdGenerator,
    private val fireState: FireStateRepository,
    private val clock: Clock,
    private val recorder: WakeEventRecorder,
) {
    /**
     * Devuelve la sesión iniciada, o `null` si no hay nada que sonar (alarma inexistente, inactiva
     * o ya sonando). Con [sessionId] (re-disparo de un snooze) se reutiliza la sesión y no se
     * reprograma ni desactiva la alarma.
     */
    suspend fun onFire(alarmId: Long, sessionId: String? = null): RingingSession? {
        val isTest = alarmId == TEST_ALARM_ID
        val alarm = when {
            isTest -> ZonedDateTime.now(clock).let { Alarm(id = TEST_ALARM_ID, hour = it.hour, minute = it.minute) }
            sessionId == null -> repository.get(alarmId)?.takeIf { it.enabled }
            else -> repository.get(alarmId)
        } ?: return null

        val scheduledAt = fireState.get(alarm.id)?.scheduledAt
        val session = RingingSession(sessionId ?: sessionIds.next(), alarm)
        if (!registry.tryBegin(session)) return null

        // Primero el sonado: lo que venga después no debe poder impedirlo.
        if (!launcher.start(session)) {
            registry.end(session.sessionId)
            notifier.notify(alarm, session.sessionId)
        }
        // El registro nunca debe impedir ni retrasar el sonado ni la reprogramación.
        if (!isTest && sessionId == null) {
            runCatching {
                recorder.record(
                    sessionId = session.sessionId,
                    type = WakeEventType.RANG,
                    alarmId = alarm.id,
                    timestamp = clock.millis(),
                    scheduledAt = scheduledAt ?: clock.millis(),
                )
            }
            fireState.markFired(alarm.id, clock.millis())
            if (alarm.repeatDays.isEmpty()) {
                repository.save(alarm.copy(enabled = false))
                scheduler.cancel(alarm.id)
            } else {
                scheduler.schedule(alarm)
            }
        }
        return session
    }
}
