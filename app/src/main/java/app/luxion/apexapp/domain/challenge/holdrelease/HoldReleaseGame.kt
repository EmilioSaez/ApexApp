package app.luxion.apexapp.domain.challenge.holdrelease

import kotlin.random.Random

enum class HoldPhase { IDLE, HOLDING, SIGNAL, DONE }

enum class RoundOutcome { HIT, EARLY, LATE, CANCELLED }

/**
 * Máquina de rondas de «Mantener y soltar». Los tiempos son milisegundos de un reloj monotónico que pone el
 * llamador. Un fallo repite la ronda con otro instante de señal y suma una ronda extra (hasta [HoldReleaseLevel.maxExtra]); el
 * tercer fallo seguido reinicia solo la ronda en curso, sin ronda extra y conservando las superadas.
 */
class HoldReleaseGame(
    private val baseRounds: Int,
    private val tuning: HoldReleaseLevel,
    private val random: Random,
) {
    var phase: HoldPhase = HoldPhase.IDLE
        private set
    var roundsDone: Int = 0
        private set
    var extraRounds: Int = 0
        private set
    private var streak = 0
    private var pressedAt = 0L
    private var signalAt = 0L

    val totalRounds: Int get() = baseRounds + extraRounds
    val done: Boolean get() = phase == HoldPhase.DONE

    /** Empieza el mantenimiento; el instante de la señal se sortea aquí. Se ignora fuera de [HoldPhase.IDLE]. */
    fun press(t: Long) {
        if (phase != HoldPhase.IDLE) return
        pressedAt = t
        signalAt = t + random.nextLong(tuning.minHoldMs, tuning.maxHoldMs + 1)
        phase = HoldPhase.HOLDING
    }

    /** Avanza el reloj: cambia la señal y da por fallada la ronda si la ventana expira con el dedo puesto. */
    fun tick(t: Long): RoundOutcome? {
        if (phase == HoldPhase.HOLDING && t >= signalAt) phase = HoldPhase.SIGNAL
        return if (phase == HoldPhase.SIGNAL && t > signalAt + tuning.windowMs) fail(RoundOutcome.LATE) else null
    }

    /** Resultado de soltar en [t], o null si no había pulsación en curso. */
    fun release(t: Long): RoundOutcome? {
        tick(t)?.let { return it }
        return when (phase) {
            HoldPhase.HOLDING -> fail(RoundOutcome.EARLY)
            HoldPhase.SIGNAL -> hit(t)
            else -> null
        }
    }

    /** La pantalla perdió el foco o se canceló el gesto: la ronda en curso cuenta como fallada. */
    fun cancel(): RoundOutcome? =
        if (phase == HoldPhase.HOLDING || phase == HoldPhase.SIGNAL) fail(RoundOutcome.CANCELLED) else null

    private fun hit(t: Long): RoundOutcome {
        streak = 0
        roundsDone++
        phase = if (roundsDone >= totalRounds) HoldPhase.DONE else HoldPhase.IDLE
        return RoundOutcome.HIT
    }

    private fun fail(outcome: RoundOutcome): RoundOutcome {
        streak++
        if (streak >= RESTART_AFTER) streak = 0 else if (extraRounds < tuning.maxExtra) extraRounds++
        phase = HoldPhase.IDLE
        return outcome
    }

    companion object {
        const val RESTART_AFTER = 3
    }
}
