package app.luxion.apexapp.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.luxion.apexapp.domain.CustomThemeColors
import app.luxion.apexapp.domain.ThemeMode
import app.luxion.apexapp.ui.theme.ApexAppTheme
import androidx.test.platform.app.InstrumentationRegistry
import app.luxion.apexapp.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun str(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test
    fun choosingOptions_notifiesSelection() {
        var theme by mutableStateOf(ThemeMode.SYSTEM)
        var language by mutableStateOf(AppLanguage.SYSTEM)
        compose.setContent {
            ApexAppTheme {
                SettingsContent(
                    themeMode = theme,
                    customColors = CustomThemeColors.Default,
                    language = language,
                    onThemeMode = { theme = it },
                    onCustomColors = {},
                    onLanguage = { language = it },
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText(str(R.string.settings_theme_dark)).performClick()
        compose.onNodeWithText("English").performClick()

        assertEquals(ThemeMode.DARK, theme)
        assertEquals(AppLanguage.ENGLISH, language)
    }
}
