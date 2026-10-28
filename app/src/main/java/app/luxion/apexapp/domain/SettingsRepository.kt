package app.luxion.apexapp.domain

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    fun observeCustomThemeColors(): Flow<CustomThemeColors>
    suspend fun setCustomThemeColors(colors: CustomThemeColors)
}
