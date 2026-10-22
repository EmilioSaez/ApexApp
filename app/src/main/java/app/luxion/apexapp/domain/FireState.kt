package app.luxion.apexapp.domain

/** Última hora programada y último disparo de una alarma, para detectar alarmas perdidas. */
data class FireState(
    val alarmId: Long,
    val scheduledAt: Long? = null,
    val lastFiredAt: Long? = null,
)

interface FireStateRepository {
    suspend fun get(alarmId: Long): FireState?
    suspend fun markScheduled(alarmId: Long, scheduledAt: Long?)
    suspend fun markFired(alarmId: Long, firedAt: Long)
    suspend fun clear(alarmId: Long)
}
