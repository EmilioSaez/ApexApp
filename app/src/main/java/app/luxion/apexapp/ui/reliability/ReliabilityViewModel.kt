package app.luxion.apexapp.ui.reliability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.luxion.apexapp.alarm.AlarmTestScheduler
import app.luxion.apexapp.alarm.DeviceProbe
import app.luxion.apexapp.alarm.ManufacturerGuide
import app.luxion.apexapp.alarm.ManufacturerGuideStore
import app.luxion.apexapp.alarm.ManufacturerGuides
import app.luxion.apexapp.alarm.ReliabilityChecker
import app.luxion.apexapp.alarm.ReliabilityIntents
import app.luxion.apexapp.alarm.ReliabilityIssue
import app.luxion.apexapp.alarm.ReliabilityState
import app.luxion.apexapp.alarm.ScheduleResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReliabilityUiState(
    val state: ReliabilityState = ReliabilityState(),
    val guide: ManufacturerGuide? = null,
    val testResult: ScheduleResult? = null,
)

@HiltViewModel
class ReliabilityViewModel @Inject constructor(
    private val checker: ReliabilityChecker,
    private val intents: ReliabilityIntents,
    private val guideStore: ManufacturerGuideStore,
    private val tester: AlarmTestScheduler,
    probe: DeviceProbe,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReliabilityUiState(guide = ManufacturerGuides.forManufacturer(probe.manufacturer)))
    val uiState: StateFlow<ReliabilityUiState> = _uiState

    /** Se llama al volver a la pantalla: el usuario pudo cambiar los ajustes del sistema. */
    fun refresh() {
        viewModelScope.launch {
            val state = checker.check()
            _uiState.update { it.copy(state = state) }
        }
    }

    fun open(issue: ReliabilityIssue) {
        intents.open(issue)
    }

    fun confirmGuide() {
        viewModelScope.launch {
            guideStore.setConfirmed(true)
            refresh()
        }
    }

    fun runTest() {
        val result = tester.schedule()
        _uiState.update { it.copy(testResult = result) }
    }
}
