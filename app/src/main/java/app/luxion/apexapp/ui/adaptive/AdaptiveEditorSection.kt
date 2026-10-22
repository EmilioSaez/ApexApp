package app.luxion.apexapp.ui.adaptive

import androidx.compose.runtime.Composable
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.ui.editor.EditorSection
import javax.inject.Inject

/**
 * Sección "Dificultad adaptativa" del editor, justo después de Retos (100). Guarda su estado por su
 * cuenta, por identificador de alarma, así que no modifica el borrador.
 */
class AdaptiveEditorSection @Inject constructor() : EditorSection {
    override val order: Int = ORDER

    @Composable
    override fun Content(draft: Alarm, onChange: (Alarm) -> Unit) {
        AdaptiveDifficultySection()
    }

    private companion object {
        const val ORDER = 150
    }
}
