package app.luxion.apexapp.domain.adaptive

import app.luxion.apexapp.domain.challenge.ChallengeContext
import app.luxion.apexapp.domain.challenge.DifficultyProvider
import app.luxion.apexapp.domain.challenge.SessionConfig
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Sustituye a `FixedDifficultyProvider`. `levelFor` se llama por reto y tras cada fallo, así que el
 * nivel se calcula una vez por sesión y se reutiliza: no cambia mientras la sesión está en curso.
 */
@Singleton
class AdaptiveDifficultyProvider @Inject constructor(
    private val engine: AdaptiveDifficultyEngine,
) : DifficultyProvider {
    private val bySession = ConcurrentHashMap<String, Int>()

    override fun levelFor(config: SessionConfig, context: ChallengeContext): Int =
        bySession.getOrPut(context.sessionId) {
            // Lectura local pequeña de Room; el contrato del gancho es síncrono.
            runBlocking(Dispatchers.IO) { engine.levelFor(context.alarmId, config.difficulty) }
                .coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY)
        }

    /** Libera el nivel guardado de una sesión terminada. */
    fun release(sessionId: String) {
        bySession.remove(sessionId)
    }
}
