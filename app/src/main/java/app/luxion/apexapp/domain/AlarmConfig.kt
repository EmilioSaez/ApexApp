package app.luxion.apexapp.domain

import kotlinx.serialization.Serializable

/**
 * Campos de extensión de [Alarm]. Se guardan como JSON (una columna por clase): los changes que los
 * poseen añaden propiedades con valor por defecto sin migrar el esquema.
 */

/** Retos para apagar la alarma. `difficulty == 0` significa "sin definir"; la interpreta challenge-engine. */
@Serializable
data class ChallengeConfig(
    val difficulty: Int = 0,
    val challenges: List<ChallengeSpec> = emptyList(),
)

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
)

@Serializable
enum class AntiCheatLevel { OFF, STANDARD, STRICT }

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
