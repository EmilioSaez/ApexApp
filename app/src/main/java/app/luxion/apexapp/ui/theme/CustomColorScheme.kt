package app.luxion.apexapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val MinTextContrast = 4.5f

/**
 * Esquema M3 a partir de dos colores base. Las superficies y los colores semánticos (éxito, aviso) son los
 * de la paleta de la app; solo cambian primary y secondary, ajustando la luminosidad hasta cumplir contraste AA.
 */
internal fun customColorScheme(primary: Color, secondary: Color, dark: Boolean): ColorScheme {
    val base = if (dark) DarkColorScheme else LightColorScheme
    val primaryColor = accent(primary, base.surface, dark)
    val secondaryColor = accent(secondary, base.surface, dark)
    return base.copy(
        primary = primaryColor,
        onPrimary = onColor(primaryColor),
        primaryContainer = container(primary, dark),
        onPrimaryContainer = onContainer(primary, dark),
        secondary = secondaryColor,
        onSecondary = onColor(secondaryColor),
        secondaryContainer = container(secondary, dark),
        onSecondaryContainer = onContainer(secondary, dark),
        inversePrimary = accent(primary, base.inverseSurface, !dark),
    )
}

/** Mismo tono y saturación que [seed], con la luminosidad movida lo justo para leerse sobre [background]. */
private fun accent(seed: Color, background: Color, dark: Boolean): Color {
    val (h, s, l) = seed.toHsl()
    val step = if (dark) 0.02f else -0.02f
    var lightness = l
    var color = hsl(h, s, lightness)
    repeat(50) {
        if (contrast(color, background) >= MinTextContrast) return color
        lightness = (lightness + step).coerceIn(0f, 1f)
        color = hsl(h, s, lightness)
    }
    return color
}

private fun container(seed: Color, dark: Boolean): Color {
    val (h, s, _) = seed.toHsl()
    return hsl(h, min(s, 0.6f), if (dark) 0.28f else 0.88f)
}

private fun onContainer(seed: Color, dark: Boolean): Color {
    val (h, s, _) = seed.toHsl()
    return hsl(h, min(s, 0.6f), if (dark) 0.90f else 0.12f)
}

private fun onColor(background: Color): Color =
    listOf(Color.White, Color.Black).maxBy { contrast(it, background) }

internal fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}

private fun Color.toHsl(): Triple<Float, Float, Float> {
    val max = max(red, max(green, blue))
    val min = min(red, min(green, blue))
    val l = (max + min) / 2f
    val d = max - min
    if (d == 0f) return Triple(0f, 0f, l)
    val s = d / (1f - abs(2f * l - 1f))
    val h = when (max) {
        red -> ((green - blue) / d).mod(6f)
        green -> (blue - red) / d + 2f
        else -> (red - green) / d + 4f
    } * 60f
    return Triple(h, s, l)
}

private fun hsl(h: Float, s: Float, l: Float): Color {
    val lightness = l.coerceIn(0f, 1f)
    val c = (1f - abs(2f * lightness - 1f)) * s.coerceIn(0f, 1f)
    val x = c * (1f - abs((h / 60f).mod(2f) - 1f))
    val m = lightness - c / 2f
    val (r, g, b) = when ((h / 60f).toInt().mod(6)) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}
