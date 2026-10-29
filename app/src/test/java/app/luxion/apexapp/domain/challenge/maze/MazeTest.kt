package app.luxion.apexapp.domain.challenge.maze

import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.MazeTrace
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MazeTest {
    private fun maze(seed: Long = 7, cols: Int = 6, rows: Int = 7) = Maze.generate(cols, rows, seed)

    private fun centerX(maze: Maze, cell: Int) = maze.col(cell) + 0.5f

    private fun centerY(maze: Maze, cell: Int) = maze.row(cell) + 0.5f

    @Test
    fun sameSeedSameMaze() {
        assertEquals(maze(3).solution, maze(3).solution)
        assertEquals(maze(3).walls, maze(3).walls)
        assertNotEquals(maze(3).walls, maze(4).walls)
    }

    @Test
    fun mazeIsPerfectWithUniqueSolution() {
        repeat(30) { seed ->
            val m = maze(seed.toLong())
            // Un árbol de expansión tiene cellCount - 1 pasos abiertos: conexo y sin ciclos.
            val openEdges = (0 until m.cellCount).sumOf { m.neighbors(it).size } / 2
            assertEquals(m.cellCount - 1, openEdges)
            val seen = mutableSetOf(m.start)
            val queue = ArrayDeque(listOf(m.start))
            while (queue.isNotEmpty()) m.neighbors(queue.removeFirst()).forEach { if (seen.add(it)) queue.add(it) }
            assertEquals(m.cellCount, seen.size)
            assertEquals(m.start, m.solution.first())
            assertEquals(m.exit, m.solution.last())
            assertEquals(m.solution.size, m.solution.toSet().size)
            assertTrue(m.solution.zipWithNext().all { (a, b) -> m.isOpen(a, b) })
        }
    }

    @Test
    fun isOpenIsFalseForDistantAndWrappedCells() {
        val m = maze()
        assertFalse(m.isOpen(0, 2))
        // La última celda de una fila y la primera de la siguiente no son contiguas.
        assertFalse(m.isOpen(m.cols - 1, m.cols))
    }

    @Test
    fun sizesGrowWithLevelAndFitSmallScreens() {
        val cells = (1..5).map { level -> MazeLevels.sizeFor(level).let { it.cols * it.rows } }
        assertTrue(cells.zipWithNext().all { (a, b) -> a < b })
        val max = MazeLevels.sizeFor(5)
        // 320 dp de ancho con 20 dp de margen por lado, y altura que no obliga a desplazar.
        assertTrue(max.cols * MazeLayout.MIN_CELL_DP <= 280f)
        assertTrue(max.rows * MazeLayout.MIN_CELL_DP <= 360f)
        assertEquals(MazeLevels.sizeFor(1), MazeLevels.sizeFor(-4))
        assertEquals(MazeLevels.sizeFor(5), MazeLevels.sizeFor(99))
    }

    @Test
    fun corridorStaysTouchable() {
        assertTrue(MazeLayout.MIN_CELL_DP - MazeLayout.WALL_DP >= 32f)
        // En una pantalla estrecha la celda nunca baja del mínimo; en una ancha no pasa del máximo.
        assertEquals(MazeLayout.MIN_CELL_DP, MazeLayout.cellDp(200f, 600f, MazeSize(7, 9)), 0f)
        assertEquals(MazeLayout.MAX_CELL_DP, MazeLayout.cellDp(2000f, 2000f, MazeSize(4, 5)), 0f)
        assertEquals(50f, MazeLayout.cellDp(350f, 600f, MazeSize(7, 9)), 0.001f)
    }

    @Test
    fun followingTheSolutionReachesExit() {
        val m = maze()
        val tracker = MazeTracker(m, 0.1f)
        assertTrue(tracker.begin(centerX(m, m.start), centerY(m, m.start)))
        var status: TraceStatus? = null
        m.solution.drop(1).forEach { status = tracker.move(centerX(m, it), centerY(m, it)) }
        assertEquals(TraceStatus.REACHED_EXIT, status)
        assertEquals(m.solution, tracker.trace)
        assertTrue(MazeInstanceFor(m).check(MazeTrace(MazeChallenge.TYPE, tracker.trace)))
    }

    @Test
    fun fastMoveThroughWallsIsCaught() {
        val m = maze()
        val tracker = MazeTracker(m, 0.1f)
        tracker.begin(centerX(m, m.start), centerY(m, m.start))
        // Salto en línea recta a una celda que el camino no une directamente: cruza paredes.
        val far = (0 until m.cellCount).first { m.col(it) == m.cols - 1 && m.row(it) == 0 && it != m.exit }
        assertEquals(TraceStatus.HIT_WALL, tracker.move(centerX(m, far), centerY(m, far)))
    }

    @Test
    fun hittingBorderEndsTraceUntilNewBegin() {
        val m = maze()
        val tracker = MazeTracker(m, 0.1f)
        tracker.begin(0.5f, 0.5f)
        assertEquals(TraceStatus.HIT_WALL, tracker.move(-0.3f, 0.5f))
        assertFalse(tracker.active)
        assertEquals(TraceStatus.HIT_WALL, tracker.move(0.5f, 0.5f))
        assertTrue(tracker.begin(0.5f, 0.5f))
        assertTrue(tracker.active)
    }

    @Test
    fun crossingClosedWallFailsButMovingInsideCellDoesNot() {
        val m = maze()
        val closed = listOf(1, m.cols).first { !m.isOpen(m.start, it) }
        val tracker = MazeTracker(m, 0.1f)
        tracker.begin(0.5f, 0.5f)
        assertEquals(TraceStatus.TRACING, tracker.move(0.3f, 0.3f))
        assertEquals(TraceStatus.HIT_WALL, tracker.move(centerX(m, closed), centerY(m, closed)))
    }

    @Test
    fun beginOutsideStartCellOrOnWallIsIgnored() {
        val m = maze()
        val tracker = MazeTracker(m, 0.1f)
        assertFalse(tracker.begin(centerX(m, m.exit), centerY(m, m.exit)))
        assertFalse(tracker.active)
        // El borde exterior (x = 0) es pared.
        assertFalse(tracker.begin(0.02f, 0.5f))
        assertNull(tracker.status)
    }

    @Test
    fun resetDiscardsTrace() {
        val m = maze()
        val tracker = MazeTracker(m, 0.1f)
        tracker.begin(0.5f, 0.5f)
        tracker.reset()
        assertFalse(tracker.active)
        assertTrue(tracker.trace.isEmpty())
        assertNull(tracker.move(0.6f, 0.6f))
    }

    @Test
    fun checkAcceptsValidPathOnly() {
        val instance = MazeChallenge().generate(3, "", Random(5)) as MazeInstance
        val solution = instance.maze.solution
        val type = MazeChallenge.TYPE
        assertTrue(instance.check(MazeTrace(type, solution)))
        assertFalse(instance.check(MazeTrace(type, solution.dropLast(1))))
        assertFalse(instance.check(MazeTrace(type, solution.drop(1))))
        assertFalse(instance.check(MazeTrace(type, emptyList())))
        assertFalse(instance.check(MazeTrace(type, listOf(instance.maze.start, instance.maze.exit))))
        assertFalse(instance.check(ChallengeAnswer.Text("maze")))
        assertFalse(instance.check(MazeTrace(ChallengeType("other"), solution)))
    }

    @Test
    fun backtrackingOverOwnTraceStillSolves() {
        val instance = MazeChallenge().generate(2, "", Random(9)) as MazeInstance
        val s = instance.maze.solution
        val detour = s.take(2) + s.take(1) + s.drop(1)
        assertTrue(instance.check(MazeTrace(MazeChallenge.TYPE, detour)))
    }

    @Test
    fun generationUsesLevelAndIgnoresParams() {
        val challenge = MazeChallenge()
        val low = challenge.generate(1, "{\"cols\":99}", Random(1)) as MazeInstance
        val high = challenge.generate(5, "", Random(1)) as MazeInstance
        assertEquals(MazeLevels.sizeFor(1), MazeSize(low.maze.cols, low.maze.rows))
        assertEquals(MazeLevels.sizeFor(5), MazeSize(high.maze.cols, high.maze.rows))
        assertTrue(high.maze.cellCount > low.maze.cellCount)
        assertEquals(1, (challenge.generate(-3, "", Random(1)) as MazeInstance).level)
        assertEquals(5, (challenge.generate(40, "", Random(1)) as MazeInstance).level)
    }

    @Test
    fun sameRandomSameInstanceDifferentRandomDiffers() {
        val challenge = MazeChallenge()
        val a = challenge.generate(4, "", Random(11)) as MazeInstance
        val b = challenge.generate(4, "", Random(11)) as MazeInstance
        val c = challenge.generate(4, "", Random(12)) as MazeInstance
        assertEquals(a.seed, b.seed)
        assertEquals(a.maze.walls, b.maze.walls)
        assertNotEquals(a.maze.walls, c.maze.walls)
        assertTrue(a.keepOnFailure)
    }

    private fun MazeInstanceFor(maze: Maze) = MazeInstance(1, 0L, maze)
}
