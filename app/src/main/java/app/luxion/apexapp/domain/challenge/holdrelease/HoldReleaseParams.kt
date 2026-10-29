package app.luxion.apexapp.domain.challenge.holdrelease

import app.luxion.apexapp.domain.challenge.SessionConfig

/** Rondas del reto de mantener y soltar: salen solo de la dificultad, sin configuración del usuario. */
object HoldReleaseParams {
    /** Rondas por nivel de dificultad (índice 0 = nivel 1). Tabla única: ajustar aquí al calibrar. */
    val ROUNDS_BY_LEVEL: List<Int> = listOf(2, 3, 3, 4, 5)

    fun rounds(level: Int): Int =
        ROUNDS_BY_LEVEL[level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY) - SessionConfig.MIN_DIFFICULTY]
}

/** Exigencia de un nivel: ventana de acierto tras la señal. La duración del reto no depende del nivel. */
data class HoldReleaseLevel(val windowMs: Long)

/** Tabla única de calibración, por nivel de dificultad (índice 0 = nivel 1). */
object HoldReleaseTuning {
    val BY_LEVEL: List<HoldReleaseLevel> = listOf(1_200L, 1_000L, 800L, 650L, 500L).map(::HoldReleaseLevel)

    fun forLevel(level: Int): HoldReleaseLevel = BY_LEVEL[
        level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY) - SessionConfig.MIN_DIFFICULTY
    ]
}
