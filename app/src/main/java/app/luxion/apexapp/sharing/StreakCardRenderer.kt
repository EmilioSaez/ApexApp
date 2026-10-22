package app.luxion.apexapp.sharing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import app.luxion.apexapp.domain.Streak
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Textos ya localizados que aparecen en la tarjeta; la imagen no incluye nada más (ni etiquetas ni horas). */
data class StreakCardText(
    val title: String,
    val currentLabel: String,
    val currentUnit: String,
    val bestLabel: String,
    val bestValue: String,
    val footer: String,
)

/** Colores ARGB tomados del tema de la app. */
data class StreakCardColors(val background: Int, val primaryText: Int, val secondaryText: Int, val accent: Int)

/** Origen de los datos de la tarjeta; envuelve el historial de despertar para poder sustituirlo en tests. */
fun interface StreakStatsSource {
    fun observeStreak(): Flow<Streak>
}

class WakeStreakStatsSource @Inject constructor(
    private val history: app.luxion.apexapp.domain.WakeHistoryRepository,
) : StreakStatsSource {
    override fun observeStreak(): Flow<Streak> = history.observeStreak()
}

/** Dibuja la tarjeta de racha con un tamaño fijo, independiente de la pantalla. */
object StreakCardRenderer {
    const val WIDTH = 1080
    const val HEIGHT = 1350

    private const val MARGIN = 96f

    fun render(
        streak: Streak,
        text: StreakCardText,
        colors: StreakCardColors,
        display: Typeface,
        body: Typeface,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(colors.background)

        fun paint(color: Int, size: Float, face: Typeface) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = face
        }

        canvas.drawText(text.title, MARGIN, 170f, paint(colors.secondaryText, 52f, body))

        canvas.drawText(text.currentLabel, MARGIN, 420f, paint(colors.secondaryText, 56f, body))
        val number = paint(colors.accent, 420f, display).also { fit(it, streak.current.toString(), WIDTH - 2 * MARGIN) }
        canvas.drawText(streak.current.toString(), MARGIN - 12f, 800f, number)
        canvas.drawText(text.currentUnit, MARGIN, 900f, paint(colors.primaryText, 72f, display))

        canvas.drawRect(MARGIN, 1010f, WIDTH - MARGIN, 1014f, paint(colors.secondaryText, 1f, body))
        canvas.drawText(text.bestLabel, MARGIN, 1100f, paint(colors.secondaryText, 52f, body))
        canvas.drawText(text.bestValue, MARGIN, 1190f, paint(colors.primaryText, 84f, display))

        canvas.drawText(text.footer, MARGIN, HEIGHT - 70f, paint(colors.secondaryText, 40f, body))
        return bitmap
    }

    /** Reduce el tamaño de [paint] hasta que [value] quepa en [maxWidth]. */
    private fun fit(paint: Paint, value: String, maxWidth: Float) {
        while (paint.measureText(value) > maxWidth && paint.textSize > 80f) paint.textSize -= 10f
    }
}
