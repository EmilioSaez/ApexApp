package app.luxion.apexapp.domain.challenge

import kotlin.random.Random
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathChallengeTest {
    private val math = MathChallenge()

    /** Evalúa el enunciado generado para comprobar que la respuesta esperada es la correcta. */
    private fun evaluate(prompt: String): Int {
        val tokens = prompt.replace("(", "").replace(")", "").split(" ")
        // Formatos: "a + b", "a × b", "a × b ± c", "(a + b) × c ± d"
        return if (prompt.startsWith("(")) {
            val a = tokens[0].toInt()
            val b = tokens[2].toInt()
            val c = tokens[4].toInt()
            val d = tokens[6].toInt()
            val product = (a + b) * c
            if (tokens[5] == "-") product - d else product + d
        } else if (tokens[1] == "×") {
            val product = tokens[0].toInt() * tokens[2].toInt()
            when {
                tokens.size == 3 -> product
                tokens[3] == "-" -> product - tokens[4].toInt()
                else -> product + tokens[4].toInt()
            }
        } else if (tokens[1] == "+") {
            tokens[0].toInt() + tokens[2].toInt()
        } else {
            tokens[0].toInt() - tokens[2].toInt()
        }
    }

    @Test
    fun `expected answer is a positive integer for every level`() {
        val random = Random(1)
        for (level in 1..5) repeat(200) {
            val instance = math.generate(level, "{}", random)
            val result = evaluate(instance.prompt)
            assertTrue("${instance.prompt} = $result", result > 0)
            assertTrue(instance.check(ChallengeAnswer.Text(result.toString())))
            assertFalse(instance.check(ChallengeAnswer.Text((result + 1).toString())))
        }
    }

    @Test
    fun `level 1 is addition or subtraction of small numbers`() {
        val random = Random(2)
        repeat(100) {
            val prompt = math.generate(1, "{}", random).prompt
            assertTrue(prompt, " + " in prompt || " - " in prompt)
            assertFalse(prompt, "×" in prompt)
        }
    }

    @Test
    fun `level 5 combines several steps`() {
        val random = Random(3)
        repeat(100) {
            val prompt = math.generate(5, "{}", random).prompt
            assertTrue(prompt, prompt.startsWith("(") && "×" in prompt)
        }
    }

    @Test
    fun `difficulty grows with level`() {
        val random = Random(4)
        fun maxResult(level: Int) = (1..200).maxOf { evaluate(math.generate(level, "{}", random).prompt) }
        assertTrue(maxResult(5) > maxResult(3))
        assertTrue(maxResult(3) > maxResult(1))
    }

    @Test
    fun `empty or non numeric input is rejected`() {
        val instance = math.generate(1, "{}", Random(5))
        assertFalse(instance.check(ChallengeAnswer.Text("")))
        assertFalse(instance.check(ChallengeAnswer.Text("abc")))
        assertFalse(instance.check(ChallengeAnswer.Text("  ")))
    }

    @Test
    fun `answer with surrounding spaces is accepted`() {
        val instance = math.generate(1, "{}", Random(6))
        val result = evaluate(instance.prompt)
        assertTrue(instance.check(ChallengeAnswer.Text("  $result ")))
    }
}
