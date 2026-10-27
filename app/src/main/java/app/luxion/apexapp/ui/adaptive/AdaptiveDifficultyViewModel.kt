package app.luxion.apexapp.ui.adaptive

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.luxion.apexapp.domain.AlarmRepository
import app.luxion.apexapp.domain.adaptive.AdaptiveDifficultyEngine
import app.luxion.apexapp.domain.adaptive.AdaptiveSettings
import app.luxion.apexapp.domain.adaptive.AdaptiveSettingsRepository
import app.luxion.apexapp.domain.adaptive.ChangeReason
import app.luxion.apexapp.domain.challenge.SessionConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdaptiveUiState(
    /** Solo las alarmas ya guardadas pueden activarlo: el estado cuelga de su identificador. */
    val available: Boolean = false,
    val enabled: Boolean = false,
    val min: Int = AdaptiveSettings.MIN_LEVEL,
    val max: Int = AdaptiveSettings.MAX_LEVEL,
    val current: Int = SessionConfig.DEFAULT_DIFFICULTY,
    val reason: ChangeReason? = null,
    val hits: Int = 0,
)

@HiltViewModel
class AdaptiveDifficultyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val alarms: AlarmRepository,
    settings: AdaptiveSettingsRepository,
    private val engine: AdaptiveDifficultyEngine,
) : ViewModel() {
    /** `0` = alarma nueva, aún sin guardar. */
    private val alarmId: Long = savedStateHandle["alarmId"] ?: 0L

    val state: StateFlow<AdaptiveUiState> =
        if (alarmId == 0L) {
            MutableStateFlow(AdaptiveUiState()).asStateFlow()
        } else {
            settings.observe(alarmId)
                .map { s ->
                    if (s == null) {
                        AdaptiveUiState(available = true)
                    } else {
                        AdaptiveUiState(
                            available = true,
                            enabled = s.enabled,
                            min = s.minLevel,
                            max = s.maxLevel,
                            current = s.currentLevel,
                            reason = s.lastChangeReason,
                            hits = s.lastChangeHits,
                        )
                    }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdaptiveUiState(available = true))
        }

    private suspend fun baseLevel(): Int =
        SessionConfig.normalizeDifficulty(alarms.get(alarmId)?.challenges?.difficulty ?: 0)

    fun setEnabled(enabled: Boolean) {
        if (alarmId == 0L) return
        viewModelScope.launch { engine.setEnabled(alarmId, enabled, baseLevel()) }
    }

    fun setRange(min: Int, max: Int) {
        if (alarmId == 0L) return
        viewModelScope.launch { engine.setRange(alarmId, min, max, baseLevel()) }
    }

    fun reset() {
        if (alarmId == 0L) return
        viewModelScope.launch { engine.reset(alarmId, baseLevel()) }
    }
}

/** Cambio de nivel pendiente de mostrar en la pantalla del reto. */
data class AdaptiveNotice(val level: Int, val reason: ChangeReason, val hits: Int)

@HiltViewModel
class AdaptiveNoticeViewModel @Inject constructor(
    private val engine: AdaptiveDifficultyEngine,
) : ViewModel() {
    private val _notice = MutableStateFlow<AdaptiveNotice?>(null)
    val notice: StateFlow<AdaptiveNotice?> = _notice.asStateFlow()
    private var loaded = false

    /** Lee una sola vez el aviso pendiente de la alarma y lo marca como mostrado. */
    fun load(alarmId: Long?) {
        if (loaded || alarmId == null) return
        loaded = true
        viewModelScope.launch {
            val s = engine.consumeNotice(alarmId) ?: return@launch
            val reason = s.lastChangeReason ?: return@launch
            _notice.value = AdaptiveNotice(s.currentLevel, reason, s.lastChangeHits)
        }
    }
}
