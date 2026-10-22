package app.luxion.apexapp.ui.challenge

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.challenge.PassedAlarms
import app.luxion.apexapp.ui.ring.DismissGate
import javax.inject.Inject

/**
 * Solo permite descartar una alarma con retos cuando se ha superado su sesión. Una alarma sin retos
 * configurados se descarta de inmediato. No está enlazada en Hilt: se activa en el flujo real cuando
 * se coordine con alarm-reliability y anti-cheat (sustituyendo el binding de [DismissGate]).
 */
class ChallengeDismissGate @Inject constructor(
    private val passedAlarms: PassedAlarms,
) : DismissGate {
    override fun canDismiss(alarm: Alarm): Boolean =
        alarm.challenges.challenges.isEmpty() || passedAlarms.isPassed(alarm.id)
}
