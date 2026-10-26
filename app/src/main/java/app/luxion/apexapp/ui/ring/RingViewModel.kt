package app.luxion.apexapp.ui.ring

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.luxion.apexapp.alarm.AlarmNotifier
import app.luxion.apexapp.alarm.AlarmResolver
import app.luxion.apexapp.alarm.RingingSession
import app.luxion.apexapp.anticheat.SnoozeController
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AlarmRepository
import app.luxion.apexapp.domain.challenge.SessionConfig
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
    val canSnooze: Boolean = false,
    /** Sesión de retos que hay que superar antes de poder apagar; `null` si se puede apagar ya. */
    val challenges: SessionConfig? = null,
    val finished: Boolean = false,
)

@HiltViewModel
class RingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AlarmRepository,
    private val gate: DismissGate,
    private val notifier: AlarmNotifier,
    private val resolver: AlarmResolver,
    private val snoozeController: SnoozeController,
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
                if (alarm == null) {
                    it.copy(finished = true)
                } else {
                    val canDismiss = gate.canDismiss(alarm)
                    it.copy(
                        alarm = alarm,
                        canDismiss = canDismiss,
                        canSnooze = canSnooze(alarm),
                        challenges = if (canDismiss) null else challengesOf(alarm),
                    )
                }
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

    private fun challengesOf(alarm: Alarm): SessionConfig? =
        alarm.challenges.takeIf { it.challenges.isNotEmpty() }?.let(SessionConfig::from)

    private fun canSnooze(alarm: Alarm): Boolean =
        sessionId != null && snoozeController.canSnooze(RingingSession(sessionId, alarm))

    /** Pospone la alarma: la sesión continúa cuando vuelva a sonar. */
    fun snooze() {
        val alarm = _uiState.value.alarm ?: return
        val id = sessionId ?: return
        viewModelScope.launch {
            if (snoozeController.snooze(RingingSession(id, alarm))) {
                notifier.cancel(alarm.id)
                _uiState.update { it.copy(finished = true) }
            }
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarmId"
        const val EXTRA_SESSION_ID = "sessionId"
    }
}
