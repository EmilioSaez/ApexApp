package app.luxion.apexapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.luxion.apexapp.alarm.AlarmController
import app.luxion.apexapp.alarm.AlarmPermissions
import app.luxion.apexapp.alarm.PermissionState
import app.luxion.apexapp.alarm.ReliabilityChecker
import app.luxion.apexapp.alarm.ReliabilityState
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val alarms: List<Alarm> = emptyList(),
    val permissions: PermissionState = PermissionState(),
    /** Última alarma borrada, disponible para deshacer hasta que la pantalla la consuma. */
    val deleted: Alarm? = null,
    val reliability: ReliabilityState = ReliabilityState(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val controller: AlarmController,
    private val alarmPermissions: AlarmPermissions,
    private val reliabilityChecker: ReliabilityChecker,
) : ViewModel() {
    private val permissions = MutableStateFlow(alarmPermissions.current())
    private val deleted = MutableStateFlow<Alarm?>(null)
    private val reliability = MutableStateFlow(ReliabilityState())

    init {
        refreshPermissions()
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeAll(),
        permissions,
        deleted,
        reliability,
    ) { alarms, permissions, deleted, reliability ->
        HomeUiState(isLoading = false, alarms = alarms, permissions = permissions, deleted = deleted, reliability = reliability)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Se llama al volver a la pantalla: el usuario pudo cambiar los permisos en Ajustes. */
    fun refreshPermissions() {
        permissions.value = alarmPermissions.current()
        viewModelScope.launch { reliability.value = reliabilityChecker.check() }
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            controller.setEnabled(id, enabled)
            refreshPermissions()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            val alarm = repository.get(id) ?: return@launch
            controller.delete(id)
            deleted.value = alarm
        }
    }

    /** Restaura la última alarma borrada con el mismo `id` y la reprograma si estaba activa. */
    fun undoDelete() {
        val alarm = deleted.value ?: return
        deleted.value = null
        viewModelScope.launch {
            controller.save(alarm)
            refreshPermissions()
        }
    }

    /** La pantalla ya mostró el aviso de borrado: ya no se puede deshacer. */
    fun consumeDeleted() {
        deleted.value = null
    }

    fun exactAlarmSettingsIntent() = alarmPermissions.exactAlarmSettingsIntent()
}
