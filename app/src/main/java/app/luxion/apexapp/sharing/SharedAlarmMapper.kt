package app.luxion.apexapp.sharing

import app.luxion.apexapp.domain.Alarm

/**
 * Configuración compartible de [this]. [soundKey] es la clave del sonido incluido en la app, o null si usa el
 * de por defecto o uno no portable. No salen el identificador, el estado activo ni nada ligado al dispositivo.
 */
fun Alarm.toShared(soundKey: String?): SharedAlarm = SharedAlarm(
    alarm = SharedAlarmBody(
        hour = hour,
        minute = minute,
        days = repeatDays.map { it.value }.sorted(),
        label = label,
    ),
    challenge = challenges,
    sound = SharedSound(key = soundKey, config = soundConfig),
    antiCheat = antiCheat,
)
