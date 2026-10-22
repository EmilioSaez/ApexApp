package app.luxion.apexapp.domain.challenge

import javax.inject.Inject
import kotlin.random.Random

/** Reto de matemáticas: operaciones con resultado entero positivo, más complejas cuanto mayor el nivel. */
class MathChallenge @Inject constructor() : Challenge {
    override val type: ChallengeType = ChallengeType.MATH

    override fun generate(level: Int, params: String, random: Random): ChallengeInstance {
        val lvl = level.coerceIn(SessionConfig.MIN_DIFFICULTY, SessionConfig.MAX_DIFFICULTY)
        val (prompt, result) = when (lvl) {
            1 -> addOrSubtract(random, 1..20)
            2 -> addOrSubtract(random, 10..99)
            3 -> multiply(random)
            4 -> multiplyAndAdjust(random)
            else -> parenthesized(random)
        }
        return MathInstance(prompt, result, lvl)
    }

    private fun addOrSubtract(random: Random, range: IntRange): Pair<String, Int> {
        val a = random.nextInt(range.first, range.last + 1)
        val b = random.nextInt(range.first, range.last + 1)
        return if (random.nextBoolean()) {
            "$a + $b" to a + b
        } else {
            // El minuendo es estrictamente mayor para que el resultado sea positivo.
            val big = if (a == b) a + 1 else maxOf(a, b)
            val small = minOf(a, b)
            "$big - $small" to big - small
        }
    }

    private fun multiply(random: Random): Pair<String, Int> {
        val a = random.nextInt(2, 13)
        val b = random.nextInt(2, 13)
        return "$a × $b" to a * b
    }

    private fun multiplyAndAdjust(random: Random): Pair<String, Int> {
        val a = random.nextInt(6, 16)
        val b = random.nextInt(6, 16)
        val product = a * b
        return if (random.nextBoolean()) {
            val c = random.nextInt(1, 51)
            "$a × $b + $c" to product + c
        } else {
            // El sustraendo es menor que el producto para que el resultado sea positivo.
            val c = random.nextInt(1, minOf(50, product - 1) + 1)
            "$a × $b - $c" to product - c
        }
    }

    private fun parenthesized(random: Random): Pair<String, Int> {
        val a = random.nextInt(10, 41)
        val b = random.nextInt(10, 41)
        val c = random.nextInt(3, 10)
        val product = (a + b) * c
        return if (random.nextBoolean()) {
            val d = random.nextInt(1, 100)
            "($a + $b) × $c + $d" to product + d
        } else {
            // El sustraendo es menor que el producto para que el resultado sea positivo.
            val d = random.nextInt(1, minOf(99, product - 1) + 1)
            "($a + $b) × $c - $d" to product - d
        }
    }

    private class MathInstance(
        override val prompt: String,
        private val expected: Int,
        override val level: Int,
    ) : ChallengeInstance {
        override val type: ChallengeType = ChallengeType.MATH

        override fun check(answer: ChallengeAnswer): Boolean = when (answer) {
            is ChallengeAnswer.Text -> answer.value.trim().toIntOrNull() == expected
        }
    }
}
