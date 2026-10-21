package app.luxion.apexapp.data

import app.luxion.apexapp.data.local.AlarmEntity
import app.luxion.apexapp.data.local.SoundEntity
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.Sound
import app.luxion.apexapp.domain.SoundSource
import java.time.DayOfWeek

private const val SOURCE_RESOURCE = "resource"
private const val SOURCE_URI = "uri"

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
)

internal fun Alarm.toEntity() = AlarmEntity(
    id = id,
    hour = hour,
    minute = minute,
    repeatMask = repeatDays.toMask(),
    enabled = enabled,
    label = label,
    soundId = soundId,
)

internal fun SoundEntity.toDomain() = Sound(
    id = id,
    name = name,
    source = when (sourceType) {
        SOURCE_RESOURCE -> SoundSource.Resource(sourceValue.toInt())
        SOURCE_URI -> SoundSource.Uri(sourceValue)
        else -> error("Unknown sound source type: $sourceType")
    },
)

internal fun Sound.toEntity(): SoundEntity {
    val (type, value) = when (val s = source) {
        is SoundSource.Resource -> SOURCE_RESOURCE to s.resId.toString()
        is SoundSource.Uri -> SOURCE_URI to s.uri
    }
    return SoundEntity(id = id, name = name, sourceType = type, sourceValue = value)
}
