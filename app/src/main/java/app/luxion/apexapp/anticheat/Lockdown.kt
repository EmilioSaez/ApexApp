package app.luxion.apexapp.anticheat

import app.luxion.apexapp.alarm.EndReason
import app.luxion.apexapp.alarm.RingingLifecycleListener
import app.luxion.apexapp.alarm.RingingSession
import app.luxion.apexapp.domain.WakeEventRecorder
import app.luxion.apexapp.domain.WakeEventType
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Lockdown activo: lo observa la pantalla de sonar para aplicar inmersivo, Atrás y lock task. */
data class ActiveLockdown(val sessionId: String, val alarmId: Long, val protection: Protection)

/** Alta/baja de la lista blanca de lock task; solo tiene efecto con device owner. */
interface LockTaskPolicy {
    fun allow()
    fun clear()
}

/** Sesión de sonado viva, persistida para detectar muerte del proceso o reinicio. */
interface ActiveSessionStore {
    fun put(sessionId: String, alarmId: Long)
    fun clear()
    fun current(): Pair<String, Long>?
}

/**
 * Aplica y libera el lockdown siguiendo el ciclo de vida del sonado de `alarm-reliability`.
 * Se libera únicamente en [onEnded]; no hay otra salida.
 */
class LockdownController(
    private val capabilities: CapabilityChecker,
    private val volume: VolumeLock,
    private val tracker: SessionIntegrityTracker,
    private val lockTaskPolicy: LockTaskPolicy,
    private val activeSession: ActiveSessionStore,
    private val snoozeCounts: SnoozeCounts,
    private val recorder: WakeEventRecorder,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val showRingScreen: (RingingSession) -> Unit = {},
) : RingingLifecycleListener {
    private val _active = MutableStateFlow<ActiveLockdown?>(null)
    val active: StateFlow<ActiveLockdown?> = _active.asStateFlow()

    override fun onStarted(session: RingingSession) {
        val protection = AntiCheatResolver.effective(session.alarm.antiCheat.level, capabilities.current())
        tracker.begin(session.sessionId)
        activeSession.put(session.sessionId, session.alarm.id)
        if (!protection.active) return
        if (protection.volumeLock) volume.engage(session.sessionId)
        if (protection.lockTask) lockTaskPolicy.allow()
        _active.value = ActiveLockdown(session.sessionId, session.alarm.id, protection)
        // Con el dispositivo en uso el full-screen intent solo muestra un aviso; con el permiso de
        // superposición la app puede abrir la pantalla de sonar directamente desde segundo plano.
        if (protection.foregroundRecovery) runCatching { showRingScreen(session) }
    }

    override fun onEnded(session: RingingSession, reason: EndReason) {
        release(session.sessionId)
        activeSession.clear()
        if (reason == EndReason.SNOOZED) return // la sesión continúa en el siguiente sonado
        snoozeCounts.clear(session.sessionId)
        val reasons = tracker.finish(session.sessionId, timedOut = reason == EndReason.TIMED_OUT)
        if (reasons.isNotEmpty()) flag(session.sessionId, session.alarm.id)
    }

    /** La pantalla de sonar salió del primer plano durante el lockdown. */
    fun exitAttempt(sessionId: String) = tracker.exitAttempt(sessionId)

    /** Un juego táctil descartó un toque sintético durante el sonado. */
    fun syntheticInput(sessionId: String) = tracker.syntheticInput(sessionId)

    fun volumeKeyPressed() = volume.keyPressed()

    /** Llamar al arrancar la app: una sesión viva sin sonado es una interrupción (proceso muerto o reinicio). */
    fun checkInterrupted() {
        val stale = activeSession.current() ?: return
        activeSession.clear()
        flag(stale.first, stale.second)
    }

    private fun release(sessionId: String) {
        if (_active.value?.sessionId != sessionId) return
        volume.release()
        lockTaskPolicy.clear()
        _active.value = null
    }

    private fun flag(sessionId: String, alarmId: Long) {
        scope.launch {
            recorder.record(sessionId, WakeEventType.SUSPICIOUS, alarmId, clock.millis())
        }
    }
}
