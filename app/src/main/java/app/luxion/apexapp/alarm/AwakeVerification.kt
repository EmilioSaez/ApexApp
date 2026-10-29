package app.luxion.apexapp.alarm

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AlarmRepository
import app.luxion.apexapp.domain.WakeEventRecorder
import app.luxion.apexapp.domain.WakeEventType
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Fase de una verificación pendiente: esperando a mostrar el aviso, o esperando la confirmación. */
enum class VerificationPhase { PROMPT, CONFIRM }

/** Verificación pendiente de una alarma; `sessionId` es la sesión resuelta que la originó. */
data class PendingVerification(
    val alarmId: Long,
    val sessionId: String,
    val phase: VerificationPhase,
    val dueAt: Long,
)

/** Qué hacer con una verificación pendiente al restaurar la programación (reinicio, cambio de hora...). */
sealed interface VerificationRestore {
    data class Schedule(val at: Long) : VerificationRestore
    data object Drop : VerificationRestore
}

object AwakeVerificationPolicy {
    /** Tiempo entre resolver la alarma y mostrar el aviso. */
    const val PROMPT_DELAY_MILLIS = 5 * 60 * 1000L

    /** Plazo para pulsar «Estoy despierto» antes de que la alarma vuelva a sonar. */
    const val CONFIRM_WINDOW_MILLIS = 2 * 60 * 1000L

    /** Una verificación vencida hace más de esto al restaurar ya no tiene sentido (móvil apagado mucho tiempo). */
    const val STALE_AFTER_MILLIS = 15 * 60 * 1000L

    fun promptAt(resolvedAt: Long) = resolvedAt + PROMPT_DELAY_MILLIS

    fun deadlineAt(promptedAt: Long) = promptedAt + CONFIRM_WINDOW_MILLIS

    fun restore(pending: PendingVerification, now: Long): VerificationRestore = when {
        now - pending.dueAt > STALE_AFTER_MILLIS -> VerificationRestore.Drop
        else -> VerificationRestore.Schedule(maxOf(pending.dueAt, now))
    }
}

/** Estado persistido de las verificaciones pendientes; sobrevive a la muerte del proceso y al reinicio. */
interface AwakeVerificationStore {
    fun get(alarmId: Long): PendingVerification?
    fun put(pending: PendingVerification)
    fun clear(alarmId: Long)
    fun all(): List<PendingVerification>
}

/** Programa el aviso y el plazo de la verificación; la implementación real usa `AlarmManager`. */
interface AwakeVerificationGateway {
    fun schedule(alarmId: Long, phase: VerificationPhase, triggerAtMillis: Long)
    fun cancel(alarmId: Long)
}

interface AwakeVerificationNotifier {
    /** Muestra el aviso; `false` si no se pudo (notificaciones bloqueadas), y entonces no hay nada que confirmar. */
    fun show(alarm: Alarm, deadlineAt: Long): Boolean
    fun cancel(alarmId: Long)
}

/** Hace sonar de nuevo una alarma en una sesión nueva, sin tocar su programación. */
interface RingAgain {
    suspend fun ringAgain(alarmId: Long): RingingSession?
}

/** Cancela la verificación pendiente de una alarma (programación, aviso y estado). */
interface AwakeVerificationCanceller {
    fun cancel(alarmId: Long)
}

/**
 * Verificar que el usuario sigue despierto: al resolver una alarma con la opción activa se avisa a los
 * 5 min; si no confirma en 2 min, la alarma vuelve a sonar con todos sus retos y se repite hasta confirmar.
 */
@Singleton
class AwakeVerifier @Inject constructor(
    private val repository: AlarmRepository,
    private val store: AwakeVerificationStore,
    private val gateway: AwakeVerificationGateway,
    private val notifier: AwakeVerificationNotifier,
    private val ringAgain: RingAgain,
    private val recorder: WakeEventRecorder,
    private val clock: Clock,
) : AwakeVerificationCanceller {

    /** Una sesión terminó como resuelta: programa el aviso si su alarma lo pide. */
    fun onResolved(session: RingingSession) {
        val alarm = session.alarm
        if (!alarm.awakeVerification || alarm.id == TEST_ALARM_ID) return
        val at = AwakeVerificationPolicy.promptAt(clock.millis())
        store.put(PendingVerification(alarm.id, session.sessionId, VerificationPhase.PROMPT, at))
        gateway.schedule(alarm.id, VerificationPhase.PROMPT, at)
    }

    /** Vence el aviso: lo muestra y abre el plazo de confirmación. */
    suspend fun onPrompt(alarmId: Long) {
        val pending = store.get(alarmId)?.takeIf { it.phase == VerificationPhase.PROMPT } ?: return
        val alarm = repository.get(alarmId)?.takeIf { it.awakeVerification }
        if (alarm == null) {
            cancel(alarmId)
            return
        }
        val deadline = AwakeVerificationPolicy.deadlineAt(clock.millis())
        // Sin notificación no hay forma de confirmar: se omite en vez de forzar un sonado inevitable.
        if (!notifier.show(alarm, deadline)) {
            cancel(alarmId)
            return
        }
        store.put(pending.copy(phase = VerificationPhase.CONFIRM, dueAt = deadline))
        gateway.schedule(alarmId, VerificationPhase.CONFIRM, deadline)
    }

    /** El usuario pulsó «Estoy despierto». */
    suspend fun onConfirm(alarmId: Long) {
        val pending = store.get(alarmId)
        cancel(alarmId)
        // Sin estado (ya confirmada, caducada o cancelada) no hay nada que registrar.
        if (pending != null) record(pending, WakeEventType.VERIFIED)
    }

    /** Vence el plazo: si nadie confirmó, la alarma vuelve a sonar. */
    suspend fun onDeadline(alarmId: Long) {
        val pending = store.get(alarmId)?.takeIf { it.phase == VerificationPhase.CONFIRM } ?: return
        cancel(alarmId)
        val alarm = repository.get(alarmId)?.takeIf { it.awakeVerification } ?: return
        record(pending, WakeEventType.VERIFICATION_FAILED)
        ringAgain.ringAgain(alarm.id)
    }

    /** Reprograma las verificaciones pendientes tras reinicio, cambio de hora o actualización. */
    fun restoreAll() {
        val now = clock.millis()
        store.all().forEach { pending ->
            when (val action = AwakeVerificationPolicy.restore(pending, now)) {
                is VerificationRestore.Schedule -> {
                    gateway.schedule(pending.alarmId, pending.phase, action.at)
                    if (action.at != pending.dueAt) store.put(pending.copy(dueAt = action.at))
                }
                VerificationRestore.Drop -> cancel(pending.alarmId)
            }
        }
    }

    override fun cancel(alarmId: Long) {
        store.clear(alarmId)
        gateway.cancel(alarmId)
        notifier.cancel(alarmId)
    }

    // El registro nunca debe impedir confirmar ni volver a sonar.
    private suspend fun record(pending: PendingVerification, type: WakeEventType) {
        runCatching {
            recorder.record(
                sessionId = pending.sessionId,
                type = type,
                alarmId = pending.alarmId,
                timestamp = clock.millis(),
            )
        }
    }
}

/** Programa la verificación cuando una sesión se resuelve. */
class AwakeVerificationListener @Inject constructor(
    private val verifier: AwakeVerifier,
) : RingingLifecycleListener {
    override fun onEnded(session: RingingSession, reason: EndReason) {
        if (reason == EndReason.RESOLVED) verifier.onResolved(session)
    }
}
