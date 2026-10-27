package app.luxion.apexapp.ui.editor.sound

import androidx.compose.runtime.Composable
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.ui.editor.EditorSection
import javax.inject.Inject

/** Sección "Sonido" del editor; va después de la de retos (order 100). */
class SoundSection @Inject constructor() : EditorSection {
    override val order = 200

    @Composable
    override fun Content(draft: Alarm, onChange: (Alarm) -> Unit) = SoundSettingsSection(draft, onChange)
}
