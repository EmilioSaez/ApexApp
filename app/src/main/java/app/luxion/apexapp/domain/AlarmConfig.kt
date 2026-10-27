package app.luxion.apexapp.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Campos de extensión de [Alarm]. Se guardan como JSON (una columna por clase): los changes que los
 * poseen añaden propiedades con valor por defecto sin migrar el esquema.
 */

/** Retos para apagar la alarma. `difficulty == 0` significa "sin definir"; la interpreta challenge-engine. */
@Serializable
data class ChallengeConfig(
    val difficulty: Int = 0,
    val challenges: List<ChallengeSpec> = emptyList(),
    /** Número de retos de la sesión (1..10). */
    val count: Int = 1,
    val mode: ChallengeMode = ChallengeMode.CHAINED,
    /** El editor está en «Con retos»; permite tenerlo activo sin ningún tipo elegido todavía (no se puede guardar así). */
    val challengesEnabled: Boolean = false,
)

/** CHAINED recorre `challenges` en orden y repite el ciclo; RANDOM elige al azar entre ellos. */
@Serializable
enum class ChallengeMode { CHAINED, RANDOM }

/** Un reto de la cadena: `params` es JSON opaco, propio de cada `type`. */
@Serializable
data class ChallengeSpec(
    val type: String,
    val params: String = "{}",
)

@Serializable
data class SoundConfig(
    val volume: Int = 100,
    val vibrate: Boolean = true,
    val ttsEnabled: Boolean = false,
    val ttsText: String? = null,
    val rampSeconds: Int = 30,
    val vibrationPattern: String = "steady",
    val speakTime: Boolean = false,
    val speakRepeatSeconds: Int = 0,
) {
    init {
        require(volume in 0..100) { "volume must be in 0..100" }
        require(rampSeconds in 0..MAX_RAMP_SECONDS) { "rampSeconds must be in 0..$MAX_RAMP_SECONDS" }
        require(speakRepeatSeconds == 0 || speakRepeatSeconds >= MIN_SPEAK_REPEAT_SECONDS) {
            "speakRepeatSeconds must be 0 or >= $MIN_SPEAK_REPEAT_SECONDS"
        }
        require((ttsText?.length ?: 0) <= MAX_TTS_TEXT_LENGTH) { "ttsText too long" }
        require(VibrationPatterns.isValid(vibrationPattern)) { "invalid vibrationPattern" }
    }

    companion object {
        const val MAX_RAMP_SECONDS = 120
        const val MIN_SPEAK_REPEAT_SECONDS = 15
        const val MAX_TTS_TEXT_LENGTH = 140
    }
}

@Serializable(with = AntiCheatLevelSerializer::class)
enum class AntiCheatLevel { OFF, STANDARD }

/** El nivel `STRICT` se eliminó: las alarmas guardadas o compartidas con él pasan a `STANDARD` en lugar de a `OFF`. */
internal object AntiCheatLevelSerializer : KSerializer<AntiCheatLevel> {
    override val descriptor = PrimitiveSerialDescriptor("AntiCheatLevel", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AntiCheatLevel) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): AntiCheatLevel = when (val name = decoder.decodeString()) {
        "STRICT" -> AntiCheatLevel.STANDARD
        else -> AntiCheatLevel.entries.firstOrNull { it.name == name } ?: AntiCheatLevel.OFF
    }
}

@Serializable
data class SnoozePolicy(
    val allowed: Boolean = false,
    val minutes: Int = 5,
    val maxCount: Int = 3,
)

/** Ausente o vacío equivale a [AntiCheatLevel.OFF] y sin snooze. */
@Serializable
data class AntiCheatPolicy(
    val level: AntiCheatLevel = AntiCheatLevel.OFF,
    val snooze: SnoozePolicy = SnoozePolicy(),
)
