package app.luxion.apexapp.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import app.luxion.apexapp.MainActivity
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.Alarm
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Programa el aviso y el plazo con `setAlarmClock` (exento de Doze y sin cuota, a diferencia de `...AllowWhileIdle`). */
@Singleton
class AndroidAwakeVerificationGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) : AwakeVerificationGateway {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(alarmId: Long, phase: VerificationPhase, triggerAtMillis: Long) {
        val show = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, show),
            pendingIntent(alarmId, phase),
        )
    }

    override fun cancel(alarmId: Long) {
        VerificationPhase.entries.forEach { alarmManager.cancel(pendingIntent(alarmId, it)) }
    }

    private fun pendingIntent(alarmId: Long, phase: VerificationPhase): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AwakeVerificationReceiver::class.java).apply {
                action = if (phase == VerificationPhase.PROMPT) ACTION_PROMPT else ACTION_DEADLINE
                data = Uri.parse("apexapp://verify/$alarmId/${phase.name.lowercase()}")
                putExtra(EXTRA_ALARM_ID, alarmId)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        const val ACTION_PROMPT = "app.luxion.apexapp.action.VERIFY_PROMPT"
        const val ACTION_DEADLINE = "app.luxion.apexapp.action.VERIFY_DEADLINE"
        const val ACTION_CONFIRM = "app.luxion.apexapp.action.VERIFY_CONFIRM"
        const val EXTRA_ALARM_ID = "alarmId"

        /** Extra de `MainActivity`: la notificación tocada confirma esa alarma. */
        const val EXTRA_CONFIRM_ALARM_ID = "confirmAwakeAlarmId"
    }
}

@Singleton
class PrefsAwakeVerificationStore @Inject constructor(
    @ApplicationContext context: Context,
) : AwakeVerificationStore {
    private val prefs = context.getSharedPreferences("awake_verification", Context.MODE_PRIVATE)

    override fun get(alarmId: Long): PendingVerification? = prefs.getString(alarmId.toString(), null)?.let { parse(alarmId, it) }

    override fun put(pending: PendingVerification) {
        val value = listOf(pending.sessionId, pending.phase.name, pending.dueAt).joinToString(SEPARATOR)
        prefs.edit().putString(pending.alarmId.toString(), value).apply()
    }

    override fun clear(alarmId: Long) {
        prefs.edit().remove(alarmId.toString()).apply()
    }

    override fun all(): List<PendingVerification> =
        prefs.all.mapNotNull { (key, value) ->
            key.toLongOrNull()?.let { id -> (value as? String)?.let { parse(id, it) } }
        }

    private fun parse(alarmId: Long, value: String): PendingVerification? {
        val parts = value.split(SEPARATOR)
        if (parts.size != 3) return null
        val phase = VerificationPhase.entries.firstOrNull { it.name == parts[1] } ?: return null
        val dueAt = parts[2].toLongOrNull() ?: return null
        return PendingVerification(alarmId, parts[0], phase, dueAt)
    }

    private companion object {
        const val SEPARATOR = "|"
    }
}

class AndroidAwakeVerificationNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : AwakeVerificationNotifier {
    private val manager = context.getSystemService(NotificationManager::class.java)

    override fun show(alarm: Alarm, deadlineAt: Long): Boolean {
        ensureChannel()
        val channel = manager.getNotificationChannel(CHANNEL_ID)
        if (!manager.areNotificationsEnabled() || channel?.importance == NotificationManager.IMPORTANCE_NONE) return false
        val confirm = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AwakeVerificationReceiver::class.java).apply {
                action = AndroidAwakeVerificationGateway.ACTION_CONFIRM
                data = Uri.parse("apexapp://verify/${alarm.id}/confirm")
                putExtra(AndroidAwakeVerificationGateway.EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // Tocar la notificación abre la app y cuenta como confirmación.
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                data = Uri.parse("apexapp://verify/${alarm.id}/open")
                putExtra(AndroidAwakeVerificationGateway.EXTRA_CONFIRM_ALARM_ID, alarm.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val minutes = (AwakeVerificationPolicy.CONFIRM_WINDOW_MILLIS / 60_000L).toInt()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.awake_verification_notification_title))
            .setContentText(context.getString(R.string.awake_verification_notification_text, minutes))
            .setContentIntent(open)
            // No se descarta deslizando: se resuelve confirmando o al vencer el plazo.
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setTimeoutAfter(maxOf(deadlineAt - System.currentTimeMillis(), 1L))
            .addAction(0, context.getString(R.string.awake_verification_action), confirm)
            .build()
        manager.notify(TAG, alarm.id.toInt(), notification)
        return true
    }

    override fun cancel(alarmId: Long) = manager.cancel(TAG, alarmId.toInt())

    private fun ensureChannel() {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.awake_verification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
    }

    companion object {
        const val CHANNEL_ID = "awake_verification"
        private const val TAG = "awake_verification"
    }
}

@AndroidEntryPoint
class AwakeVerificationReceiver : BroadcastReceiver() {
    @Inject lateinit var verifier: AwakeVerifier

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AndroidAwakeVerificationGateway.EXTRA_ALARM_ID, -1)
        if (alarmId < 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    AndroidAwakeVerificationGateway.ACTION_PROMPT -> verifier.onPrompt(alarmId)
                    AndroidAwakeVerificationGateway.ACTION_DEADLINE -> verifier.onDeadline(alarmId)
                    AndroidAwakeVerificationGateway.ACTION_CONFIRM -> verifier.onConfirm(alarmId)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
