package app.luxion.apexapp.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.luxion.apexapp.alarm.AlarmController
import app.luxion.apexapp.domain.Alarm
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Solo debug. Crea una alarma desde adb:
 * `adb shell am broadcast -n app.luxion.apexapp/.debug.DebugAlarmReceiver --ei minutesFromNow 2 [--es label X]`
 */
@AndroidEntryPoint
class DebugAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var controller: AlarmController

    override fun onReceive(context: Context, intent: Intent) {
        val at = LocalTime.now().plusMinutes(intent.getIntExtra("minutesFromNow", 1).toLong())
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                controller.save(Alarm(hour = at.hour, minute = at.minute, label = intent.getStringExtra("label")))
            } finally {
                pending.finish()
            }
        }
    }
}
