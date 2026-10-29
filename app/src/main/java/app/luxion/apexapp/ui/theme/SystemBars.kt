package app.luxion.apexapp.ui.theme

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

// Mismos scrims que usa enableEdgeToEdge() por defecto para la navegación de 3 botones.
private const val NAV_LIGHT_SCRIM = 0xe6FFFFFF.toInt()
private const val NAV_DARK_SCRIM = 0x801b1b1b.toInt()

/**
 * Edge-to-edge con los iconos de las barras del sistema acordes al tema de la app, no al del sistema:
 * con tema oscuro sobre sistema claro (o al revés) los iconos de atrás/inicio/recientes se perderían.
 */
@Composable
fun SystemBarsEffect(darkTheme: Boolean) {
    val activity = LocalContext.current as? ComponentActivity ?: return
    DisposableEffect(activity, darkTheme) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(NAV_LIGHT_SCRIM, NAV_DARK_SCRIM) { darkTheme },
        )
        onDispose { }
    }
}
