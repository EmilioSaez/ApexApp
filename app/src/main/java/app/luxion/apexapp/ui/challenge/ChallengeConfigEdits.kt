package app.luxion.apexapp.ui.challenge

import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.SessionConfig

/** Transformaciones puras de la configuración de retos que usa el editor. */

fun ChallengeConfig.isEnabled(type: ChallengeType): Boolean = challenges.any { it.type == type.id }

/** Habilita el tipo añadiéndolo al final de la cadena, o lo quita si ya estaba. */
fun ChallengeConfig.toggled(type: ChallengeType): ChallengeConfig =
    if (isEnabled(type)) {
        copy(challenges = challenges.filterNot { it.type == type.id })
    } else {
        copy(challenges = challenges + ChallengeSpec(type.id))
    }

fun ChallengeConfig.withCount(count: Int): ChallengeConfig =
    copy(count = count.coerceIn(SessionConfig.MIN_COUNT, SessionConfig.MAX_COUNT))

fun ChallengeConfig.withDifficulty(level: Int): ChallengeConfig =
    copy(difficulty = level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY))

fun ChallengeConfig.withMode(mode: ChallengeMode): ChallengeConfig = copy(mode = mode)

fun ChallengeConfig.withParams(type: ChallengeType, params: String): ChallengeConfig =
    copy(challenges = challenges.map { if (it.type == type.id) it.copy(params = params) else it })

/** Dificultad que se muestra: 0 (sin definir) equivale a la dificultad por defecto. */
val ChallengeConfig.displayDifficulty: Int get() = SessionConfig.normalizeDifficulty(difficulty)
