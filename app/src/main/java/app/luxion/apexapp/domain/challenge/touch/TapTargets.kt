package app.luxion.apexapp.domain.challenge.touch

import app.luxion.apexapp.domain.challenge.SessionConfig
import kotlin.math.hypot
import kotlin.random.Random

/** Exigencia de un nivel: cuántos círculos hay y el tiempo para tocar cada uno (la ronda dura su producto). */
data class TapTargetsLevel(val circles: Int, val timePerTargetMs: Long) {
    val roundMs: Long get() = circles * timePerTargetMs
}

/** Tabla única de calibración, por nivel de dificultad (índice 0 = nivel 1). */
object TapTargetsTuning {
    const val DIAMETER_DP = 56

    val BY_LEVEL: List<TapTargetsLevel> = listOf(
        TapTargetsLevel(circles = 3, timePerTargetMs = 1_600),
        TapTargetsLevel(circles = 4, timePerTargetMs = 1_300),
        TapTargetsLevel(circles = 5, timePerTargetMs = 1_000),
        TapTargetsLevel(circles = 6, timePerTargetMs = 800),
        TapTargetsLevel(circles = 7, timePerTargetMs = 650),
    )

    /** Pausa aleatoria antes de cada intento. */
    const val GAP_MIN_MS = 600L
    const val GAP_MAX_MS = 1_200L

    /** Margen extra al tocar, para no fallar por unos píxeles. */
    const val TOUCH_SLOP_DP = 8f

    fun forLevel(level: Int): TapTargetsLevel = BY_LEVEL[
        level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY) - SessionConfig.MIN_DIFFICULTY,
    ]
}

/** Centro de un círculo, en dp dentro de la zona de juego. */
data class Circle(val xDp: Float, val yDp: Float)

data class TapRound(
    val circles: List<Circle>,
    val diameterDp: Int,
    val timeLimitMs: Long,
    /** Pausa antes de que aparezca la ronda. */
    val gapMs: Long,
)

enum class TapResult { MISS, HIT, ROUND_CLEARED }

/**
 * Estado puro del juego: una única ronda con los círculos del nivel. Si vence el plazo se repite con posiciones
 * nuevas hasta superarla. El tiempo lo controla quien lo usa: llama a [expire] cuando vence el plazo.
 */
class TapTargetsGame(
    level: Int,
    private val random: Random,
) {
    private val config = TapTargetsTuning.forLevel(level)

    var solved = 0
        private set

    val done: Boolean get() = solved >= 1

    private var current: TapRound? = null
    private val hit = mutableSetOf<Int>()

    /** Índices de los círculos ya tocados en la ronda en curso. */
    val hitCircles: Set<Int> get() = hit

    /** Genera la ronda (o la repetición de la fallida) con posiciones nuevas. */
    fun nextRound(areaWidthDp: Float, areaHeightDp: Float): TapRound {
        val radius = TapTargetsTuning.DIAMETER_DP / 2f
        val circles = ArrayList<Circle>()
        var attempts = 0
        while (circles.size < config.circles) {
            val candidate = Circle(
                xDp = randomIn(radius, areaWidthDp - radius),
                yDp = randomIn(radius, areaHeightDp - radius),
            )
            attempts++
            // Pasado el límite se acepta aunque se rocen: una zona diminuta no debe bloquear la alarma.
            val free = circles.none { hypot(it.xDp - candidate.xDp, it.yDp - candidate.yDp) < TapTargetsTuning.DIAMETER_DP * 1.1f }
            if (free || attempts > MAX_ATTEMPTS) circles += candidate
        }
        hit.clear()
        val round = TapRound(
            circles = circles,
            diameterDp = TapTargetsTuning.DIAMETER_DP,
            timeLimitMs = config.roundMs,
            gapMs = random.nextLong(TapTargetsTuning.GAP_MIN_MS, TapTargetsTuning.GAP_MAX_MS + 1),
        )
        current = round
        return round
    }

    /** Toque en ([xDp], [yDp]) dentro de la zona de juego. Fuera de los círculos no hace nada. */
    fun tap(xDp: Float, yDp: Float): TapResult {
        val round = current ?: return TapResult.MISS
        val reach = round.diameterDp / 2f + TapTargetsTuning.TOUCH_SLOP_DP
        val index = round.circles.indices.firstOrNull { i ->
            i !in hit && hypot(round.circles[i].xDp - xDp, round.circles[i].yDp - yDp) <= reach
        } ?: return TapResult.MISS
        hit += index
        if (hit.size < round.circles.size) return TapResult.HIT
        current = null
        solved++
        return TapResult.ROUND_CLEARED
    }

    /** Venció el plazo de la ronda en curso: no cuenta y habrá que repetirla. */
    fun expire() {
        current = null
    }

    private fun randomIn(min: Float, max: Float): Float =
        if (max <= min) (min + max) / 2f else min + random.nextFloat() * (max - min)

    private companion object {
        const val MAX_ATTEMPTS = 200
    }
}
