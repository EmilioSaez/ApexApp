package app.luxion.apexapp.ui.anticheat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.ui.editor.EditorSection
import javax.inject.Inject

/** Lo provee la navegación: abre la pantalla de estado de anti-trampas. */
val LocalOpenAntiCheatStatus = staticCompositionLocalOf<() -> Unit> { {} }

/** Sección del editor con el nivel anti-trampas y la política de snooze, tras Retos y Sonido. */
class AntiCheatEditorSection @Inject constructor() : EditorSection {
    override val order: Int = ORDER

    @Composable
    override fun Content(draft: Alarm, onChange: (Alarm) -> Unit) {
        AntiCheatSection(
            policy = draft.antiCheat,
            onChange = { onChange(draft.copy(antiCheat = it)) },
            onOpenStatus = LocalOpenAntiCheatStatus.current,
        )
    }

    private companion object {
        const val ORDER = 300
    }
}
