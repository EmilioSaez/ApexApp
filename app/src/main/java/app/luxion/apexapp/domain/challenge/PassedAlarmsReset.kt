package app.luxion.apexapp.domain.challenge

import app.luxion.apexapp.alarm.EndReason
import app.luxion.apexapp.alarm.RingingLifecycleListener
import app.luxion.apexapp.alarm.RingingSession
import javax.inject.Inject

/**
 * Olvida que la alarma estaba superada al empezar y al terminar cada sonado, de modo que cada disparo
 * (también tras posponer) exija sus retos de nuevo.
 */
class PassedAlarmsReset @Inject constructor(
    private val passedAlarms: PassedAlarms,
) : RingingLifecycleListener {
    override fun onStarted(session: RingingSession) = passedAlarms.clear(session.alarm.id)

    override fun onEnded(session: RingingSession, reason: EndReason) = passedAlarms.clear(session.alarm.id)
}
