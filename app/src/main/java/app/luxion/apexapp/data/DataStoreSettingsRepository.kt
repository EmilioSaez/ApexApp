package app.luxion.apexapp.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.luxion.apexapp.domain.AppIconVariant
import app.luxion.apexapp.domain.CustomThemeColors
import app.luxion.apexapp.domain.SettingsRepository
import app.luxion.apexapp.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreSettingsRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : SettingsRepository {

    override fun observeThemeMode(): Flow<ThemeMode> = store.data.map { prefs ->
        prefs[THEME_MODE]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
    }

    override fun observeCustomThemeColors(): Flow<CustomThemeColors> = store.data.map { prefs ->
        CustomThemeColors(
            primary = prefs[CUSTOM_PRIMARY] ?: CustomThemeColors.Default.primary,
            secondary = prefs[CUSTOM_SECONDARY] ?: CustomThemeColors.Default.secondary,
        )
    }

    override suspend fun setCustomThemeColors(colors: CustomThemeColors) {
        store.edit {
            it[CUSTOM_PRIMARY] = colors.primary
            it[CUSTOM_SECONDARY] = colors.secondary
        }
    }

    override fun observeAppIcon(): Flow<AppIconVariant> = store.data.map { prefs ->
        prefs[APP_ICON]?.let { name -> AppIconVariant.entries.firstOrNull { it.name == name } } ?: AppIconVariant.DEFAULT
    }

    override suspend fun setAppIcon(variant: AppIconVariant) {
        store.edit { it[APP_ICON] = variant.name }
    }

    private companion object {
        val APP_ICON = stringPreferencesKey("app_icon")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CUSTOM_PRIMARY = intPreferencesKey("theme_custom_primary")
        val CUSTOM_SECONDARY = intPreferencesKey("theme_custom_secondary")
    }
}
