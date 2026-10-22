package app.luxion.apexapp.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AndroidDeviceProbe @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifications: RingingNotifications,
) : DeviceProbe {
    override val sdkInt: Int = Build.VERSION.SDK_INT
    override val manufacturer: String = Build.MANUFACTURER.orEmpty()

    override fun exactAlarmsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    override fun notificationsEnabled(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun ringingChannelMuted(): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        notifications.ensureChannel()
        return manager.getNotificationChannel(RingingNotifications.CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
    }

    override fun fullScreenIntentAllowed(): Boolean = notifications.canUseFullScreenIntent()

    override fun ignoringBatteryOptimizations(): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
}

class DataStoreManufacturerGuideStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ManufacturerGuideStore {
    override suspend fun isConfirmed(): Boolean = dataStore.data.map { it[KEY] ?: false }.first()

    override suspend fun setConfirmed(confirmed: Boolean) {
        dataStore.edit { it[KEY] = confirmed }
    }

    private companion object {
        val KEY = booleanPreferencesKey("manufacturer_guide_confirmed")
    }
}

/** Intents de los ajustes del sistema que resuelven cada comprobación, en orden de preferencia. */
class ReliabilityIntents @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissions: AlarmPermissions,
    private val probe: DeviceProbe,
) {
    // BatteryLife: el aviso de Play no aplica, la app se distribuye por APK y es un despertador.
    @SuppressLint("BatteryLife")
    fun intentsFor(issue: ReliabilityIssue): List<Intent> {
        val packageUri = Uri.parse("package:${context.packageName}")
        val candidates = when (issue) {
            ReliabilityIssue.ExactAlarmDenied -> listOfNotNull(permissions.exactAlarmSettingsIntent())
            ReliabilityIssue.NotificationsDisabled -> listOf(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )

            ReliabilityIssue.RingingChannelMuted -> listOf(
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, RingingNotifications.CHANNEL_ID),
            )

            ReliabilityIssue.FullScreenIntentDenied ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    listOf(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri))
                } else {
                    emptyList()
                }

            ReliabilityIssue.BatteryOptimized -> listOf(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri))
            ReliabilityIssue.ManufacturerSetup -> ManufacturerGuides.forManufacturer(probe.manufacturer)?.intents().orEmpty()
        }
        return (candidates + Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
            .map { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }

    /** Abre el primer intent que el sistema acepte; los componentes de fabricante pueden no existir. */
    fun open(issue: ReliabilityIssue): Boolean {
        for (intent in intentsFor(issue)) {
            try {
                context.startActivity(intent)
                return true
            } catch (e: ActivityNotFoundException) {
                continue
            } catch (e: SecurityException) {
                continue
            }
        }
        return false
    }
}
