package app.luxion.apexapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.luxion.apexapp.domain.SettingsRepository
import app.luxion.apexapp.domain.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = settings.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
}
