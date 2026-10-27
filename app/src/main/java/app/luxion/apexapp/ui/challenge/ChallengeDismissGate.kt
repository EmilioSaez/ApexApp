package app.luxion.apexapp.ui.challenge

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.challenge.PassedAlarms
import app.luxion.apexapp.ui.ring.DismissGate
import javax.inject.Inject

/**
 * Solo permite descartar una alarma con retos cuando se ha superado su sesión. Una alarma sin retos
 * configurados se descarta de inmediato. Es el binding de Hilt de [DismissGate]. [PassedAlarms] vive
 * en memoria: si el proceso muere con la alarma sonando no queda como superada (falla cerrado).
 */
class ChallengeDismissGate @Inject constructor(
    private val passedAlarms: PassedAlarms,
) : DismissGate {
    override fun canDismiss(alarm: Alarm): Boolean =
        alarm.challenges.challenges.isEmpty() || passedAlarms.isPassed(alarm.id)
}
