package app.luxion.apexapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.ui.MainViewModel
import app.luxion.apexapp.ui.navigation.AppNavigation
import app.luxion.apexapp.ui.theme.ApexAppTheme
import dagger.hilt.android.AndroidEntryPoint

// AppCompatActivity: necesario para el idioma por app (AppCompatDelegate) por debajo de Android 13.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            ApexAppTheme(darkTheme = themeMode.isDark(isSystemInDarkTheme())) {
                AppNavigation()
            }
        }
    }
}
