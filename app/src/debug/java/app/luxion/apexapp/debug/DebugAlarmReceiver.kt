package app.luxion.apexapp.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.luxion.apexapp.alarm.AlarmController
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AntiCheatLevel
import app.luxion.apexapp.domain.AntiCheatPolicy
import app.luxion.apexapp.domain.SnoozePolicy
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Solo debug. Crea una alarma desde adb:
 * `adb shell am broadcast -n app.luxion.apexapp/.debug.DebugAlarmReceiver --ei minutesFromNow 2 [--es label X] [--es level OFF|STANDARD|STRICT] [--ei snoozeMinutes 1 --ei snoozeMax 2]`
 */
@AndroidEntryPoint
class DebugAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var controller: AlarmController

    override fun onReceive(context: Context, intent: Intent) {
        val at = LocalTime.now().plusMinutes(intent.getIntExtra("minutesFromNow", 1).toLong())
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snoozeMinutes = intent.getIntExtra("snoozeMinutes", 0)
                val policy = AntiCheatPolicy(
                    level = intent.getStringExtra("level")?.let(AntiCheatLevel::valueOf) ?: AntiCheatLevel.OFF,
                    snooze = SnoozePolicy(
                        allowed = snoozeMinutes > 0,
                        minutes = snoozeMinutes.coerceAtLeast(1),
                        maxCount = intent.getIntExtra("snoozeMax", 3),
                    ),
                )
                controller.save(
                    Alarm(hour = at.hour, minute = at.minute, label = intent.getStringExtra("label"), antiCheat = policy),
                )
            } finally {
                pending.finish()
            }
        }
    }
}
