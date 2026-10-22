package app.luxion.apexapp.domain

import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** Lectura del historial de despertar. Contrato estable para adaptive-difficulty y sharing. */
interface WakeHistoryRepository {
    /** Sesiones cuyo instante programado cae en `[from, to)`, de la más antigua a la más reciente. */
    fun observeSessions(from: Instant, to: Instant, challengeType: String? = null): Flow<List<WakeSession>>
    suspend fun sessions(from: Instant, to: Instant, challengeType: String? = null): List<WakeSession>

    /** Últimas [limit] sesiones de la alarma, de la más reciente a la más antigua. */
    suspend fun recentSessions(alarmId: Long, limit: Int, excludeSuspicious: Boolean): List<WakeSession>

    /** Racha actual y mejor racha, con las mismas reglas que la pantalla de estadísticas. */
    fun observeStreak(): Flow<Streak>
}
