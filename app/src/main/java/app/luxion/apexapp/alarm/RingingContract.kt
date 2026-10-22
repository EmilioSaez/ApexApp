package app.luxion.apexapp.alarm

import android.content.Intent
import app.luxion.apexapp.domain.Alarm
import java.util.concurrent.ConcurrentHashMap

/**
 * Contrato de enganche del sonado. [AlarmRingingService] es el único servicio de sonado:
 * alarm-sound, anti-cheat y alarm-core se acoplan con estas interfaces y no crean servicios propios.
 */
data class RingingSession(val sessionId: String, val alarm: Alarm)

enum class EndReason { RESOLVED, SNOOZED, TIMED_OUT }

/** Audio del sonado. El binding por defecto es [SystemToneRinger]; alarm-sound lo sustituye. */
interface AlarmRinger {
    fun start(session: RingingSession)
    fun stop()
}

/** Intent que abre la pantalla de sonar desde la notificación de pantalla completa. */
interface RingScreenLauncher {
    fun intentFor(session: RingingSession): Intent
}

/** Termina una sesión: detiene audio, notificación y servicio, y avisa a los observadores. */
interface AlarmResolver {
    fun resolve(sessionId: String, reason: EndReason = EndReason.RESOLVED)
}

/** Observa el ciclo de vida del sonado. Se inyecta como `Set` (multibinding, vacío por defecto). */
interface RingingLifecycleListener {
    fun onStarted(session: RingingSession) = Unit
    fun onEnded(session: RingingSession, reason: EndReason) = Unit
}

/** Arranca el servicio de sonado; devuelve `false` si el sistema no lo permite. */
interface RingingLauncher {
    fun start(session: RingingSession): Boolean
}

/** Sesiones de sonado activas, a lo sumo una por alarma. */
class RingingRegistry @javax.inject.Inject constructor() {
    private val sessions = ConcurrentHashMap<String, RingingSession>()

    /** `false` si esa alarma ya está sonando. */
    @Synchronized
    fun tryBegin(session: RingingSession): Boolean {
        if (sessions.values.any { it.alarm.id == session.alarm.id }) return false
        sessions[session.sessionId] = session
        return true
    }

    fun get(sessionId: String): RingingSession? = sessions[sessionId]

    fun end(sessionId: String): RingingSession? = sessions.remove(sessionId)

    fun isEmpty(): Boolean = sessions.isEmpty()

    fun active(): List<RingingSession> = sessions.values.toList()
}

const val TEST_ALARM_ID = Long.MAX_VALUE
