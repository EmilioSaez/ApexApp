package app.luxion.apexapp.ui.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeSession
import app.luxion.apexapp.domain.challenge.ChallengeSessionFactory
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.SessionConfig
import app.luxion.apexapp.domain.challenge.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ChallengeSessionViewModel @Inject constructor(
    private val factory: ChallengeSessionFactory,
    renderers: Set<@JvmSuppressWildcards ChallengeRenderer>,
) : ViewModel() {
    private val renderersByType = renderers.associateBy { it.type }
    private var session: ChallengeSession? = null

    private val _state = MutableStateFlow<SessionState?>(null)

    /** `null` hasta que se llama a [start]. */
    val state: StateFlow<SessionState?> = _state.asStateFlow()

    /** Inicia la sesión una sola vez: recomponer o rotar la pantalla no la reinicia. */
    fun start(config: SessionConfig, sessionId: String, alarmId: Long? = null) {
        if (session != null) return
        val created = factory.create(config, sessionId, alarmId)
        session = created
        viewModelScope.launch { created.state.collect { _state.value = it } }
    }

    fun submit(answer: ChallengeAnswer) {
        session?.submit(answer)
    }

    fun rendererFor(type: ChallengeType): ChallengeRenderer? = renderersByType[type]
}
