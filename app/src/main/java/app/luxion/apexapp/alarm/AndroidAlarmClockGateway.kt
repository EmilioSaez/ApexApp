package app.luxion.apexapp.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import app.luxion.apexapp.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidAlarmClockGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) : AlarmClockGateway {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    override fun setAlarmClock(alarmId: Long, triggerAtMillis: Long) {
        val showIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
            firePendingIntent(alarmId, PendingIntent.FLAG_UPDATE_CURRENT),
        )
    }

    override fun cancel(alarmId: Long) {
        alarmManager.cancel(firePendingIntent(alarmId, PendingIntent.FLAG_UPDATE_CURRENT))
    }

    private fun firePendingIntent(alarmId: Long, flags: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            // El data URI hace único el PendingIntent de cada alarma.
            data = Uri.parse("apexapp://alarm/$alarmId")
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val ACTION_FIRE = "app.luxion.apexapp.action.FIRE_ALARM"
        const val EXTRA_ALARM_ID = "alarmId"
    }
}
