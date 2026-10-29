package app.luxion.apexapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.luxion.apexapp.alarm.AndroidAwakeVerificationGateway
import app.luxion.apexapp.alarm.AwakeVerifier
import app.luxion.apexapp.sharing.ImportIntents
import app.luxion.apexapp.sharing.PendingImport
import app.luxion.apexapp.ui.MainViewModel
import app.luxion.apexapp.ui.navigation.AppNavigation
import app.luxion.apexapp.ui.theme.ApexUserTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// AppCompatActivity: necesario para el idioma por app (AppCompatDelegate) por debajo de Android 13.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var pendingImport: PendingImport
    @Inject lateinit var awakeVerifier: AwakeVerifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            offerImport(intent)
            confirmAwake(intent)
        }
        setContent {
            val importRequested by viewModel.importRequested.collectAsStateWithLifecycle()
            ApexUserTheme(viewModel) {
                AppNavigation(importRequested)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        offerImport(intent)
        confirmAwake(intent)
    }

    /** Tocar la notificación de verificación cuenta como pulsar «Estoy despierto». */
    private fun confirmAwake(intent: Intent?) {
        val alarmId = intent?.getLongExtra(AndroidAwakeVerificationGateway.EXTRA_CONFIRM_ALARM_ID, -1L) ?: -1L
        if (alarmId < 0) return
        intent?.removeExtra(AndroidAwakeVerificationGateway.EXTRA_CONFIRM_ALARM_ID)
        lifecycleScope.launch(Dispatchers.IO) { awakeVerifier.onConfirm(alarmId) }
    }

    private fun offerImport(intent: Intent?) {
        ImportIntents.inputOf(intent, contentResolver)?.let(pendingImport::offer)
    }
}
