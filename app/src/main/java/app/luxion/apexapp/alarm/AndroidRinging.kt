package app.luxion.apexapp.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.luxion.apexapp.R
import app.luxion.apexapp.ui.ring.RingActivity
import app.luxion.apexapp.ui.ring.RingViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Tono de alarma del sistema en bucle; garantiza que el sonado nunca sea silencioso. */
@Singleton
class SystemToneRinger @Inject constructor(
    @ApplicationContext private val context: Context,
) : AlarmRinger {
    private var ringtone: Ringtone? = null

    @Synchronized
    override fun start(session: RingingSession) {
        stop()
        val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(context, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            isLooping = true
            play()
        }
    }

    @Synchronized
    override fun stop() {
        ringtone?.stop()
        ringtone = null
    }
}

/** Abre la pantalla de sonar (RingActivity) de la alarma y la sesión indicadas. */
class AndroidRingScreenLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) : RingScreenLauncher {
    override fun intentFor(session: RingingSession): Intent =
        Intent(context, RingActivity::class.java).apply {
            // El data URI evita que se confundan los PendingIntent de alarmas distintas.
            data = Uri.parse("apexapp://ring/${session.alarm.id}")
            putExtra(RingViewModel.EXTRA_ALARM_ID, session.alarm.id)
            putExtra(RingViewModel.EXTRA_SESSION_ID, session.sessionId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
}

class AndroidRingingLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) : RingingLauncher {
    override fun start(session: RingingSession): Boolean = try {
        ContextCompat.startForegroundService(
            context,
            Intent(context, AlarmRingingService::class.java)
                .putExtra(AndroidAlarmClockGateway.EXTRA_SESSION_ID, session.sessionId),
        )
        true
    } catch (e: Exception) {
        // ForegroundServiceStartNotAllowedException (API 31+) u otra restricción del sistema:
        // el llamador cae a una notificación de alta prioridad.
        false
    }
}

/** Notificación del sonado: canal propio sin sonido (el audio lo pone [AlarmRinger]) y pantalla completa. */
class RingingNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
    private val screenLauncher: RingScreenLauncher,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.ringing_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun build(session: RingingSession): Notification {
        ensureChannel()
        val alarm = session.alarm
        val open = PendingIntent.getActivity(
            context,
            alarm.id.hashCode(),
            screenLauncher.intentFor(session),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = when {
            alarm.id == TEST_ALARM_ID -> context.getString(R.string.reliability_test_title)
            else -> alarm.label ?: context.getString(R.string.alarm_default_title)
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(open)
            .apply { if (canUseFullScreenIntent()) setFullScreenIntent(open, true) }
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /** Notificación mínima para llamar a `startForeground` cuando la sesión ya no existe. */
    fun placeholder(): Notification {
        ensureChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.alarm_default_title))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    fun canUseFullScreenIntent(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || manager.canUseFullScreenIntent()

    companion object {
        const val CHANNEL_ID = "ringing"
    }
}
