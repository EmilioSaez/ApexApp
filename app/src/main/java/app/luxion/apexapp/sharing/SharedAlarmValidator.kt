package app.luxion.apexapp.sharing

import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AntiCheatPolicy
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.SnoozePolicy
import app.luxion.apexapp.domain.SoundConfig
import app.luxion.apexapp.domain.challenge.SessionConfig
import app.luxion.apexapp.domain.challenge.qr.QrChallenge
import java.time.DayOfWeek
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Avisos de la vista previa: partes del contenido que no se importan tal cual. */
enum class ImportNote { QR_CHALLENGE_OMITTED, UNKNOWN_CHALLENGE_OMITTED, UNKNOWN_SOUND_DEFAULTED }

/**
 * Alarma lista para guardar. [soundKey] es la clave de un sonido incluido en la app (null = el de por defecto);
 * la alarma nace desactivada y sin `soundId`, que lo fija quien la guarda.
 */
data class ImportPreview(val alarm: Alarm, val soundKey: String?, val notes: Set<ImportNote>)

sealed interface ValidationResult {
    data class Valid(val preview: ImportPreview) : ValidationResult
    data class Invalid(val error: ImportError) : ValidationResult
}

/** Valida por completo contenido no confiable: o sale una vista previa íntegra o nada. */
object SharedAlarmValidator {
    const val MAX_LABEL_LENGTH = 60
    const val MAX_CHALLENGES = 10
    const val MAX_PARAMS_LENGTH = 1024
    const val MAX_SNOOZE_MINUTES = 60
    const val MAX_SNOOZE_COUNT = 10

    private val json = Json

    fun validate(
        shared: SharedAlarm,
        knownChallengeTypes: Set<String>,
        knownSoundKeys: Set<String>,
    ): ValidationResult {
        val body = shared.alarm
        val invalid = ValidationResult.Invalid(ImportError.INVALID)
        if (body.hour !in 0..23 || body.minute !in 0..59) return invalid
        if (body.days.any { it !in 1..7 } || body.days.toSet().size != body.days.size) return invalid
        val label = body.label?.trim()?.takeIf { it.isNotEmpty() }
        if ((label?.length ?: 0) > MAX_LABEL_LENGTH) return invalid

        val notes = mutableSetOf<ImportNote>()
        val challenges = validChallenges(shared.challenge, knownChallengeTypes, notes) ?: return invalid
        val antiCheat = shared.antiCheat ?: AntiCheatPolicy()
        if (antiCheat.snooze.minutes !in 1..MAX_SNOOZE_MINUTES) return invalid
        if (antiCheat.snooze.maxCount != SnoozePolicy.UNLIMITED && antiCheat.snooze.maxCount !in 0..MAX_SNOOZE_COUNT) return invalid

        val key = shared.sound?.key
        val soundKey = key?.takeIf { it in knownSoundKeys }
        if (key != null && soundKey == null) notes += ImportNote.UNKNOWN_SOUND_DEFAULTED

        val alarm = Alarm(
            hour = body.hour,
            minute = body.minute,
            repeatDays = body.days.map { DayOfWeek.of(it) }.toSet(),
            enabled = false,
            label = label,
            challenges = challenges,
            soundConfig = shared.sound?.config ?: SoundConfig(),
            antiCheat = antiCheat,
        )
        return ValidationResult.Valid(ImportPreview(alarm, soundKey, notes))
    }

    private fun validChallenges(
        config: ChallengeConfig?,
        knownTypes: Set<String>,
        notes: MutableSet<ImportNote>,
    ): ChallengeConfig? {
        config ?: return ChallengeConfig()
        if (config.difficulty !in 0..SessionConfig.MAX_DIFFICULTY) return null
        if (config.count !in SessionConfig.MIN_COUNT..SessionConfig.MAX_COUNT) return null
        if (config.challenges.size > MAX_CHALLENGES) return null
        val specs = mutableListOf<ChallengeSpec>()
        for (spec in config.challenges) {
            when {
                spec.type == QrChallenge.TYPE.id -> notes += ImportNote.QR_CHALLENGE_OMITTED
                spec.type !in knownTypes -> notes += ImportNote.UNKNOWN_CHALLENGE_OMITTED
                !isJsonObject(spec.params) -> return null
                else -> specs += spec
            }
        }
        return config.copy(challenges = specs)
    }

    private fun isJsonObject(params: String): Boolean =
        params.length <= MAX_PARAMS_LENGTH &&
            runCatching { json.parseToJsonElement(params) is JsonObject }.getOrDefault(false)
}
