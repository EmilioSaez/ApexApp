package app.luxion.apexapp.domain.adaptive

import javax.inject.Inject

/**
 * Orquesta el ajuste: evalúa al cerrar cada sesión y deja el nivel guardado para la siguiente.
 * Nunca lanza por fallos de lectura del historial: el disparo de la alarma no puede depender de ello.
 */
class AdaptiveDifficultyEngine(
    private val settings: AdaptiveSettingsRepository,
    private val history: WakeHistorySource,
    private val calculator: AdaptiveDifficultyCalculator,
    private val config: AdaptiveDifficultyConfig,
    private val now: () -> Long,
) {
    @Inject
    constructor(
        settings: AdaptiveSettingsRepository,
        history: WakeHistorySource,
    ) : this(settings, history, AdaptiveDifficultyCalculator(), AdaptiveDifficultyConfig(), System::currentTimeMillis)

    /** Nivel para una sesión que empieza: el adaptativo si la alarma lo tiene activo, si no [baseLevel]. */
    suspend fun levelFor(alarmId: Long?, baseLevel: Int): Int {
        if (alarmId == null) return baseLevel
        val s = runCatching { settings.get(alarmId) }.getOrNull() ?: return baseLevel
        if (!s.enabled) return baseLevel
        return s.currentLevel.coerceIn(s.minLevel, s.maxLevel)
    }

    /** Evalúa tras una sesión finalizada. Null si no hay nada que evaluar o falla la lectura. */
    suspend fun onSessionFinished(alarmId: Long): AdaptiveDecision? {
        val s = settings.get(alarmId)?.takeIf { it.enabled } ?: return null
        val recent = runCatching { history.recent(alarmId, config.window) }.getOrNull() ?: return null
        val decision = calculator.decide(s.currentLevel, s.minLevel, s.maxLevel, recent, s.lastChangeSessionId)
        if (!decision.changed) return decision
        settings.save(
            s.copy(
                currentLevel = decision.level,
                lastChangeReason = decision.reason,
                lastChangeHits = decision.hits,
                lastChangeFrom = s.currentLevel.coerceIn(s.minLevel, s.maxLevel),
                lastChangeAt = now(),
                lastChangeSessionId = recent.firstOrNull()?.sessionId ?: s.lastChangeSessionId,
                noticePending = true,
            ),
        )
        return decision
    }

    /** Activa o desactiva. Al activar parte de [baseLevel]; desactivar conserva el rango. */
    suspend fun setEnabled(alarmId: Long, enabled: Boolean, baseLevel: Int) {
        val s = settings.get(alarmId)
        if (s == null) {
            if (enabled) {
                settings.save(
                    AdaptiveSettings(
                        alarmId = alarmId,
                        enabled = true,
                        currentLevel = baseLevel.coerceIn(AdaptiveSettings.MIN_LEVEL, AdaptiveSettings.MAX_LEVEL),
                    ),
                )
            }
            return
        }
        if (enabled == s.enabled) return
        settings.save(
            if (enabled) {
                s.copy(enabled = true, currentLevel = baseLevel.coerceIn(s.minLevel, s.maxLevel))
            } else {
                s.copy(enabled = false)
            },
        )
    }

    /** Fija el rango. Devuelve false, sin cambios, si es inválido. */
    suspend fun setRange(alarmId: Long, min: Int, max: Int, baseLevel: Int): Boolean {
        if (min > max || min < AdaptiveSettings.MIN_LEVEL || max > AdaptiveSettings.MAX_LEVEL) return false
        val s = settings.get(alarmId)
            ?: AdaptiveSettings(alarmId = alarmId, enabled = false, currentLevel = baseLevel.coerceIn(min, max))
        settings.save(s.copy(minLevel = min, maxLevel = max, currentLevel = s.currentLevel.coerceIn(min, max)))
        return true
    }

    /** Vuelve al nivel base y descarta el historial anterior para ajustes futuros. */
    suspend fun reset(alarmId: Long, baseLevel: Int) {
        val s = settings.get(alarmId) ?: return
        val latest = runCatching { history.recent(alarmId, 1) }.getOrNull()?.firstOrNull()?.sessionId
        settings.save(
            s.copy(
                currentLevel = baseLevel.coerceIn(s.minLevel, s.maxLevel),
                lastChangeReason = null,
                lastChangeHits = 0,
                lastChangeFrom = null,
                lastChangeAt = null,
                lastChangeSessionId = latest ?: s.lastChangeSessionId,
                noticePending = false,
            ),
        )
    }

    /** Devuelve el aviso pendiente (y lo marca como mostrado) para la pantalla del reto. */
    suspend fun consumeNotice(alarmId: Long): AdaptiveSettings? {
        val s = settings.get(alarmId)?.takeIf { it.enabled && it.noticePending } ?: return null
        settings.save(s.copy(noticePending = false))
        return s
    }
}
