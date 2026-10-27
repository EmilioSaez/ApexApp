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
        val default = RhythmParams.defaultRounds(3)
        assertEquals(default, RhythmParams.rounds("", 3))
        assertEquals(default, RhythmParams.rounds("no json", 3))
        assertEquals(default, RhythmParams.rounds("{}", 3))
        assertEquals(RhythmParams.MAX_ROUNDS, RhythmParams.rounds("""{"rounds":99}""", 3))
        assertEquals(RhythmParams.MIN_ROUNDS, RhythmParams.rounds("""{"rounds":0,"x":1}""", 3))
    }

    @Test
    fun `sin params las rondas salen de la dificultad`() {
        assertEquals(RhythmParams.DEFAULT_ROUNDS_BY_LEVEL, (1..5).map { RhythmParams.rounds("", it) })
        assertTrue(RhythmParams.rounds("", 5) > RhythmParams.rounds("", 1))
        assertEquals(RhythmParams.rounds("", 1), RhythmParams.rounds("", -3))
        assertEquals(RhythmParams.rounds("", 5), RhythmParams.rounds("", 99))
    }

    @Test
    fun `los parametros hacen ida y vuelta`() {
        assertEquals(6, RhythmParams.rounds(RhythmParams.paramsFor(6), 3))
        assertEquals(RhythmParams.MAX_ROUNDS, RhythmParams.rounds(RhythmParams.paramsFor(50), 3))
    }

    private val pace = RhythmTuning.pace(3)

    @Test
    fun `el patron tiene las notas y carriles esperados y espaciado regular`() {
        val pattern = RhythmPattern.generate(Random(1), pace)
        assertEquals(6, pattern.size)
        assertTrue(pattern.all { it.lane in 0 until RhythmTuning.LANES })
        assertEquals(pace.fallMs, pattern.first().timeMs)
        assertTrue(pattern.zipWithNext().all { (a, b) -> b.timeMs - a.timeMs == pace.noteSpacingMs })
    }

    @Test
    fun `las notas caen sobre pulsos de la musica`() {
        assertEquals(500L, pace.beatMs)
        RhythmTuning.let { (1..5).map(it::pace) }.forEach { p ->
            assertTrue(RhythmPattern.generate(Random(1), p).all { it.timeMs % p.beatMs == 0L })
        }
    }

    @Test
    fun `mas dificultad es mas rapido`() {
        val paces = (1..5).map(RhythmTuning::pace)
        assertTrue(paces.zipWithNext().all { (a, b) -> b.bpm > a.bpm && b.fallMs < a.fallMs })
    }

    @Test
    fun `la duracion de la ronda es la misma en todos los niveles`() {
        (1..5).forEach { level ->
            val p = RhythmTuning.pace(level)
            val end = RhythmPattern.generate(Random(1), p).last().timeMs + p.noteSpacingMs
            assertTrue("nivel $level: $end", kotlin.math.abs(end - RhythmTuning.ROUND_MS) <= p.noteSpacingMs / 2)
        }
    }

    @Test
    fun `el nivel se acota a 1 a 5`() {
        assertEquals(RhythmTuning.pace(1), RhythmTuning.pace(-4))
        assertEquals(RhythmTuning.pace(5), RhythmTuning.pace(99))
    }

    @Test
    fun `el patron no repite un carril mas de lo permitido`() {
        repeat(200) { seed ->
            val lanes = RhythmPattern.generate(Random(seed), pace).map { it.lane }
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
        assertNotEquals(RhythmPattern.generate(Random(1), pace), RhythmPattern.generate(Random(2), pace))
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
