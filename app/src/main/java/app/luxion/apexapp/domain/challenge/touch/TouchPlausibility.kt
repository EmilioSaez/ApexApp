package app.luxion.apexapp.domain.challenge.touch

import app.luxion.apexapp.domain.AntiCheatLevel
import kotlin.math.sqrt

enum class TouchAction { PRESS, RELEASE }

/** Un toque ya reducido a lo que se valida; sirve para pulsar, mantener/soltar y toques por carril. */
data class TouchSample(
    val tMillis: Long,
    val action: TouchAction = TouchAction.PRESS,
    val fromTouchscreen: Boolean = true,
    /** La ventana estaba tapada, total o parcialmente, por otra superficie. */
    val obscured: Boolean = false,
)

sealed interface TouchVerdict {
    data object Accepted : TouchVerdict
    data class Rejected(val reason: Reason) : TouchVerdict

    enum class Reason { NOT_TOUCHSCREEN, OBSCURED, TOO_FAST, TOO_REGULAR }
}

/**
 * Valida toques según el nivel anti-trampas efectivo de la alarma: `OFF` acepta todo; `STANDARD` descarta
 * origen no táctil, ventana tapada y pulsaciones más rápidas que un humano; `STRICT` añade la regularidad
 * sintética. En `STRICT` cada descarte se notifica a [onSuspect] (la sesión se marca sospechosa con varios).
 */
class TouchPlausibility(
    private val level: AntiCheatLevel,
    private val onSuspect: () -> Unit = {},
) {
    private val recentPresses = ArrayDeque<Long>()

    fun check(sample: TouchSample): TouchVerdict {
        if (level == AntiCheatLevel.OFF) return TouchVerdict.Accepted
        val reason = when {
            !sample.fromTouchscreen -> TouchVerdict.Reason.NOT_TOUCHSCREEN
            sample.obscured -> TouchVerdict.Reason.OBSCURED
            sample.action == TouchAction.PRESS -> pressReason(sample.tMillis)
            else -> null
        } ?: return TouchVerdict.Accepted
        if (level == AntiCheatLevel.STRICT) onSuspect()
        return TouchVerdict.Rejected(reason)
    }

    /** Solo las pulsaciones aceptadas entran en la ventana de regularidad. */
    private fun pressReason(t: Long): TouchVerdict.Reason? {
        val last = recentPresses.lastOrNull()
        if (last != null && t - last < MIN_INTERVAL_MS) return TouchVerdict.Reason.TOO_FAST
        recentPresses.addLast(t)
        if (recentPresses.size > WINDOW) recentPresses.removeFirst()
        if (level == AntiCheatLevel.STRICT && recentPresses.size == WINDOW && tooRegular()) {
            recentPresses.removeLast()
            return TouchVerdict.Reason.TOO_REGULAR
        }
        return null
    }

    private fun tooRegular(): Boolean {
        val intervals = recentPresses.zipWithNext { a, b -> (b - a).toDouble() }
        val mean = intervals.average()
        val sd = sqrt(intervals.sumOf { (it - mean) * (it - mean) } / intervals.size)
        return mean > 0 && sd / mean < MIN_VARIATION
    }

    companion object {
        /** Intervalo mínimo humano entre pulsaciones sucesivas. */
        const val MIN_INTERVAL_MS = 60L

        /** Pulsaciones recientes que se juzgan por regularidad. */
        const val WINDOW = 6

        /** Coeficiente de variación de los intervalos por debajo del cual se considera sintético (como `SensorPlausibility`). */
        const val MIN_VARIATION = 0.02
    }
}
