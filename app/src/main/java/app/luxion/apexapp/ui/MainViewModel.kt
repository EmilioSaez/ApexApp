package app.luxion.apexapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.luxion.apexapp.domain.CustomThemeColors
import app.luxion.apexapp.domain.SettingsRepository
import app.luxion.apexapp.domain.ThemeMode
import app.luxion.apexapp.sharing.PendingImport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settings: SettingsRepository,
    pendingImport: PendingImport,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = settings.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val customColors: StateFlow<CustomThemeColors> = settings.observeCustomThemeColors()
        .stateIn(viewModelScope, SharingStarted.Eagerly, CustomThemeColors.Default)

    /** Verdadero mientras hay un enlace o archivo de ApexApp recibido que aún no ha visto la pantalla de importación. */
    val importRequested: StateFlow<Boolean> = pendingImport.content.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
}
