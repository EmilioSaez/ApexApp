package app.luxion.apexapp.domain.challenge.touch

import app.luxion.apexapp.domain.AntiCheatLevel

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

    enum class Reason { NOT_TOUCHSCREEN, OBSCURED, TOO_FAST }
}

/**
 * Valida toques según el nivel anti-trampas efectivo de la alarma: `OFF` acepta todo; `STANDARD` descarta
 * origen no táctil, ventana tapada y pulsaciones más rápidas que un humano.
 */
class TouchPlausibility(private val level: AntiCheatLevel) {
    private var lastPressAt: Long? = null

    fun check(sample: TouchSample): TouchVerdict {
        if (level == AntiCheatLevel.OFF) return TouchVerdict.Accepted
        val reason = when {
            !sample.fromTouchscreen -> TouchVerdict.Reason.NOT_TOUCHSCREEN
            sample.obscured -> TouchVerdict.Reason.OBSCURED
            sample.action == TouchAction.PRESS -> pressReason(sample.tMillis)
            else -> null
        } ?: return TouchVerdict.Accepted
        return TouchVerdict.Rejected(reason)
    }

    /** Solo las pulsaciones aceptadas cuentan para el intervalo mínimo. */
    private fun pressReason(t: Long): TouchVerdict.Reason? {
        val last = lastPressAt
        if (last != null && t - last < MIN_INTERVAL_MS) return TouchVerdict.Reason.TOO_FAST
        lastPressAt = t
        return null
    }

    companion object {
        /** Intervalo mínimo humano entre pulsaciones sucesivas. */
        const val MIN_INTERVAL_MS = 60L
    }
}
