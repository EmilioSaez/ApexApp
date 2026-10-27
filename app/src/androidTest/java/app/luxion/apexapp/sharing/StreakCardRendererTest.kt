package app.luxion.apexapp.sharing

import android.graphics.Color
import android.graphics.Typeface
import app.luxion.apexapp.domain.Streak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StreakCardRendererTest {
    private val text = StreakCardText(
        title = "Tu racha",
        currentLabel = "Racha actual",
        currentUnit = "días",
        bestLabel = "Mejor racha",
        bestValue = "12 días",
        footer = "Despierto con ApexApp",
    )
    private val colors = StreakCardColors(
        background = Color.rgb(20, 22, 26),
        primaryText = Color.WHITE,
        secondaryText = Color.GRAY,
        accent = Color.rgb(255, 138, 76),
    )

    private fun render(streak: Streak) =
        StreakCardRenderer.render(streak, text, colors, Typeface.DEFAULT, Typeface.DEFAULT)

    @Test
    fun cardHasTheFixedSizeAndBackground() {
        val bitmap = render(Streak(7, 12))
        assertEquals(StreakCardRenderer.WIDTH, bitmap.width)
        assertEquals(StreakCardRenderer.HEIGHT, bitmap.height)
        assertEquals(colors.background, bitmap.getPixel(2, 2))
    }

    @Test
    fun differentStreaksDrawDifferentCards() {
        val a = render(Streak(7, 12))
        val b = render(Streak(8, 12))
        assertNotEquals(
            a.getPixels(), b.getPixels(),
        )
    }

    @Test
    fun threeDigitStreakStillFitsTheCard() {
        val bitmap = render(Streak(365, 365))
        // El margen derecho queda libre de tinta: el número se reduce hasta caber.
        val x = StreakCardRenderer.WIDTH - 20
        assertEquals(colors.background, bitmap.getPixel(x, 600))
    }

    private fun android.graphics.Bitmap.getPixels(): List<Int> {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        return pixels.toList()
    }
}
