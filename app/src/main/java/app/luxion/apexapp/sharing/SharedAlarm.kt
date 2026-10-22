package app.luxion.apexapp.sharing

import app.luxion.apexapp.domain.AntiCheatPolicy
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.SoundConfig
import kotlinx.serialization.Serializable

/**
 * Contenido que viaja entre dispositivos (enlace, QR o archivo). Es un formato propio, desacoplado de
 * [app.luxion.apexapp.domain.Alarm]: `v` sube solo si un cambio rompe la lectura en versiones anteriores;
 * los campos opcionales nuevos no necesitan subirla.
 */
@Serializable
data class SharedAlarm(
    val v: Int = SharedAlarmCodec.SCHEMA_VERSION,
    val alarm: SharedAlarmBody,
    val challenge: ChallengeConfig? = null,
    val sound: SharedSound? = null,
    val antiCheat: AntiCheatPolicy? = null,
)

/** Hora, repetición (ISO: 1 = lunes … 7 = domingo) y etiqueta. */
@Serializable
data class SharedAlarmBody(
    val hour: Int,
    val minute: Int,
    val days: List<Int> = emptyList(),
    val label: String? = null,
)

/** `key` es la clave estable de un sonido incluido en la app; nunca una URI. */
@Serializable
data class SharedSound(
    val key: String? = null,
    val config: SoundConfig = SoundConfig(),
)
