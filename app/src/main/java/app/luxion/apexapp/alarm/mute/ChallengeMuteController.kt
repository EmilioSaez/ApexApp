package app.luxion.apexapp.alarm.mute

import app.luxion.apexapp.alarm.EndReason
import app.luxion.apexapp.alarm.RingingLifecycleListener
import app.luxion.apexapp.alarm.RingingRegistry
import app.luxion.apexapp.alarm.RingingSession
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Silencia o reactiva el audio de la alarma que suena. La implementación real es `AlarmSoundEngine`. */
fun interface RingMuter {
    fun setMuted(muted: Boolean)
}

/**
 * Aplica «silenciar mientras resuelvo los retos»: las interacciones de la pantalla de retos silencian
 * la alarma y, pasada la inactividad de su alarma, vuelve a sonar. Una [MuteStateMachine] por sesión;
 * un único bucle de espera por sesión silenciada (las interacciones solo refrescan el instante).
 */
class ChallengeMuteController(
    private val registry: RingingRegistry,
    private val muter: RingMuter,
    private val clock: Clock,
    private val scope: CoroutineScope,
) : RingingLifecycleListener {
    private class Entry(val machine: MuteStateMachine) {
        var watcher: Job? = null
    }

    private val entries = ConcurrentHashMap<String, Entry>()

    /** Toque o progreso del usuario en los retos de la sesión [sessionId]. */
    fun onInteraction(sessionId: String) {
        val session = registry.get(sessionId) ?: return
        if (!session.alarm.muteDuringChallenges) return
        val entry = entries.getOrPut(sessionId) {
            Entry(MuteStateMachine(session.alarm.muteInactivitySeconds * 1000L, clock))
        }
        if (!entry.machine.onInteraction()) return
        muter.setMuted(true)
        entry.watcher = scope.launch { watch(entry) }
    }

    private suspend fun watch(entry: Entry) {
        while (true) {
            delay(entry.machine.millisUntilRing() ?: return)
            if (entry.machine.poll()) {
                muter.setMuted(false)
                return
            }
        }
    }

    override fun onEnded(session: RingingSession, reason: EndReason) {
        entries.remove(session.sessionId)?.watcher?.cancel()
        // Al cerrarse una sesión el sonado pasa a la siguiente sin silenciar: se respeta su estado.
        val current = registry.active().lastOrNull() ?: return
        if (entries[current.sessionId]?.machine?.state == MuteStateMachine.State.MUTED) muter.setMuted(true)
    }
}
