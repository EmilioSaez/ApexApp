package app.luxion.apexapp.sharing

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.challenge.qr.QrChallenge

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
    challenge = challenges.withoutDeviceBoundParams(),
    sound = SharedSound(key = soundKey, config = soundConfig),
    antiCheat = antiCheat,
)

/** El reto QR guarda el hash de un código físico del emisor: sin él el receptor registra el suyo. */
private fun ChallengeConfig.withoutDeviceBoundParams(): ChallengeConfig =
    copy(challenges = challenges.map { if (it.type == QrChallenge.TYPE.id) ChallengeSpec(it.type) else it })

