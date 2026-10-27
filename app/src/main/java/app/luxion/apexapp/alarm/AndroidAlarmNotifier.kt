package app.luxion.apexapp.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.ui.ring.RingActivity
import app.luxion.apexapp.ui.ring.RingViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

class AndroidAlarmNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : AlarmNotifier {
    private val manager = context.getSystemService(NotificationManager::class.java)

    override fun notify(alarm: Alarm, sessionId: String) {
        ensureChannel()
        if (!manager.areNotificationsEnabled()) return
        val ring = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            Intent(context, RingActivity::class.java).apply {
                // El data URI evita que se confundan los PendingIntent de alarmas distintas.
                data = Uri.parse("apexapp://ring/${alarm.id}")
                putExtra(RingViewModel.EXTRA_ALARM_ID, alarm.id)
                putExtra(RingViewModel.EXTRA_SESSION_ID, sessionId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val time = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(alarm.label ?: context.getString(R.string.alarm_default_title))
            .setContentText(time)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(ring)
            .setFullScreenIntent(ring, true)
            .setOngoing(true)
            .build()
        manager.notify(alarm.id.toInt(), notification)
    }

    override fun cancel(alarmId: Long) = manager.cancel(alarmId.toInt())

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "alarms"
    }
}
