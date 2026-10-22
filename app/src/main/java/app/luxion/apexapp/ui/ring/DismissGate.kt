package app.luxion.apexapp.ui.ring

import app.luxion.apexapp.domain.Alarm
import javax.inject.Inject

/**
 * Punto de extensión que decide si una alarma que suena puede descartarse. Challenge-engine y
 * anti-cheat sustituyen el binding por el suyo; por defecto se permite descartar de inmediato.
 */
interface DismissGate {
    fun canDismiss(alarm: Alarm): Boolean
}

class DefaultDismissGate @Inject constructor() : DismissGate {
    override fun canDismiss(alarm: Alarm) = true
}
