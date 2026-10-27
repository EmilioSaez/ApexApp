package app.luxion.apexapp.domain.challenge.sensor

import kotlin.random.Random

internal const val MS = 1_000_000L

/** Muestras a 50 Hz con ruido gaussiano aproximado de [noise] m/s² por eje y magnitud base [level]. */
internal fun noisySamples(
    fromMs: Long,
    durationMs: Long,
    level: Float,
    noise: Float = 0.15f,
    random: Random = Random(7),
): List<AccelSample> = (0 until durationMs / 20).map { i ->
    val dir = level + (random.nextFloat() - 0.5f) * noise * 2
    AccelSample((fromMs + i * 20) * MS, dir, (random.nextFloat() - 0.5f) * noise, (random.nextFloat() - 0.5f) * noise)
}

/** Una muestra de pico en [tMs] con amplitud [amp]. */
internal fun spike(tMs: Long, amp: Float) = AccelSample(tMs * MS, amp, 0.1f, 0.1f)
