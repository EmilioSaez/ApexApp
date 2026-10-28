package app.luxion.apexapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.domain.ThemeMode
import app.luxion.apexapp.sharing.ImportIntents
import app.luxion.apexapp.sharing.PendingImport
import app.luxion.apexapp.ui.MainViewModel
import app.luxion.apexapp.ui.navigation.AppNavigation
import app.luxion.apexapp.ui.theme.ApexAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// AppCompatActivity: necesario para el idioma por app (AppCompatDelegate) por debajo de Android 13.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var pendingImport: PendingImport

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) offerImport(intent)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val customColors by viewModel.customColors.collectAsStateWithLifecycle()
            val importRequested by viewModel.importRequested.collectAsStateWithLifecycle()
            ApexAppTheme(
                darkTheme = themeMode.isDark(isSystemInDarkTheme()),
                customColors = customColors.takeIf { themeMode == ThemeMode.CUSTOM },
            ) {
                AppNavigation(importRequested)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        offerImport(intent)
    }

    private fun offerImport(intent: Intent?) {
        ImportIntents.inputOf(intent, contentResolver)?.let(pendingImport::offer)
    }
}
