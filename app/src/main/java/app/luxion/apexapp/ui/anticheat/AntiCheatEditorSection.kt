package app.luxion.apexapp.ui.anticheat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import app.luxion.apexapp.anticheat.CapabilityChecker
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.ui.editor.EditorSection
import javax.inject.Inject

/** Lo provee la navegación: abre la pantalla de estado de anti-trampas. */
val LocalOpenAntiCheatStatus = staticCompositionLocalOf<() -> Unit> { {} }

/** Sección del editor con el nivel anti-trampas y la política de snooze, tras Retos y Sonido. */
class AntiCheatEditorSection @Inject constructor(
    private val capabilities: CapabilityChecker,
) : EditorSection {
    override val order: Int = ORDER

    @Composable
    override fun Content(draft: Alarm, onChange: (Alarm) -> Unit) {
        AntiCheatSection(
            policy = draft.antiCheat,
            onChange = { onChange(draft.copy(antiCheat = it)) },
            onOpenStatus = LocalOpenAntiCheatStatus.current,
            // Se lee en cada composición: al volver de Ajustes tras conceder el permiso el aviso desaparece.
            capabilities = capabilities.current(),
        )
    }

    private companion object {
        const val ORDER = 300
    }
}
