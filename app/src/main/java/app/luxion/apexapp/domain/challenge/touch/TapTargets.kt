package app.luxion.apexapp.domain.challenge.touch

import app.luxion.apexapp.domain.challenge.SessionConfig
import kotlin.math.hypot
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Lectura y escritura de `ChallengeSpec.params` de Tocar objetivos: solo el número de rondas. */
object TapTargetsParams {
    const val MIN_ROUNDS = 3
    const val MAX_ROUNDS = 15
    const val DEFAULT_ROUNDS = 8

    @Serializable
    private data class Stored(val rounds: Int = DEFAULT_ROUNDS)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Rondas de [params]; ausente o inválido da el valor por defecto y fuera de rango se acota. */
    fun rounds(params: String): Int =
        runCatching { json.decodeFromString<Stored>(params).rounds }.getOrDefault(DEFAULT_ROUNDS)
            .coerceIn(MIN_ROUNDS, MAX_ROUNDS)

    fun paramsFor(rounds: Int): String = json.encodeToString(Stored(rounds.coerceIn(MIN_ROUNDS, MAX_ROUNDS)))
}

/** Exigencia de un nivel: cuántos círculos, de qué tamaño y cuánto tiempo hay para tocarlos. */
data class TapTargetsLevel(val circles: Int, val diameterDp: Int, val timeLimitMs: Long)

/** Tabla única de calibración, por nivel de dificultad (índice 0 = nivel 1). */
object TapTargetsTuning {
    val BY_LEVEL: List<TapTargetsLevel> = listOf(
        TapTargetsLevel(circles = 2, diameterDp = 72, timeLimitMs = 4_000),
        TapTargetsLevel(circles = 3, diameterDp = 64, timeLimitMs = 3_500),
        TapTargetsLevel(circles = 3, diameterDp = 56, timeLimitMs = 3_000),
        TapTargetsLevel(circles = 4, diameterDp = 52, timeLimitMs = 2_500),
        TapTargetsLevel(circles = 5, diameterDp = 48, timeLimitMs = 2_000),
    )

    /** Pausa aleatoria entre rondas. */
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
 * Estado puro del juego. Una ronda fallida no cuenta y añade una ronda extra mientras haya menos extras que
 * rondas configuradas; superado el tope se repite sin crecer. Nunca se pierden rondas ya superadas. El tiempo
 * lo controla quien lo usa: llama a [expire] cuando vence el plazo de la ronda.
 */
class TapTargetsGame(
    private val rounds: Int,
    level: Int,
    private val random: Random,
) {
    private val tuning = TapTargetsTuning.forLevel(level)

    var solved = 0
        private set
    var extras = 0
        private set

    val total: Int get() = rounds + extras
    val done: Boolean get() = solved >= total

    private var current: TapRound? = null
    private val hit = mutableSetOf<Int>()

    /** Índices de los círculos ya tocados en la ronda en curso. */
    val hitCircles: Set<Int> get() = hit

    /** Genera la ronda siguiente (o la repetición de la fallida) con posiciones nuevas. */
    fun nextRound(areaWidthDp: Float, areaHeightDp: Float): TapRound {
        val radius = tuning.diameterDp / 2f
        val circles = ArrayList<Circle>()
        var attempts = 0
        while (circles.size < tuning.circles) {
            val candidate = Circle(
                xDp = randomIn(radius, areaWidthDp - radius),
                yDp = randomIn(radius, areaHeightDp - radius),
            )
            attempts++
            // Pasado el límite se acepta aunque se rocen: una zona diminuta no debe bloquear la alarma.
            val free = circles.none { hypot(it.xDp - candidate.xDp, it.yDp - candidate.yDp) < tuning.diameterDp * 1.1f }
            if (free || attempts > MAX_ATTEMPTS) circles += candidate
        }
        hit.clear()
        val round = TapRound(
            circles = circles,
            diameterDp = tuning.diameterDp,
            timeLimitMs = tuning.timeLimitMs,
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

    /** Venció el plazo de la ronda en curso: no cuenta y se añade una ronda extra si hay tope libre. */
    fun expire() {
        if (current == null) return
        current = null
        if (extras < rounds) extras++
    }

    private fun randomIn(min: Float, max: Float): Float =
        if (max <= min) (min + max) / 2f else min + random.nextFloat() * (max - min)

    private companion object {
        const val MAX_ATTEMPTS = 200
    }
}
