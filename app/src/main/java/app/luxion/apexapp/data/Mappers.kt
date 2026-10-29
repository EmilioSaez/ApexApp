package app.luxion.apexapp.data

import app.luxion.apexapp.data.local.AlarmEntity
import app.luxion.apexapp.data.local.SoundEntity
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.Sound
import app.luxion.apexapp.domain.SoundSource
import app.luxion.apexapp.domain.AntiCheatPolicy
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.SoundConfig
import java.time.DayOfWeek
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val SOURCE_RESOURCE = "resource"
private const val SOURCE_URI = "uri"
private const val SOURCE_SYSTEM = "system"
private const val SOURCE_BUNDLED = "bundled"

/** Tolerante: ignora claves desconocidas y usa los defaults del dominio para las ausentes. */
internal val ConfigJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
}

/** El reto de foto a un objeto se eliminó: las alarmas guardadas con él lo pierden en lugar de quedar con un tipo oculto. */
private fun ChallengeConfig.withoutRemovedTypes(): ChallengeConfig =
    copy(challenges = challenges.filterNot { it.type == "object" })

private inline fun <reified T> decodeConfig(json: String, default: T): T =
    runCatching { ConfigJson.decodeFromString<T>(json) }.getOrDefault(default)

internal fun Set<DayOfWeek>.toMask(): Int = fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

internal fun Int.toDays(): Set<DayOfWeek> =
    DayOfWeek.entries.filter { this and (1 shl (it.value - 1)) != 0 }.toSet()

internal fun AlarmEntity.toDomain() = Alarm(
    id = id,
    hour = hour,
    minute = minute,
    repeatDays = repeatMask.toDays(),
    enabled = enabled,
    label = label,
    soundId = soundId,
    challenges = decodeConfig(challengesJson, ChallengeConfig()).withoutRemovedTypes(),
    soundConfig = decodeConfig(soundJson, SoundConfig()),
    antiCheat = decodeConfig(antiCheatJson, AntiCheatPolicy()),
    awakeVerification = awakeVerificationEnabled,
)

internal fun Alarm.toEntity() = AlarmEntity(
    id = id,
    hour = hour,
    minute = minute,
    repeatMask = repeatDays.toMask(),
    enabled = enabled,
    label = label,
    soundId = soundId,
    challengesJson = ConfigJson.encodeToString(challenges),
    soundJson = ConfigJson.encodeToString(soundConfig),
    antiCheatJson = ConfigJson.encodeToString(antiCheat),
    awakeVerificationEnabled = awakeVerification,
)

internal fun SoundEntity.toDomain() = Sound(
    id = id,
    name = name,
    source = when (sourceType) {
        SOURCE_RESOURCE -> SoundSource.Resource(sourceValue.toInt())
        SOURCE_URI -> SoundSource.Uri(sourceValue)
        SOURCE_SYSTEM -> SoundSource.SystemTone(sourceValue)
        SOURCE_BUNDLED -> SoundSource.Bundled(sourceValue)
        else -> error("Unknown sound source type: $sourceType")
    },
)

internal fun Sound.toEntity(): SoundEntity {
    val (type, value) = when (val s = source) {
        is SoundSource.Resource -> SOURCE_RESOURCE to s.resId.toString()
        is SoundSource.Uri -> SOURCE_URI to s.uri
        is SoundSource.SystemTone -> SOURCE_SYSTEM to s.uri
        is SoundSource.Bundled -> SOURCE_BUNDLED to s.key
    }
    return SoundEntity(id = id, name = name, sourceType = type, sourceValue = value)
}
