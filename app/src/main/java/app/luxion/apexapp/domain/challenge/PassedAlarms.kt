package app.luxion.apexapp.domain.challenge

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Alarmas cuya sesión de retos se ha superado. La rellena [ChallengeSessionFactory] al terminar una
 * sesión con `alarmId`; quien lance una nueva sesión para la alarma debe llamar a [clear] antes.
 */
@Singleton
class PassedAlarms @Inject constructor() {
    private val passed: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    fun markPassed(alarmId: Long) {
        passed += alarmId
    }

    fun isPassed(alarmId: Long): Boolean = alarmId in passed

    fun clear(alarmId: Long) {
        passed -= alarmId
    }
}
