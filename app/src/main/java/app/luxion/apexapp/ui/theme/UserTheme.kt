package app.luxion.apexapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.domain.ThemeMode
import app.luxion.apexapp.ui.MainViewModel

/** [ApexAppTheme] con las preferencias de tema del usuario (modo, base y colores del tema Personalizado). */
@Composable
fun ApexUserTheme(viewModel: MainViewModel, content: @Composable () -> Unit) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val customColors by viewModel.customColors.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = if (themeMode == ThemeMode.CUSTOM) customColors.base.isDark(systemDark) else themeMode.isDark(systemDark)
    SystemBarsEffect(darkTheme)
    ApexAppTheme(
        darkTheme = darkTheme,
        customColors = customColors.takeIf { themeMode == ThemeMode.CUSTOM },
        content = content,
    )
}
