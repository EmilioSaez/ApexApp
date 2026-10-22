package app.luxion.apexapp.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Único servicio de sonado de la app. Mantiene vivo el proceso mientras suena una alarma; el audio,
 * la pantalla de sonar y la resolución se acoplan mediante el contrato de `RingingContract`.
 */
@AndroidEntryPoint
class AlarmRingingService : Service() {
    @Inject lateinit var registry: RingingRegistry
    @Inject lateinit var ringer: AlarmRinger
    @Inject lateinit var controller: RingingController
    @Inject lateinit var notifications: RingingNotifications

    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val session = intent?.getStringExtra(AndroidAlarmClockGateway.EXTRA_SESSION_ID)?.let(registry::get)
        // startForeground es obligatorio tras startForegroundService, aunque no quede nada que sonar.
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            session?.let(notifications::build) ?: notifications.placeholder(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        if (session == null) {
            if (registry.isEmpty()) stopSelf()
            return START_NOT_STICKY
        }
        acquireWakeLock()
        ringer.start(session)
        controller.started(session)
        handler.postDelayed({ controller.resolve(session.sessionId, EndReason.TIMED_OUT) }, MAX_RING_MILLIS)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        // Si el sistema destruye el servicio, el audio no debe seguir sonando huérfano.
        registry.active().forEach { controller.resolve(it.sessionId, EndReason.TIMED_OUT) }
        ringer.stop()
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "apexapp:ringing")
            .apply { acquire(MAX_RING_MILLIS + WAKE_LOCK_GRACE_MILLIS) }
    }

    companion object {
        const val NOTIFICATION_ID = 4242

        /** Tiempo máximo de sonado sin resolver. */
        const val MAX_RING_MILLIS = 10 * 60 * 1000L
        private const val WAKE_LOCK_GRACE_MILLIS = 60 * 1000L
    }
}
