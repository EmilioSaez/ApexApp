package app.luxion.apexapp.ui.ring

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.luxion.apexapp.alarm.AlarmNotifier
import app.luxion.apexapp.alarm.AlarmResolver
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RingUiState(
    val alarm: Alarm? = null,
    val sessionId: String? = null,
    val canDismiss: Boolean = false,
    val finished: Boolean = false,
)

@HiltViewModel
class RingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AlarmRepository,
    private val gate: DismissGate,
    private val notifier: AlarmNotifier,
    private val resolver: AlarmResolver,
) : ViewModel() {
    private val alarmId: Long = savedStateHandle[EXTRA_ALARM_ID] ?: -1L
    private val sessionId: String? = savedStateHandle[EXTRA_SESSION_ID]

    private val _uiState = MutableStateFlow(RingUiState(sessionId = sessionId))
    val uiState: StateFlow<RingUiState> = _uiState

    init {
        viewModelScope.launch {
            val alarm = repository.get(alarmId)
            // La alarma pudo borrarse entre el disparo y la apertura: no hay nada que mostrar.
            _uiState.update {
                if (alarm == null) it.copy(finished = true) else it.copy(alarm = alarm, canDismiss = gate.canDismiss(alarm))
            }
        }
    }

    fun dismiss() {
        val alarm = _uiState.value.alarm ?: return
        if (!gate.canDismiss(alarm)) return
        notifier.cancel(alarm.id)
        // Detiene el audio y el servicio de sonado; sin sesión (apertura manual) no hay nada que cerrar.
        sessionId?.let(resolver::resolve)
        _uiState.update { it.copy(finished = true) }
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarmId"
        const val EXTRA_SESSION_ID = "sessionId"
    }
}
