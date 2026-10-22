package app.luxion.apexapp.alarm.sound

import app.luxion.apexapp.alarm.AlarmRinger
import app.luxion.apexapp.alarm.RingingSession
import javax.inject.Inject

/** Audio del sonado: sustituye al tono del sistema por el motor de sonido de la alarma. */
class SoundAlarmRinger @Inject constructor(
    private val engine: AlarmSoundEngine,
) : AlarmRinger {
    override fun start(session: RingingSession) = engine.start(session.alarm)

    override fun stop() = engine.stop()
}
