package app.luxion.apexapp.domain.adaptive

import app.luxion.apexapp.domain.WakeHistoryRepository
import javax.inject.Inject

/** Historial fiable de una alarma, de la sesión más reciente a la más antigua. */
interface WakeHistorySource {
    suspend fun recent(alarmId: Long, limit: Int): List<SessionSummary>
}

/** Adaptador sobre `wake-stats`: excluye sospechosas y sesiones sin completar. */
class WakeStatsHistorySource @Inject constructor(
    private val history: WakeHistoryRepository,
) : WakeHistorySource {
    override suspend fun recent(alarmId: Long, limit: Int): List<SessionSummary> =
        history.recentSessions(alarmId, limit, excludeSuspicious = true).toSummaries(alarmId)
}
