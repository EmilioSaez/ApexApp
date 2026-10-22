package app.luxion.apexapp.alarm.sound

import android.content.Context
import android.text.format.DateFormat
import app.luxion.apexapp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class AndroidTimeAnnouncer @Inject constructor(
    @ApplicationContext private val context: Context,
) : TimeAnnouncer {
    override fun announce(time: LocalTime): String {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        return context.getString(R.string.speech_time, time.format(DateTimeFormatter.ofPattern(pattern)))
    }
}
