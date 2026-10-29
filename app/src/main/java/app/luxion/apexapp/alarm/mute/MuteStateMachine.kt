package app.luxion.apexapp.alarm.mute

import java.time.Clock

/**
 * Silencio de la alarma mientras el usuario resuelve retos. Lógica pura: sin Android ni corrutinas,
 * solo el [clock] inyectado. Empieza en [State.RINGING]; una interacción la pasa a [State.MUTED] y
 * pasado [timeoutMillis] sin nuevas interacciones, [poll] la devuelve a [State.RINGING].
 */
class MuteStateMachine(
    private val timeoutMillis: Long,
    private val clock: Clock,
) {
    enum class State { RINGING, MUTED }

    var state: State = State.RINGING
        private set

    private var lastInteractionAt = 0L

    init {
        require(timeoutMillis > 0) { "timeoutMillis must be positive" }
    }

    /** Registra una interacción y reinicia la cuenta. Devuelve `true` si pasó de sonar a silenciada. */
    @Synchronized
    fun onInteraction(): Boolean {
        lastInteractionAt = clock.millis()
        if (state == State.MUTED) return false
        state = State.MUTED
        return true
    }

    /** Comprueba la inactividad. Devuelve `true` si la alarma debe volver a sonar. */
    @Synchronized
    fun poll(): Boolean {
        if (state != State.MUTED || clock.millis() - lastInteractionAt < timeoutMillis) return false
        state = State.RINGING
        return true
    }

    /** Milisegundos que faltan para volver a sonar, o `null` si ya suena. */
    @Synchronized
    fun millisUntilRing(): Long? {
        if (state != State.MUTED) return null
        return (timeoutMillis - (clock.millis() - lastInteractionAt)).coerceAtLeast(0L)
    }
}
