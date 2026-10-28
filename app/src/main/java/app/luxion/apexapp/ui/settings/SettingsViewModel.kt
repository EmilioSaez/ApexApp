package app.luxion.apexapp.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.luxion.apexapp.domain.CustomThemeColors
import app.luxion.apexapp.domain.SettingsRepository
import app.luxion.apexapp.domain.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppLanguage(val tag: String) {
    SYSTEM(""), SPANISH("es"), ENGLISH("en");

    companion object {
        fun fromTags(tags: String): AppLanguage =
            entries.firstOrNull { it != SYSTEM && tags.startsWith(it.tag) } ?: SYSTEM
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settings.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val customColors: StateFlow<CustomThemeColors> = settings.observeCustomThemeColors()
        .stateIn(viewModelScope, SharingStarted.Eagerly, CustomThemeColors.Default)

    private val _language =MutableStateFlow(
        AppLanguage.fromTags(AppCompatDelegate.getApplicationLocales().toLanguageTags()),
    )
    val language: StateFlow<AppLanguage> = _language

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setCustomColors(colors: CustomThemeColors) {
        viewModelScope.launch { settings.setCustomThemeColors(colors) }
    }

    fun setLanguage(language: AppLanguage) {
        _language.value = language
        // El sistema (o AppCompat por debajo de Android 13) guarda la elección y recrea la actividad.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }
}
