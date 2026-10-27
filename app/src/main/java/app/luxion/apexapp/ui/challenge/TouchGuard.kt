package app.luxion.apexapp.ui.challenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import app.luxion.apexapp.domain.AntiCheatLevel
import app.luxion.apexapp.domain.challenge.touch.TouchPlausibility

/** Nivel anti-trampas efectivo de la alarma, para los juegos táctiles. */
class TouchGuard(val level: AntiCheatLevel)

/** Sin sesión de alarma (pruebas, previews) el nivel es `OFF`. */
val LocalTouchGuard = staticCompositionLocalOf { TouchGuard(AntiCheatLevel.OFF) }

/** Validador de toques del juego en curso, con el nivel de la alarma. */
@Composable
fun rememberTouchPlausibility(): TouchPlausibility {
    val guard = LocalTouchGuard.current
    return remember(guard.level) { TouchPlausibility(guard.level) }
}
