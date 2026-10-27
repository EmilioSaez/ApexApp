package app.luxion.apexapp.data

import app.luxion.apexapp.data.local.AdaptiveSettingsDao
import app.luxion.apexapp.data.local.AdaptiveSettingsEntity
import app.luxion.apexapp.domain.adaptive.AdaptiveSettings
import app.luxion.apexapp.domain.adaptive.AdaptiveSettingsRepository
import app.luxion.apexapp.domain.adaptive.ChangeReason
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAdaptiveSettingsRepository @Inject constructor(
    private val dao: AdaptiveSettingsDao,
) : AdaptiveSettingsRepository {
    override fun observe(alarmId: Long): Flow<AdaptiveSettings?> = dao.observe(alarmId).map { it?.toDomain() }

    override suspend fun get(alarmId: Long): AdaptiveSettings? = dao.get(alarmId)?.toDomain()

    override suspend fun save(settings: AdaptiveSettings) = dao.upsert(settings.toEntity())

    override suspend fun delete(alarmId: Long) = dao.delete(alarmId)
}

private fun AdaptiveSettingsEntity.toDomain() = AdaptiveSettings(
    alarmId = alarmId,
    enabled = enabled,
    minLevel = minLevel,
    maxLevel = maxLevel,
    currentLevel = currentLevel,
    lastChangeReason = lastChangeReason?.let { name -> ChangeReason.entries.firstOrNull { it.name == name } },
    lastChangeHits = lastChangeHits,
    lastChangeFrom = lastChangeFrom,
    lastChangeAt = lastChangeAt,
    lastChangeSessionId = lastChangeSessionId,
    noticePending = noticePending,
)

private fun AdaptiveSettings.toEntity() = AdaptiveSettingsEntity(
    alarmId = alarmId,
    enabled = enabled,
    minLevel = minLevel,
    maxLevel = maxLevel,
    currentLevel = currentLevel,
    lastChangeReason = lastChangeReason?.name,
    lastChangeHits = lastChangeHits,
    lastChangeFrom = lastChangeFrom,
    lastChangeAt = lastChangeAt,
    lastChangeSessionId = lastChangeSessionId,
    noticePending = noticePending,
)
