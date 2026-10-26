package app.luxion.apexapp.domain.challenge.sensor

import app.luxion.apexapp.domain.challenge.SessionConfig

/** Límites absolutos del objetivo de cada reto de movimiento; la tabla [MotionTargets] debe caber en ellos. */
enum class MotionKind(val typeId: String, val min: Int, val max: Int) {
    SHAKE("shake", min = 10, max = 100),
    STEPS("steps", min = 10, max = 200),

    /** El objetivo es la duración mínima de la marcha, en segundos. */
    STAND_UP("standup", min = 5, max = 60),
    ;

    /**
     * Objetivo para el nivel de dificultad [level] (1..5, se acota). Es el único origen del objetivo: los
     * `params` guardados en alarmas antiguas (`{"target":N}`) se ignoran.
     */
    fun targetFor(level: Int): Int = MotionTargets.BY_LEVEL.getValue(this)[
        level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY) - SessionConfig.MIN_DIFFICULTY,
    ]
}

/**
 * Objetivo por nivel de dificultad (índice 0 = nivel 1) de cada reto de movimiento. Tabla única: ajustar
 * aquí al calibrar. Sacudidas 10→60, pasos 15→120, segundos de marcha 5→40.
 */
object MotionTargets {
    val BY_LEVEL: Map<MotionKind, List<Int>> = mapOf(
        MotionKind.SHAKE to listOf(10, 20, 30, 45, 60),
        MotionKind.STEPS to listOf(15, 30, 55, 85, 120),
        MotionKind.STAND_UP to listOf(5, 10, 20, 30, 40),
    )
}
