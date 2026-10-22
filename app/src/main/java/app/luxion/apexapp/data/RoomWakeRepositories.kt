package app.luxion.apexapp.data

import androidx.room.withTransaction
import app.luxion.apexapp.data.local.ApexDatabase
import app.luxion.apexapp.data.local.SleepDao
import app.luxion.apexapp.data.local.SleepLogEntity
import app.luxion.apexapp.data.local.SleepPreferencesEntity
import app.luxion.apexapp.data.local.WakeEventDao
import app.luxion.apexapp.data.local.WakeEventEntity
import app.luxion.apexapp.domain.SleepRepository
import app.luxion.apexapp.domain.Streak
import app.luxion.apexapp.domain.WakeEvent
import app.luxion.apexapp.domain.WakeEventRecorder
import app.luxion.apexapp.domain.WakeEventType
import app.luxion.apexapp.domain.WakeHistoryRepository
import app.luxion.apexapp.domain.WakeSession
import app.luxion.apexapp.domain.computeStreak
import app.luxion.apexapp.domain.groupSessions
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Máximo de sesiones leídas al buscar las recientes de una alarma, con o sin sospechosas. */
private const val RECENT_SCAN_LIMIT = 100

/** Los eventos que solo pueden existir una vez por sesión. */
private val ONCE_PER_SESSION = setOf(WakeEventType.RANG, WakeEventType.COMPLETED, WakeEventType.MISSED)

internal fun WakeEventEntity.toDomain() = WakeEvent(
    sessionId = sessionId,
    type = WakeEventType.valueOf(type),
    alarmId = alarmId,
    timestamp = timestamp,
    scheduledAt = scheduledAt,
    challengeType = challengeType,
    difficulty = difficulty,
)

class RoomWakeEventRecorder @Inject constructor(
    private val db: ApexDatabase,
    private val dao: WakeEventDao,
) : WakeEventRecorder {
    override suspend fun record(
        sessionId: String,
        type: WakeEventType,
        alarmId: Long?,
        timestamp: Long,
        scheduledAt: Long?,
        challengeType: String?,
        difficulty: Int?,
    ) {
        db.withTransaction {
            if (type in ONCE_PER_SESSION && dao.count(sessionId, type.name) > 0) return@withTransaction
            dao.insert(
                WakeEventEntity(
                    sessionId = sessionId,
                    type = type.name,
                    alarmId = alarmId,
                    timestamp = timestamp,
                    scheduledAt = scheduledAt,
                    challengeType = challengeType,
                    difficulty = difficulty,
                ),
            )
        }
    }
}

class RoomWakeHistoryRepository @Inject constructor(
    private val dao: WakeEventDao,
    private val clock: Clock,
) : WakeHistoryRepository {
    override fun observeSessions(from: Instant, to: Instant, challengeType: String?): Flow<List<WakeSession>> =
        dao.observeInRange(from.toEpochMilli(), to.toEpochMilli()).map { sessionsIn(it, from, to, challengeType) }

    override suspend fun sessions(from: Instant, to: Instant, challengeType: String?): List<WakeSession> =
        observeSessions(from, to, challengeType).first()

    override suspend fun recentSessions(alarmId: Long, limit: Int, excludeSuspicious: Boolean): List<WakeSession> =
        groupSessions(dao.recentForAlarm(alarmId, RECENT_SCAN_LIMIT).map { it.toDomain() }, clock.instant())
            .filter { !excludeSuspicious || !it.suspicious }
            .sortedByDescending { it.firedAt }
            .take(limit)

    override fun observeStreak(): Flow<Streak> =
        dao.observeAll().map { computeStreak(groupSessions(it.map(WakeEventEntity::toDomain), clock.instant()), clock.zone) }

    private fun sessionsIn(events: List<WakeEventEntity>, from: Instant, to: Instant, challengeType: String?) =
        groupSessions(events.map { it.toDomain() }, clock.instant()).filter {
            it.scheduledAt >= from.toEpochMilli() && it.scheduledAt < to.toEpochMilli() &&
                (challengeType == null || challengeType in it.challengeTypes)
        }
}

class RoomSleepRepository @Inject constructor(private val dao: SleepDao) : SleepRepository {
    override fun observeManualBedtimes(): Flow<Map<LocalDate, Long>> =
        dao.observeLogs().map { logs -> logs.associate { LocalDate.parse(it.wakeDate) to it.bedtimeAt } }

    override suspend fun setBedtime(wakeDate: LocalDate, bedtimeAt: Long) =
        dao.upsertLog(SleepLogEntity(wakeDate.toString(), bedtimeAt))

    override suspend fun clearBedtime(wakeDate: LocalDate) = dao.deleteLog(wakeDate.toString())

    override fun observeUsualBedtime(): Flow<LocalTime?> =
        dao.observePreferences().map { p -> p?.usualBedtimeMinutes?.let { LocalTime.ofSecondOfDay(it * 60L) } }

    override suspend fun setUsualBedtime(time: LocalTime?) =
        dao.upsertPreferences(SleepPreferencesEntity(usualBedtimeMinutes = time?.let { it.hour * 60 + it.minute }))
}
