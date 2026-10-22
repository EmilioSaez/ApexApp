package app.luxion.apexapp.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.luxion.apexapp.alarm.AlarmController
import app.luxion.apexapp.alarm.ScheduleResult
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AntiCheatLevel
import app.luxion.apexapp.domain.AntiCheatPolicy
import app.luxion.apexapp.domain.AlarmRepository
import app.luxion.apexapp.domain.nextOccurrence
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Resultado de guardar: si quedó programada y cuánto falta para la próxima ocurrencia. */
data class SavedResult(val schedule: ScheduleResult, val remaining: Duration?)

data class EditorUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val hour: Int = DEFAULT_HOUR,
    val minute: Int = 0,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val label: String = "",
    /** Alarma en edición con todos sus campos de extensión; las [EditorSection] la transforman. */
    val draft: Alarm = Alarm(
        hour = DEFAULT_HOUR,
        minute = 0,
        // Una alarma nueva nace con anti-trampas estándar; las existentes conservan su política.
        antiCheat = AntiCheatPolicy(level = AntiCheatLevel.STANDARD),
    ),
    val saved: SavedResult? = null,
)

private const val DEFAULT_HOUR = 7

@HiltViewModel
class AlarmEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AlarmRepository,
    private val controller: AlarmController,
    private val clock: Clock,
    sections: Set<@JvmSuppressWildcards EditorSection> = emptySet(),
) : ViewModel() {
    /** Secciones aportadas por otros módulos, en orden estable. */
    val sections: List<EditorSection> = sections.sortedForEditor()

    /** `0` = alarma nueva. */
    private val alarmId: Long = savedStateHandle["alarmId"] ?: 0L
    private var original: Alarm? = null

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState

    init {
        viewModelScope.launch {
            val alarm = if (alarmId == 0L) null else repository.get(alarmId)
            original = alarm
            _uiState.value = if (alarm == null) {
                EditorUiState(isLoading = false)
            } else {
                EditorUiState(
                    isLoading = false,
                    isNew = false,
                    hour = alarm.hour,
                    minute = alarm.minute,
                    repeatDays = alarm.repeatDays,
                    label = alarm.label.orEmpty(),
                    draft = alarm,
                )
            }
        }
    }

    fun updateDraft(draft: Alarm) = _uiState.update { it.copy(draft = draft) }

    /** Falso si alguna sección rechaza la alarma en edición. */
    fun canSave(state: EditorUiState = _uiState.value): Boolean = sections.all { it.isValid(state.draft) }

    fun setLabel(label: String) = _uiState.update { it.copy(label = label) }

    fun toggleDay(day: DayOfWeek) = _uiState.update {
        it.copy(repeatDays = if (day in it.repeatDays) it.repeatDays - day else it.repeatDays + day)
    }

    /** Guarda con la hora elegida en el selector; conserva el resto de campos de una alarma existente. */
    fun save(hour: Int, minute: Int) {
        val state = _uiState.value
        viewModelScope.launch {
            if (!canSave(state)) return@launch
            val alarm = state.draft.copy(
                hour = hour,
                minute = minute,
                repeatDays = state.repeatDays,
                label = state.label.trim().ifEmpty { null },
            )
            val schedule = controller.save(alarm)
            val remaining = alarm.nextOccurrence(clock)?.let { Duration.between(ZonedDateTime.now(clock), it) }
            _uiState.update { it.copy(hour = hour, minute = minute, saved = SavedResult(schedule, remaining)) }
        }
    }
}

/** Minutos totales redondeados hacia arriba, desglosados en días, horas y minutos. */
data class RemainingTime(val days: Long, val hours: Long, val minutes: Long)

fun Duration.toRemainingTime(): RemainingTime {
    val totalMinutes = (seconds + 59) / 60
    return RemainingTime(totalMinutes / (24 * 60), totalMinutes / 60 % 24, totalMinutes % 60)
}
