package app.luxion.apexapp.domain.challenge.maze

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/** Estado de un trazo en marcha o terminado. */
enum class TraceStatus { TRACING, HIT_WALL, REACHED_EXIT }

/**
 * Seguimiento del trazo del dedo sobre un [Maze]. Las coordenadas están en unidades de celda (0..cols, 0..rows) y
 * [wallThickness] es el grosor de pared en esas mismas unidades.
 *
 * Un trazo empieza al apoyar el dedo en la celda de inicio ([begin]). Cada [move] interpola entre el punto anterior
 * y el nuevo para que un movimiento rápido no atraviese una pared entre dos eventos táctiles. Tocar una pared (o
 * salirse del laberinto) deja el trazo en [TraceStatus.HIT_WALL] y hay que reiniciarlo con [begin].
 */
class MazeTracker(private val maze: Maze, wallThickness: Float) {
    private val half = wallThickness / 2f
    private val cells = mutableListOf<Int>()
    private var lastX = 0f
    private var lastY = 0f

    var status: TraceStatus? = null
        private set

    /** Celdas recorridas en orden, sin repetir la misma celda dos veces seguidas. */
    val trace: List<Int> get() = cells.toList()

    /** El trazo está en marcha (empezó y no ha chocado ni llegado). */
    val active: Boolean get() = status == TraceStatus.TRACING

    /** Intenta empezar el trazo en ([x], [y]); solo vale dentro de la celda de inicio y fuera de paredes. */
    fun begin(x: Float, y: Float): Boolean {
        reset()
        if (cellAt(x, y) != maze.start || hitsWall(x, y)) return false
        cells += maze.start
        lastX = x
        lastY = y
        status = TraceStatus.TRACING
        return true
    }

    /** Avanza el trazo hasta ([x], [y]). Sin trazo activo no hace nada y devuelve el estado actual (o null). */
    fun move(x: Float, y: Float): TraceStatus? {
        if (!active) return status
        val distance = maxOf(abs(x - lastX), abs(y - lastY))
        // Pasos de media pared como mucho: ninguna pared cabe entre dos muestras.
        val steps = ceil(distance / half).toInt().coerceAtLeast(1)
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val px = lastX + (x - lastX) * t
            val py = lastY + (y - lastY) * t
            if (hitsWall(px, py)) {
                status = TraceStatus.HIT_WALL
                return status
            }
            val cell = cellAt(px, py)
            if (cell != cells.last()) cells += cell
            if (cell == maze.exit) {
                status = TraceStatus.REACHED_EXIT
                return status
            }
        }
        lastX = x
        lastY = y
        return status
    }

    /** Descarta el trazo (p. ej. al soltar el dedo antes de llegar). */
    fun reset() {
        cells.clear()
        status = null
    }

    private fun cellAt(x: Float, y: Float): Int =
        floor(y).toInt().coerceIn(0, maze.rows - 1) * maze.cols + floor(x).toInt().coerceIn(0, maze.cols - 1)

    private fun hitsWall(x: Float, y: Float): Boolean {
        if (x < 0f || y < 0f || x >= maze.cols || y >= maze.rows) return true
        return maze.walls.any { w ->
            x >= minOf(w.x1, w.x2) - half && x <= maxOf(w.x1, w.x2) + half &&
                y >= minOf(w.y1, w.y2) - half && y <= maxOf(w.y1, w.y2) + half
        }
    }
}
