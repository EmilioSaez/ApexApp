package app.luxion.apexapp.domain

/**
 * Patrones de vibración: id de preset (`steady`, `pulse`, `sos`) o personalizado `c:200,100,400`
 * (ms, alterna pulso y pausa empezando por pulso).
 */
object VibrationPatterns {
    const val DEFAULT = "steady"
    const val CUSTOM_PREFIX = "c:"
    const val MAX_ELEMENTS = 20
    const val MAX_CYCLE_MS = 10_000L
    private const val MAX_STEP_MS = 5_000L

    /** Duraciones de cada preset: pulso, pausa, pulso, pausa... */
    val presets: Map<String, List<Long>> = linkedMapOf(
        "steady" to listOf(800L, 400L),
        "pulse" to listOf(150L, 150L, 150L, 150L, 150L, 700L),
        "sos" to listOf(150L, 150L, 150L, 150L, 150L, 400L, 450L, 150L, 450L, 150L, 450L, 400L, 150L, 150L, 150L, 150L, 150L, 900L),
    )

    /** Pulsos y pausas del patrón, o null si no es válido. */
    fun steps(pattern: String): List<Long>? {
        presets[pattern]?.let { return it }
        if (!pattern.startsWith(CUSTOM_PREFIX)) return null
        val steps = pattern.removePrefix(CUSTOM_PREFIX).split(',').map { it.trim().toLongOrNull() ?: return null }
        if (steps.isEmpty() || steps.size > MAX_ELEMENTS) return null
        if (steps.any { it <= 0 || it > MAX_STEP_MS }) return null
        if (steps.sum() > MAX_CYCLE_MS) return null
        return steps
    }

    fun isValid(pattern: String): Boolean = steps(pattern) != null

    fun custom(steps: List<Long>): String = CUSTOM_PREFIX + steps.joinToString(",")

    /**
     * Temporización para `VibrationEffect.createWaveform`: empieza con 0 de espera y, si el patrón
     * acaba en pulso, añade una pausa para que el bucle no encadene pulsos.
     */
    fun waveform(pattern: String): LongArray {
        val steps = steps(pattern) ?: presets.getValue(DEFAULT)
        val padded = if (steps.size % 2 == 1) steps + 1_000L else steps
        return (listOf(0L) + padded).toLongArray()
    }
}
