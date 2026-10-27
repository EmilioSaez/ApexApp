package app.luxion.apexapp.domain.challenge

import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SessionState(
    val solved: Int,
    val total: Int,
    val current: ChallengeInstance?,
    val passed: Boolean,
    /** La última respuesta fue incorrecta. */
    val lastAnswerWrong: Boolean = false,
)

/**
 * Sesión de retos: presenta [SessionConfig.count] retos y se supera al resolverlos todos.
 * [sessionId] y [alarmId] vienen de quien la lanza (AlarmFiringHandler o la pantalla de prueba).
 */
class ChallengeSession(
    private val sessionId: String,
    private val alarmId: Long?,
    private val config: SessionConfig,
    private val registry: ChallengeRegistry,
    private val difficultyProvider: DifficultyProvider,
    private val eventSink: ChallengeEventSink,
    private val random: Random = Random.Default,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val pool: List<ChallengeSpec> = config.challenges.filter { ChallengeType(it.type) in registry }
    private val mathSpec = ChallengeSpec(ChallengeType.MATH.id)

    private val total = config.effectiveCount

    /** En modo aleatorio sale primero uno de cada tipo, en orden aleatorio, y después cualquiera. */
    private val firstRound = ArrayDeque(pool.distinctBy { it.type }.shuffled(random))

    private val solvedInstances = mutableListOf<ChallengeInstance>()

    private var index = 0
    private var failures = 0
    private var currentSpec: ChallengeSpec = mathSpec

    private val _state = MutableStateFlow(SessionState(0, total, null, false))
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        emit { ChallengeEvent.SessionStarted(sessionId, alarmId, it, total, config.difficulty) }
        startChallenge()
    }

    fun submit(answer: ChallengeAnswer) {
        val current = _state.value.current ?: return
        if (_state.value.passed) return
        if (current.check(answer)) {
            solvedInstances += current
            emit { ChallengeEvent.ChallengeSolved(sessionId, alarmId, it, current.type, current.level, index) }
            index++
            if (index >= total) {
                emit { ChallengeEvent.SessionFinished(sessionId, alarmId, it, total) }
                _state.value = SessionState(index, total, null, passed = true)
            } else {
                startChallenge()
            }
        } else {
            emit { ChallengeEvent.ChallengeFailed(sessionId, alarmId, it, current.type, current.level, index) }
            failures++
            _state.value = _state.value.copy(current = generate(currentSpec), lastAnswerWrong = true)
        }
    }

    /** El reto en curso no puede completarse (p. ej. permiso revocado): se sustituye por la reserva. */
    fun reportUnavailable(reason: String) {
        if (_state.value.current == null || _state.value.passed) return
        val spec = fallbackSpec(ChallengeType(currentSpec.type), reason)
        currentSpec = spec
        _state.value = _state.value.copy(current = generate(spec), lastAnswerWrong = false)
    }

    private fun startChallenge() {
        failures = 0
        var spec = select()
        val availability = registry[ChallengeType(spec.type)]?.availability()
        if (availability is Availability.Unavailable) {
            spec = fallbackSpec(ChallengeType(spec.type), availability.reason)
        }
        currentSpec = spec
        val instance = generate(spec)
        emit { ChallengeEvent.ChallengeStarted(sessionId, alarmId, it, instance.type, instance.level, index) }
        _state.value = SessionState(index, total, instance, passed = false)
    }

    private fun select(): ChallengeSpec = when {
        pool.isEmpty() -> mathSpec
        config.mode == ChallengeMode.CHAINED -> pool[index % pool.size]
        firstRound.isNotEmpty() -> firstRound.removeFirst()
        else -> pool[random.nextInt(pool.size)]
    }

    /** Otro tipo habilitado y disponible; si no hay, matemáticas. Emite el evento de reserva. */
    private fun fallbackSpec(from: ChallengeType, reason: String): ChallengeSpec {
        val alternative = pool.firstOrNull {
            val type = ChallengeType(it.type)
            type != from && registry[type]?.availability() == Availability.Available
        } ?: mathSpec
        emit {
            ChallengeEvent.ChallengeFellBack(sessionId, alarmId, it, from, ChallengeType(alternative.type), reason, index)
        }
        return alternative
    }

    private fun generate(spec: ChallengeSpec): ChallengeInstance {
        val challenge = registry[ChallengeType(spec.type)] ?: registry[ChallengeType.MATH]
            ?: error("math challenge not registered")
        val level = difficultyProvider
            .levelFor(config, ChallengeContext(sessionId, alarmId, challenge.type, index, failures))
            .coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY)
        return challenge.generate(level, spec.params, random, solvedInstances)
    }

    private fun emit(event: (Long) -> ChallengeEvent) = eventSink.onEvent(event(clock()))
}
