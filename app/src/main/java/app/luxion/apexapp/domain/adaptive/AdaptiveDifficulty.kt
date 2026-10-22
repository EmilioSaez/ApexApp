package app.luxion.apexapp.domain.adaptive

/** Resumen de una sesión de despertar con las tres señales que usa el algoritmo. */
data class SessionSummary(
    val sessionId: String,
    val snoozes: Int,
    val failures: Int,
    /** Desde que suena hasta completar; incluye los snoozes. */
    val timeToWakeMs: Long?,
)

/** Constantes del algoritmo. No son editables por el usuario. */
data class AdaptiveDifficultyConfig(
    /** Sesiones recientes que se consultan. */
    val window: Int = 5,
    /** Sesiones mínimas para poder ajustar. */
    val minSessions: Int = 3,
    /** Sesiones más recientes que se evalúan con las reglas. */
    val lookback: Int = 3,
    /** Sesiones de [lookback] que deben cumplir una señal de subida o bajada. */
    val requiredHits: Int = 2,
    val struggleFailures: Int = 3,
    val slowMs: Long = 120_000,
    val easyMs: Long = 30_000,
)

enum class ChangeReason { SNOOZES, EASY, FAILURES, SLOW }

/**
 * Resultado de un ajuste. [reason] es null cuando el nivel no cambia; [hits] es cuántas de las
 * últimas [AdaptiveDifficultyConfig.lookback] sesiones cumplieron la señal.
 */
data class AdaptiveDecision(
    val level: Int,
    val reason: ChangeReason? = null,
    val hits: Int = 0,
) {
    val changed: Boolean get() = reason != null
}
