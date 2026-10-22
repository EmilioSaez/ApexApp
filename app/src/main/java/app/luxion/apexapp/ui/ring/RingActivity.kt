package app.luxion.apexapp.ui.ring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import app.luxion.apexapp.ui.theme.ApexAppTheme
import dagger.hilt.android.AndroidEntryPoint

/** Pantalla de sonar: se muestra sobre el bloqueo cuando se dispara una alarma. */
@AndroidEntryPoint
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        enableEdgeToEdge()
        setContent {
            ApexAppTheme(darkTheme = isSystemInDarkTheme()) {
                RingScreen(onFinished = ::finish)
            }
        }
    }
}
