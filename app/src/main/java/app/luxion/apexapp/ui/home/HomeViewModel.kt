package app.luxion.apexapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.luxion.apexapp.alarm.AlarmController
import app.luxion.apexapp.alarm.AlarmPermissions
import app.luxion.apexapp.alarm.PermissionState
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
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: AlarmRepository,
    private val controller: AlarmController,
    private val alarmPermissions: AlarmPermissions,
) : ViewModel() {
    private val permissions = MutableStateFlow(alarmPermissions.current())

    val uiState: StateFlow<HomeUiState> = combine(repository.observeAll(), permissions) { alarms, permissions ->
        HomeUiState(isLoading = false, alarms = alarms, permissions = permissions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Se llama al volver a la pantalla: el usuario pudo cambiar los permisos en Ajustes. */
    fun refreshPermissions() {
        permissions.value = alarmPermissions.current()
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            controller.setEnabled(id, enabled)
            refreshPermissions()
        }
    }

    fun exactAlarmSettingsIntent() = alarmPermissions.exactAlarmSettingsIntent()
}
