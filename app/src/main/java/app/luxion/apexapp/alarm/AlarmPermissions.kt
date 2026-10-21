package app.luxion.apexapp.alarm

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class PermissionState(
    val notificationsGranted: Boolean = true,
    val exactAlarmsGranted: Boolean = true,
) {
    val allGranted: Boolean get() = notificationsGranted && exactAlarmsGranted
}

interface AlarmPermissions {
    fun current(): PermissionState

    /** Intent al ajuste de alarmas exactas de la app, o `null` si no hace falta (API < 31). */
    fun exactAlarmSettingsIntent(): Intent?
}

class AndroidAlarmPermissions @Inject constructor(
    @ApplicationContext private val context: Context,
) : AlarmPermissions {
    override fun current() = PermissionState(
        notificationsGranted = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        exactAlarmsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
    )

    override fun exactAlarmSettingsIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
    }
}
