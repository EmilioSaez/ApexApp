package app.luxion.apexapp.ui.challenge

import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.SessionConfig

/** Transformaciones puras de la configuración de retos que usa el editor. */

fun ChallengeConfig.isEnabled(type: ChallengeType): Boolean = challenges.any { it.type == type.id }

/** Mínimo de retos para los tipos elegidos: uno de cada tipo. */
val ChallengeConfig.minCount: Int get() = SessionConfig.minCount(challenges.size)

/** Nº de retos que se muestra: nunca menos que [minCount], también en alarmas ya guardadas. */
val ChallengeConfig.displayCount: Int get() = SessionConfig.normalizeCount(count, challenges.size)

/**
 * Habilita el tipo añadiéndolo al final de la cadena, o lo quita si ya estaba. Al añadir tipos el número de
 * retos sube solo si hace falta; al quitarlos no baja.
 */
fun ChallengeConfig.toggled(type: ChallengeType): ChallengeConfig =
    if (isEnabled(type)) {
        copy(challenges = challenges.filterNot { it.type == type.id })
    } else {
        val added = challenges + ChallengeSpec(type.id)
        copy(challenges = added, count = SessionConfig.normalizeCount(count, added.size))
    }

fun ChallengeConfig.withCount(count: Int): ChallengeConfig =
    copy(count = SessionConfig.normalizeCount(count, challenges.size))

fun ChallengeConfig.withDifficulty(level: Int): ChallengeConfig =
    copy(difficulty = level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY))

fun ChallengeConfig.withMode(mode: ChallengeMode): ChallengeConfig = copy(mode = mode)

fun ChallengeConfig.withParams(type: ChallengeType, params: String): ChallengeConfig =
    copy(challenges = challenges.map { if (it.type == type.id) it.copy(params = params) else it })

/** Dificultad que se muestra: 0 (sin definir) equivale a la dificultad por defecto. */
val ChallengeConfig.displayDifficulty: Int get() = SessionConfig.normalizeDifficulty(difficulty)
