package app.luxion.apexapp.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.luxion.apexapp.domain.SleepRepository
import app.luxion.apexapp.domain.WakeHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    history: WakeHistoryRepository,
    private val sleep: SleepRepository,
    private val clock: Clock,
) : ViewModel() {
    private val range = MutableStateFlow(StatsRange.WEEK)

    val uiState: StateFlow<StatsUiState> = range.flatMapLatest { r ->
        val today = LocalDate.now(clock)
        val from = today.minusDays(r.days - 1L).atStartOfDay(clock.zone).toInstant()
        val to = today.plusDays(1).atStartOfDay(clock.zone).toInstant()
        combine(
            history.observeSessions(from, to),
            history.observeSessions(Instant.EPOCH, to),
            sleep.observeManualBedtimes(),
            sleep.observeUsualBedtime(),
        ) { sessions, all, manual, usual ->
            buildStatsState(r, sessions, all, manual, usual, today, clock.zone)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun selectRange(newRange: StatsRange) {
        range.value = newRange
    }

    /** Registra la última vez que [time] ocurrió antes de ahora como hora de acostarse de esta mañana. */
    fun setLastNightBedtime(time: LocalTime) {
        val now = clock.instant().atZone(clock.zone)
        var at = now.toLocalDate().atTime(time).atZone(clock.zone)
        if (at.isAfter(now)) at = at.minusDays(1)
        viewModelScope.launch { sleep.setBedtime(now.toLocalDate(), at.toInstant().toEpochMilli()) }
    }

    fun setUsualBedtime(time: LocalTime?) {
        viewModelScope.launch { sleep.setUsualBedtime(time) }
    }
}
