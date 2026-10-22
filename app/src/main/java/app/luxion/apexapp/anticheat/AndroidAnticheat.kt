package app.luxion.apexapp.anticheat

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowInsets
import android.view.WindowInsetsController
import app.luxion.apexapp.MainActivity
import app.luxion.apexapp.alarm.AlarmReceiver
import app.luxion.apexapp.alarm.AndroidAlarmClockGateway
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidCapabilityChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : CapabilityChecker {
    private val dpm = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, ApexDeviceAdminReceiver::class.java)

    override fun current() = Capabilities(
        overlay = Settings.canDrawOverlays(context),
        deviceAdmin = dpm.isAdminActive(admin),
        deviceOwner = dpm.isDeviceOwnerApp(context.packageName),
    )

    /** Ajuste del sistema para conceder la superposición. */
    fun overlaySettingsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

    /** Pantalla del sistema para activar el administrador de dispositivo. */
    fun deviceAdminIntent(): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
}

@Singleton
class AndroidAlarmVolumeGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) : AlarmVolumeGateway {
    private val audio = context.getSystemService(AudioManager::class.java)

    override fun get() = audio.getStreamVolume(AudioManager.STREAM_ALARM)
    override fun max() = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
    override fun set(volume: Int) = audio.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0)

    override fun observe(onChanged: () -> Unit): () -> Unit {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = onChanged()
        }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        return { context.contentResolver.unregisterContentObserver(observer) }
    }
}

@Singleton
class AndroidLockTaskPolicy @Inject constructor(
    @ApplicationContext private val context: Context,
) : LockTaskPolicy {
    private val dpm = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, ApexDeviceAdminReceiver::class.java)

    override fun allow() {
        if (!dpm.isDeviceOwnerApp(context.packageName)) return
        dpm.setLockTaskPackages(admin, arrayOf(context.packageName))
        dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
    }

    override fun clear() {
        if (!dpm.isDeviceOwnerApp(context.packageName)) return
        dpm.setLockTaskPackages(admin, emptyArray())
    }
}

@Singleton
class PrefsActiveSessionStore @Inject constructor(
    @ApplicationContext context: Context,
) : ActiveSessionStore {
    private val prefs = context.getSharedPreferences("anticheat_session", Context.MODE_PRIVATE)

    override fun put(sessionId: String, alarmId: Long) {
        prefs.edit().putString(KEY_SESSION, sessionId).putLong(KEY_ALARM, alarmId).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    override fun current(): Pair<String, Long>? =
        prefs.getString(KEY_SESSION, null)?.let { it to prefs.getLong(KEY_ALARM, -1) }

    private companion object {
        const val KEY_SESSION = "sessionId"
        const val KEY_ALARM = "alarmId"
    }
}

@Singleton
class PrefsSnoozeCounts @Inject constructor(
    @ApplicationContext context: Context,
) : SnoozeCounts {
    private val prefs = context.getSharedPreferences("anticheat_snooze", Context.MODE_PRIVATE)

    override fun get(sessionId: String) = prefs.getInt(sessionId, 0)
    override fun set(sessionId: String, count: Int) = prefs.edit().putInt(sessionId, count).apply()
    override fun clear(sessionId: String) = prefs.edit().remove(sessionId).apply()
}

/** Re-dispara la alarma con el mismo sessionId mediante setAlarmClock (exento de Doze). */
@Singleton
class AndroidSnoozeScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : SnoozeScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(alarmId: Long, sessionId: String, triggerAtMillis: Long) {
        val fire = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AlarmReceiver::class.java).apply {
                action = AndroidAlarmClockGateway.ACTION_FIRE
                data = Uri.parse("apexapp://alarm/$alarmId")
                putExtra(AndroidAlarmClockGateway.EXTRA_ALARM_ID, alarmId)
                putExtra(AndroidAlarmClockGateway.EXTRA_SESSION_ID, sessionId)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val show = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, show), fire)
    }
}

/**
 * Ayudante que cualquier pantalla de sonar usa para aplicar el lockdown: inmersivo, lock task y
 * consumo de las teclas de volumen. La pantalla observa LockdownController.active.
 */
object LockdownWindow {
    fun apply(activity: Activity, lockdown: ActiveLockdown?) {
        val controller = activity.window.insetsController
        if (controller != null) {
            if (lockdown?.protection?.immersive == true) {
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsets.Type.systemBars())
            } else {
                controller.show(WindowInsets.Type.systemBars())
            }
        }
        if (lockdown?.protection?.lockTask == true) {
            runCatching { activity.startLockTask() }
        } else {
            runCatching { activity.stopLockTask() }
        }
    }

    /** true si la tecla debe consumirse (volumen durante el lockdown). */
    fun consumesKey(lockdown: ActiveLockdown?, keyCode: Int): Boolean =
        lockdown?.protection?.volumeLock == true &&
            (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
}
