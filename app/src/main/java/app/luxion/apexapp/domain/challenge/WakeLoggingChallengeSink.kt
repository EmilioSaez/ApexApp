package app.luxion.apexapp.domain.challenge

import app.luxion.apexapp.alarm.TEST_ALARM_ID
import app.luxion.apexapp.domain.WakeEventRecorder
import app.luxion.apexapp.domain.WakeEventType
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Entrega eventos al [WakeEventRecorder] (que es `suspend`) desde código síncrono, en orden y de uno en uno.
 * Un fallo del recorder (p. ej. de Room) se registra y no afecta a quien emite ni a los eventos siguientes.
 */
@Singleton
class SequentialWakeRecorder(
    private val recorder: WakeEventRecorder,
    scope: CoroutineScope,
) {
    @Inject
    constructor(recorder: WakeEventRecorder) : this(recorder, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (delivery in queue) {
                try {
                    delivery()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.log(Level.WARNING, "Wake event delivery failed", e)
                }
            }
        }
    }

    fun record(
        sessionId: String,
        type: WakeEventType,
        alarmId: Long,
        timestamp: Long,
        challengeType: String? = null,
        difficulty: Int? = null,
    ) {
        queue.trySend {
            recorder.record(
                sessionId = sessionId,
                type = type,
                alarmId = alarmId,
                timestamp = timestamp,
                challengeType = challengeType,
                difficulty = difficulty,
            )
        }
    }

    private companion object {
        val logger: Logger = Logger.getLogger("SequentialWakeRecorder")
    }
}

/**
 * Sink real de eventos de sesión: registra en el historial de despertar cada fallo de reto
 * (CHALLENGE_FAILED, con tipo y nivel) y el fin de sesión (COMPLETED). Las sesiones sin alarma o de la
 * alarma de prueba no son despertares y no se registran.
 */
class WakeLoggingChallengeSink @Inject constructor(
    private val recorder: SequentialWakeRecorder,
) : ChallengeEventSink {
    override fun onEvent(event: ChallengeEvent) {
        val alarmId = event.alarmId ?: return
        if (alarmId == TEST_ALARM_ID) return
        when (event) {
            is ChallengeEvent.ChallengeFailed -> recorder.record(
                sessionId = event.sessionId,
                type = WakeEventType.CHALLENGE_FAILED,
                alarmId = alarmId,
                timestamp = event.timestampMillis,
                challengeType = event.type.id,
                difficulty = event.level,
            )

            is ChallengeEvent.SessionFinished -> recorder.record(
                sessionId = event.sessionId,
                type = WakeEventType.COMPLETED,
                alarmId = alarmId,
                timestamp = event.timestampMillis,
            )

            else -> Unit
        }
    }
}
