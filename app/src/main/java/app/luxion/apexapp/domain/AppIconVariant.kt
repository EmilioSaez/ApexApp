package app.luxion.apexapp.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Variantes de icono del launcher. Android no deja recolorear el icono en runtime, así que cada variante es un
 * `activity-alias` del manifest (`.icon.<name>`) con su propio icono; [accent] es el color del trazo en ese icono.
 * [DEFAULT] es el icono original (rojo) y [NEUTRAL] cubre los colores sin tono (blanco, negro, grises).
 */
enum class AppIconVariant(val accent: Int, val hue: Float?) {
    DEFAULT(0xFFFF4D4F.toInt(), 358f),
    ORANGE(0xFFFF8A3D.toInt(), 25f),
    AMBER(0xFFFFC233.toInt(), 43f),
    GREEN(0xFF4CD07D.toInt(), 142f),
    TEAL(0xFF2CC3C3.toInt(), 180f),
    BLUE(0xFF4D9BFF.toInt(), 215f),
    PURPLE(0xFFA66BFF.toInt(), 270f),
    PINK(0xFFFF5FA8.toInt(), 335f),
    NEUTRAL(0xFFC9CED6.toInt(), null);

    companion object {
        private const val MIN_SATURATION = 0.18f
        private const val MIN_VALUE = 0.2f

        /** Variante más cercana al color [argb]: por tono, o [NEUTRAL] si apenas tiene color. */
        fun nearestTo(argb: Int): AppIconVariant {
            val r = (argb shr 16 and 0xFF) / 255f
            val g = (argb shr 8 and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            val top = max(r, max(g, b))
            val delta = top - min(r, min(g, b))
            if (top < MIN_VALUE || delta / top < MIN_SATURATION) return NEUTRAL
            val hue = when (top) {
                r -> 60f * (((g - b) / delta) % 6f)
                g -> 60f * ((b - r) / delta + 2f)
                else -> 60f * ((r - g) / delta + 4f)
            }.let { if (it < 0f) it + 360f else it }
            return entries.filter { it.hue != null }.minBy { hueDistance(hue, it.hue!!) }
        }

        private fun hueDistance(a: Float, b: Float): Float = abs(a - b).let { min(it, 360f - it) }
    }
}
