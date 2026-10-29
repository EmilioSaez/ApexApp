package app.luxion.apexapp.domain.challenge.maze

import app.luxion.apexapp.domain.challenge.SessionConfig
import kotlin.random.Random

/** Tamaño del laberinto en celdas. */
data class MazeSize(val cols: Int, val rows: Int)

/**
 * Tamaño del laberinto por nivel (índice 0 = nivel 1). Tabla única: ajustar aquí al calibrar.
 *
 * El tope es 7 columnas × 9 filas. Con [MazeLayout.MIN_CELL_DP] = 40 dp ocupa 280 × 360 dp, que cabe en la
 * pantalla más estrecha soportada (320 dp menos 2 × 20 dp de margen) sin obligar a desplazar la pantalla.
 */
object MazeLevels {
    val SIZE_BY_LEVEL: List<MazeSize> = listOf(
        MazeSize(4, 5),
        MazeSize(5, 6),
        MazeSize(6, 7),
        MazeSize(7, 8),
        MazeSize(7, 9),
    )

    fun sizeFor(level: Int): MazeSize =
        SIZE_BY_LEVEL[level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY) - SessionConfig.MIN_DIFFICULTY]
}

/**
 * Medidas físicas del laberinto. Con celda de 40 dp y pared de 4 dp el pasillo libre mide 36 dp: el trazo sigue
 * el centro del dedo (no su huella, de unos 48 dp), así que sobran ±18 dp de margen para un pulso medio dormido.
 */
object MazeLayout {
    const val MIN_CELL_DP = 40f
    const val MAX_CELL_DP = 64f
    const val WALL_DP = 4f

    /** Pared en unidades de celda. */
    fun wallThickness(cellDp: Float): Float = WALL_DP / cellDp

    /** Lado de celda que cabe en [availableWidthDp] y [availableHeightDp], nunca menor que [MIN_CELL_DP]. */
    fun cellDp(availableWidthDp: Float, availableHeightDp: Float, size: MazeSize): Float =
        minOf(availableWidthDp / size.cols, availableHeightDp / size.rows, MAX_CELL_DP).coerceAtLeast(MIN_CELL_DP)
}

/**
 * Laberinto perfecto (sin ciclos): entre dos celdas hay exactamente un camino, así que la solución es única.
 * Las celdas se numeran `fila * cols + columna`; la salida es la celda más lejana del inicio.
 */
class Maze private constructor(
    val cols: Int,
    val rows: Int,
    private val openRight: BooleanArray,
    private val openDown: BooleanArray,
) {
    val cellCount: Int get() = cols * rows

    /** Celda del icono de alarma: esquina superior izquierda. */
    val start: Int = 0

    val exit: Int

    /** Camino único de [start] a [exit], ambos incluidos. */
    val solution: List<Int>

    init {
        val parent = IntArray(cellCount) { -1 }
        val order = ArrayDeque<Int>().apply { add(start) }
        parent[start] = start
        var last = start
        while (order.isNotEmpty()) {
            val cell = order.removeFirst()
            last = cell
            neighbors(cell).forEach { next ->
                if (parent[next] == -1) {
                    parent[next] = cell
                    order.add(next)
                }
            }
        }
        exit = last
        solution = generateSequence(exit) { if (it == start) null else parent[it] }.toList().asReversed()
    }

    fun col(cell: Int) = cell % cols

    fun row(cell: Int) = cell / cols

    /** Hay paso directo entre dos celdas contiguas. */
    fun isOpen(a: Int, b: Int): Boolean {
        val from = minOf(a, b)
        val to = maxOf(a, b)
        return when (to - from) {
            1 -> row(from) == row(to) && openRight[from]
            cols -> openDown[from]
            else -> false
        }
    }

    /** Celdas contiguas a las que se puede pasar desde [cell]. */
    fun neighbors(cell: Int): List<Int> = buildList {
        if (col(cell) > 0 && openRight[cell - 1]) add(cell - 1)
        if (col(cell) < cols - 1 && openRight[cell]) add(cell + 1)
        if (row(cell) > 0 && openDown[cell - cols]) add(cell - cols)
        if (row(cell) < rows - 1 && openDown[cell]) add(cell + cols)
    }

    /** Segmento de pared entre vértices de la malla, en unidades de celda. */
    data class Wall(val x1: Int, val y1: Int, val x2: Int, val y2: Int)

    /** Paredes cerradas, incluido el borde exterior; una pared por lado de celda. */
    val walls: List<Wall> = buildList {
        for (x in 0 until cols) {
            add(Wall(x, 0, x + 1, 0))
            add(Wall(x, rows, x + 1, rows))
        }
        for (y in 0 until rows) {
            add(Wall(0, y, 0, y + 1))
            add(Wall(cols, y, cols, y + 1))
        }
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val cell = y * cols + x
                if (x < cols - 1 && !openRight[cell]) add(Wall(x + 1, y, x + 1, y + 1))
                if (y < rows - 1 && !openDown[cell]) add(Wall(x, y + 1, x + 1, y + 1))
            }
        }
    }

    companion object {
        /** Backtracker iterativo: el mismo [seed] y tamaño dan siempre el mismo laberinto. */
        fun generate(cols: Int, rows: Int, seed: Long): Maze {
            require(cols >= 2 && rows >= 2) { "maze needs at least 2x2 cells" }
            val random = Random(seed)
            val openRight = BooleanArray(cols * rows)
            val openDown = BooleanArray(cols * rows)
            val visited = BooleanArray(cols * rows)
            val stack = ArrayDeque<Int>()
            visited[0] = true
            stack.add(0)
            while (stack.isNotEmpty()) {
                val cell = stack.last()
                val x = cell % cols
                val y = cell / cols
                val options = buildList {
                    if (x > 0 && !visited[cell - 1]) add(cell - 1)
                    if (x < cols - 1 && !visited[cell + 1]) add(cell + 1)
                    if (y > 0 && !visited[cell - cols]) add(cell - cols)
                    if (y < rows - 1 && !visited[cell + cols]) add(cell + cols)
                }
                if (options.isEmpty()) {
                    stack.removeLast()
                    continue
                }
                val next = options[random.nextInt(options.size)]
                when (next - cell) {
                    1 -> openRight[cell] = true
                    -1 -> openRight[next] = true
                    cols -> openDown[cell] = true
                    else -> openDown[next] = true
                }
                visited[next] = true
                stack.add(next)
            }
            return Maze(cols, rows, openRight, openDown)
        }
    }
}
