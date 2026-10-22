package app.luxion.apexapp.ui.editor

import androidx.compose.runtime.Composable
import app.luxion.apexapp.domain.Alarm

/**
 * Sección del editor de alarmas aportada por un módulo (retos, sonido, anti-trampa...). Se registra con
 * `@Binds @IntoSet EditorSection` y el editor las muestra en `extraContent` ordenadas por [order].
 */
interface EditorSection {
    /** Menor primero; a igual valor el editor desempata por nombre de clase para un orden estable. */
    val order: Int

    /** [draft] es la alarma en edición; [onChange] la sustituye por una versión modificada. */
    @Composable
    fun Content(draft: Alarm, onChange: (Alarm) -> Unit)

    /** Si es falso, el editor no permite guardar. */
    fun isValid(draft: Alarm): Boolean = true
}

fun Collection<EditorSection>.sortedForEditor(): List<EditorSection> =
    sortedWith(compareBy<EditorSection> { it.order }.thenBy { it::class.java.name })
