package app.luxion.apexapp.domain.challenge.rhythm

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RhythmLogicTest {
    private val notes = listOf(RhythmNote(0, 1000), RhythmNote(1, 2000), RhythmNote(2, 3000))

    @Test
    fun `parametros ausentes, ilegibles o fuera de rango dan el defecto o se acotan`() {
        assertEquals(RhythmParams.DEFAULT_ROUNDS, RhythmParams.rounds(""))
        assertEquals(RhythmParams.DEFAULT_ROUNDS, RhythmParams.rounds("no json"))
        assertEquals(RhythmParams.DEFAULT_ROUNDS, RhythmParams.rounds("{}"))
        assertEquals(RhythmParams.MAX_ROUNDS, RhythmParams.rounds("""{"rounds":99}"""))
        assertEquals(RhythmParams.MIN_ROUNDS, RhythmParams.rounds("""{"rounds":0,"x":1}"""))
    }

    @Test
    fun `los parametros hacen ida y vuelta`() {
        assertEquals(6, RhythmParams.rounds(RhythmParams.paramsFor(6)))
        assertEquals(RhythmParams.MAX_ROUNDS, RhythmParams.rounds(RhythmParams.paramsFor(50)))
    }

    @Test
    fun `el patron tiene las notas y carriles esperados y espaciado regular`() {
        val pattern = RhythmPattern.generate(Random(1))
        assertEquals(RhythmTuning.NOTES_PER_ROUND, pattern.size)
        assertTrue(pattern.all { it.lane in 0 until RhythmTuning.LANES })
        assertEquals(RhythmTuning.FALL_MS, pattern.first().timeMs)
        assertTrue(pattern.zipWithNext().all { (a, b) -> b.timeMs - a.timeMs == RhythmTuning.NOTE_SPACING_MS })
    }

    @Test
    fun `las notas caen sobre pulsos de la musica`() {
        assertEquals(500L, RhythmTuning.BEAT_MS)
        assertTrue(RhythmPattern.generate(Random(1)).all { it.timeMs % RhythmTuning.BEAT_MS == 0L })
    }

    @Test
    fun `el patron no repite un carril mas de lo permitido`() {
        repeat(200) { seed ->
            val lanes = RhythmPattern.generate(Random(seed)).map { it.lane }
            var run = 1
            var maxRun = 1
            for (i in 1 until lanes.size) {
                run = if (lanes[i] == lanes[i - 1]) run + 1 else 1
                maxRun = maxOf(maxRun, run)
            }
            assertTrue("seed $seed", maxRun <= RhythmTuning.MAX_SAME_LANE_IN_A_ROW)
        }
    }

    @Test
    fun `semillas distintas dan patrones distintos`() {
        assertNotEquals(RhythmPattern.generate(Random(1)), RhythmPattern.generate(Random(2)))
    }

    @Test
    fun `acertar todas las notas supera la ronda`() {
        val round = RhythmRound(notes)
        notes.forEach { round.tap(it.lane, it.timeMs + 100) }
        assertEquals(RhythmRound.Status.CLEARED, round.status)
        assertEquals(3, round.hits)
        assertEquals(0, round.failures)
    }

    @Test
    fun `una nota perdida es un fallo y la ronda continua`() {
        val round = RhythmRound(notes)
        round.advance(1000 + RhythmTuning.HIT_WINDOW_MS + 1)
        assertEquals(1, round.failures)
        assertEquals(RhythmRound.Status.PLAYING, round.status)
    }

    @Test
    fun `un toque en un carril sin nota es un fallo`() {
        val round = RhythmRound(notes)
        round.tap(2, 1000)
        assertEquals(1, round.failures)
        assertEquals(0, round.hits)
    }

    @Test
    fun `un toque fuera de la ventana es un fallo`() {
        val round = RhythmRound(notes)
        round.tap(0, 1000 + RhythmTuning.HIT_WINDOW_MS + 1)
        assertEquals(0, round.hits)
        assertTrue(round.failures >= 1)
    }

    @Test
    fun `una nota solo se acierta una vez`() {
        val round = RhythmRound(notes)
        round.tap(0, 1000)
        round.tap(0, 1010)
        assertEquals(1, round.hits)
        assertEquals(1, round.failures)
    }

    @Test
    fun `tres fallos piden reiniciar la ronda`() {
        val round = RhythmRound(notes)
        repeat(RhythmTuning.MAX_FAILURES) { round.tap(2, 500) }
        assertEquals(RhythmRound.Status.RESTART, round.status)
    }

    @Test
    fun `uno o dos fallos no impiden superar la ronda`() {
        val round = RhythmRound(notes)
        round.tap(2, 500)
        round.tap(0, 1000)
        round.tap(1, 2000)
        round.advance(3000 + RhythmTuning.HIT_WINDOW_MS + 1)
        assertEquals(2, round.failures)
        assertEquals(RhythmRound.Status.CLEARED, round.status)
    }

    @Test
    fun `tras terminar la ronda los toques se ignoran`() {
        val round = RhythmRound(notes)
        repeat(RhythmTuning.MAX_FAILURES) { round.tap(2, 500) }
        round.tap(0, 1000)
        assertEquals(0, round.hits)
        assertEquals(RhythmTuning.MAX_FAILURES, round.failures)
    }
}
