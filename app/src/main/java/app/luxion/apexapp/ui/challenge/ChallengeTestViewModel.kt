package app.luxion.apexapp.ui.challenge

import androidx.lifecycle.ViewModel
import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.challenge.ChallengeRegistry
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.SessionConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ChallengeTestForm(
    val difficulty: Int = SessionConfig.DEFAULT_DIFFICULTY,
    val count: Int = 3,
    val mode: ChallengeMode = ChallengeMode.CHAINED,
    val types: Set<ChallengeType> = setOf(ChallengeType.MATH),
)

/** Sesión en marcha: el `sessionId` es propio de la prueba (en el flujo real lo crea AlarmFiringHandler). */
data class RunningTest(val session: SessionConfig, val sessionId: String)

@HiltViewModel
class ChallengeTestViewModel @Inject constructor(
    registry: ChallengeRegistry,
    renderers: Set<@JvmSuppressWildcards ChallengeRenderer> = emptySet(),
) : ViewModel() {
    private val renderersByType = renderers.associateBy { it.type }

    fun rendererFor(type: ChallengeType): ChallengeRenderer? = renderersByType[type]

    /** Orden estable para que las fichas no salten de sitio. */
    val availableTypes: List<ChallengeType> = registry.types.sortedBy { it.id }

    private val _form = MutableStateFlow(ChallengeTestForm())
    val form: StateFlow<ChallengeTestForm> = _form.asStateFlow()

    private val _running = MutableStateFlow<RunningTest?>(null)
    val running: StateFlow<RunningTest?> = _running.asStateFlow()

    fun setDifficulty(level: Int) = _form.update {
        it.copy(difficulty = level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY))
    }

    fun setCount(count: Int) = _form.update {
        it.copy(count = SessionConfig.normalizeCount(count, it.types.size))
    }

    fun setMode(mode: ChallengeMode) = _form.update { it.copy(mode = mode) }

    /** Al añadir tipos el número de retos sube solo si hace falta; al quitarlos no baja. */
    fun toggleType(type: ChallengeType) = _form.update {
        val types = if (type in it.types) it.types - type else it.types + type
        it.copy(types = types, count = SessionConfig.normalizeCount(it.count, types.size))
    }

    /** Sin tipos marcados la sesión usa matemáticas por defecto, como en una alarma sin tipos. */
    fun start() {
        val form = _form.value
        val session = SessionConfig(
            difficulty = form.difficulty,
            count = SessionConfig.normalizeCount(form.count, form.types.size),
            mode = form.mode,
            challenges = availableTypes.filter { it in form.types }.map { ChallengeSpec(it.id) },
        )
        _running.value = RunningTest(session, UUID.randomUUID().toString())
    }

    fun stop() {
        _running.value = null
    }
}
