package app.luxion.apexapp.domain

import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

interface SleepRepository {
    /** Horas de acostarse manuales, indexadas por la fecha del despertar. */
    fun observeManualBedtimes(): Flow<Map<LocalDate, Long>>
    suspend fun setBedtime(wakeDate: LocalDate, bedtimeAt: Long)
    suspend fun clearBedtime(wakeDate: LocalDate)

    fun observeUsualBedtime(): Flow<LocalTime?>
    suspend fun setUsualBedtime(time: LocalTime?)
}
