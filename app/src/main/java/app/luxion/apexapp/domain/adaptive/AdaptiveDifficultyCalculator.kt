package app.luxion.apexapp.domain.adaptive

/** Función pura: decide el siguiente nivel de una alarma a partir de su historial reciente. */
class AdaptiveDifficultyCalculator(private val config: AdaptiveDifficultyConfig = AdaptiveDifficultyConfig()) {

    /**
     * [recent] va de la sesión más reciente a la más antigua y solo contiene sesiones fiables.
     * Las sesiones desde [lastChangeSessionId] (incluida) hacia atrás no cuentan: tras un cambio hacen
     * falta sesiones nuevas.
     */
    fun decide(
        level: Int,
        min: Int,
        max: Int,
        recent: List<SessionSummary>,
        lastChangeSessionId: String? = null,
    ): AdaptiveDecision {
        require(min <= max) { "min must be <= max" }
        val current = level.coerceIn(min, max)
        val fresh = recent
            .takeWhile { it.sessionId != lastChangeSessionId }
            .take(config.window)
        if (fresh.size < config.minSessions) return AdaptiveDecision(current)

        val last = fresh.take(config.lookback)
        val failures = last.count { it.failures >= config.struggleFailures }
        val slow = last.count { it.snoozes == 0 && (it.timeToWakeMs ?: 0) > config.slowMs }
        val struggle = last.count {
            it.failures >= config.struggleFailures || (it.snoozes == 0 && (it.timeToWakeMs ?: 0) > config.slowMs)
        }
        if (struggle >= config.requiredHits) {
            val next = (current - 1).coerceAtLeast(min)
            if (next == current) return AdaptiveDecision(current)
            // Se explica con la señal dominante; ante empate, los fallos.
            val reason = if (failures >= slow) ChangeReason.FAILURES else ChangeReason.SLOW
            return AdaptiveDecision(next, reason, struggle)
        }

        val sleepy = last.count { it.snoozes >= 1 }
        val easy = last.size == config.lookback && last.all {
            it.snoozes == 0 && it.failures == 0 && (it.timeToWakeMs ?: Long.MAX_VALUE) < config.easyMs
        }
        val reason = when {
            sleepy >= config.requiredHits -> ChangeReason.SNOOZES
            easy -> ChangeReason.EASY
            else -> return AdaptiveDecision(current)
        }
        val next = (current + 1).coerceAtMost(max)
        if (next == current) return AdaptiveDecision(current)
        return AdaptiveDecision(next, reason, if (reason == ChangeReason.SNOOZES) sleepy else last.size)
    }
}
