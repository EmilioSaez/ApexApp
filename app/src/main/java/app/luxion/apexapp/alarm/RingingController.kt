package app.luxion.apexapp.alarm

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Cierra las sesiones de sonado y avisa a los observadores. Es el [AlarmResolver] que usan
 * alarm-core (reto completado), anti-cheat (snooze) y el propio servicio (tiempo agotado).
 */
@Singleton
class RingingController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: RingingRegistry,
    private val ringer: Provider<AlarmRinger>,
    private val listeners: Set<@JvmSuppressWildcards RingingLifecycleListener>,
) : AlarmResolver {

    fun started(session: RingingSession) {
        listeners.forEach { runCatching { it.onStarted(session) } }
    }

    override fun resolve(sessionId: String, reason: EndReason) {
        val session = registry.end(sessionId) ?: return
        val remaining = registry.active()
        if (remaining.isEmpty()) ringer.get().stop() else ringer.get().start(remaining.last())
        // Un fallo en un observador no debe interrumpir el cierre.
        listeners.forEach { runCatching { it.onEnded(session, reason) } }
        if (remaining.isEmpty()) context.stopService(Intent(context, AlarmRingingService::class.java))
    }
}
