package app.luxion.apexapp.domain.adaptive

import app.luxion.apexapp.domain.challenge.SessionFinishedListener
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Cierra el ciclo al terminar una sesión de retos: libera el nivel fijado de la sesión y evalúa el
 * siguiente ajuste. La evaluación va fuera del hilo del llamador (el gancho es síncrono) y un fallo
 * no se propaga: la alarma ya se ha apagado.
 */
@Singleton
class AdaptiveSessionListener(
    private val engine: AdaptiveDifficultyEngine,
    private val provider: AdaptiveDifficultyProvider,
    private val scope: CoroutineScope,
) : SessionFinishedListener {
    @Inject
    constructor(
        engine: AdaptiveDifficultyEngine,
        provider: AdaptiveDifficultyProvider,
    ) : this(engine, provider, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    override fun onFinished(sessionId: String, alarmId: Long?) {
        provider.release(sessionId)
        // Sin alarma (pantalla de prueba) no hay nada que ajustar.
        if (alarmId == null) return
        scope.launch {
            runCatching { engine.onSessionFinished(alarmId, sessionId) }
        }
    }
}
