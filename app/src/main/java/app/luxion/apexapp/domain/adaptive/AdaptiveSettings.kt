package app.luxion.apexapp.domain.adaptive

import kotlinx.coroutines.flow.Flow

/** Estado adaptativo de una alarma. Sin fila equivale a "desactivado". */
data class AdaptiveSettings(
    val alarmId: Long,
    val enabled: Boolean = false,
    val minLevel: Int = MIN_LEVEL,
    val maxLevel: Int = MAX_LEVEL,
    val currentLevel: Int,
    val lastChangeReason: ChangeReason? = null,
    /** Cuántas de las últimas sesiones evaluadas cumplieron la señal del último cambio. */
    val lastChangeHits: Int = 0,
    val lastChangeFrom: Int? = null,
    val lastChangeAt: Long? = null,
    /** Sesión más reciente en el momento del último cambio o reinicio; las anteriores no cuentan. */
    val lastChangeSessionId: String? = null,
    /** Hay un cambio de nivel que la pantalla del reto aún no ha mostrado. */
    val noticePending: Boolean = false,
) {
    companion object {
        const val MIN_LEVEL = 1
        const val MAX_LEVEL = 5
    }
}

interface AdaptiveSettingsRepository {
    fun observe(alarmId: Long): Flow<AdaptiveSettings?>
    suspend fun get(alarmId: Long): AdaptiveSettings?
    suspend fun save(settings: AdaptiveSettings)
    suspend fun delete(alarmId: Long)
}
