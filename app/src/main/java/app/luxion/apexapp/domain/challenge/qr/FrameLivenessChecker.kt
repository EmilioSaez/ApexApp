package app.luxion.apexapp.domain.challenge.qr

import app.luxion.apexapp.domain.challenge.sensor.SensorPlausibility
import kotlin.math.sqrt

/**
 * Datos de un fotograma ya reducidos por el lector. [value] es el código leído (null si no hay),
 * [centerX]/[centerY] el centro de su cuadro delimitador en píxeles y [backgroundLuma] la luminancia media
 * (0..255) de la región que rodea al código.
 */
data class FrameObservation(
    val tNanos: Long,
    val value: String?,
    val centerX: Float = 0f,
    val centerY: Float = 0f,
    val backgroundLuma: Float = 0f,
)

sealed interface LivenessVerdict {
    /** Se sigue reuniendo evidencia; [progress] va de 0 a 1. */
    data class Collecting(val progress: Float) : LivenessVerdict

    data class Accepted(val value: String) : LivenessVerdict

    data class Rejected(val reason: Reason) : LivenessVerdict

    enum class Reason { STATIC_IMAGE, SCREEN }
}

/**
 * Acepta un código solo si se lee de forma continua durante [minDurationNanos] y al menos [minFrames]
 * fotogramas con variación natural de la escena. Rechaza imágenes estáticas (ni el fondo ni el código se
 * mueven) y pantallas (parpadeo periódico de la luminancia). Si el código sale del encuadre o cambia,
 * la verificación se reinicia.
 */
class FrameLivenessChecker(
    private val minDurationNanos: Long = 1_500_000_000L,
    private val minFrames: Int = 15,
    private val minBackgroundStd: Float = 0.2f,
    private val minJitterStd: Float = 0.5f,
    private val flickerAmplitude: Float = 2f,
    private val flickerMaxVariation: Double = 0.1,
) {
    private val frames = ArrayList<FrameObservation>()

    fun onFrame(frame: FrameObservation): LivenessVerdict {
        val value = frame.value
        if (value == null || (frames.isNotEmpty() && frames.first().value != value)) frames.clear()
        if (value == null) return LivenessVerdict.Collecting(0f)
        frames += frame

        val elapsed = frame.tNanos - frames.first().tNanos
        if (elapsed < minDurationNanos || frames.size < minFrames) {
            return LivenessVerdict.Collecting(minOf(elapsed.toFloat() / minDurationNanos, 0.99f))
        }
        val verdict = judge(value)
        if (verdict is LivenessVerdict.Rejected) frames.clear()
        return verdict
    }

    fun reset() = frames.clear()

    private fun judge(value: String): LivenessVerdict {
        val luma = frames.map { it.backgroundLuma.toDouble() }
        val backgroundStd = std(luma)
        val jitterStd = maxOf(std(frames.map { it.centerX.toDouble() }), std(frames.map { it.centerY.toDouble() }))
        if (backgroundStd < minBackgroundStd && jitterStd < minJitterStd) {
            return LivenessVerdict.Rejected(LivenessVerdict.Reason.STATIC_IMAGE)
        }
        if (isFlickering(luma)) return LivenessVerdict.Rejected(LivenessVerdict.Reason.SCREEN)
        return LivenessVerdict.Accepted(value)
    }

    /** Parpadeo de pantalla: oscilación de luminancia de amplitud apreciable y periodo muy regular. */
    private fun isFlickering(luma: List<Double>): Boolean {
        val mean = luma.average()
        val centered = luma.map { it - mean }
        if (centered.maxOf { it } - centered.minOf { it } < flickerAmplitude) return false
        val crossings = ArrayList<Long>()
        for (i in 1 until centered.size) {
            if (centered[i - 1] < 0 && centered[i] >= 0) crossings += frames[i].tNanos
        }
        if (crossings.size < SensorPlausibility.MIN_PEAKS_FOR_PERIODICITY) return false
        val intervals = crossings.zipWithNext { a, b -> (b - a).toDouble() }
        return SensorPlausibility.variation(intervals) < flickerMaxVariation
    }

    private fun std(values: List<Double>): Float {
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size).toFloat()
    }
}
