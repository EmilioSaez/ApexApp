package app.luxion.apexapp.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.luxion.apexapp.ui.components.ScreenTopBar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.ThemeMode
import app.luxion.apexapp.ui.components.SectionHeader
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    SettingsContent(
        themeMode = themeMode,
        language = language,
        onThemeMode = viewModel::setThemeMode,
        onLanguage = viewModel::setLanguage,
        onBack = onBack,
    )
}

/** Otros cambios añaden sus secciones dentro de la columna con [SectionHeader] + contenido. */
@Composable
fun SettingsContent(
    themeMode: ThemeMode,
    language: AppLanguage,
    onThemeMode: (ThemeMode) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ScreenTopBar(stringResource(R.string.settings_title), onBack)
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            SectionHeader(stringResource(R.string.settings_section_appearance))
            Choice(
                options = listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                    ThemeMode.DARK to stringResource(R.string.settings_theme_dark),
                ),
                selected = themeMode,
                onSelect = onThemeMode,
            )
            Spacer(Modifier.height(ApexTheme.spacing.lg))
            SectionHeader(stringResource(R.string.settings_section_language))
            Choice(
                options = listOf(
                    AppLanguage.SYSTEM to stringResource(R.string.settings_language_system),
                    AppLanguage.SPANISH to stringResource(R.string.settings_language_es),
                    AppLanguage.ENGLISH to stringResource(R.string.settings_language_en),
                ),
                selected = language,
                onSelect = onLanguage,
            )
        }
    }
}

/** Opciones como filas con radio, separadas por una linea fina: lista, no botones segmentados. */
@Composable
private fun <T> Choice(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = ApexTheme.spacing.screen)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        options.forEach { (value, label) ->
            Row(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
                    .selectable(selected = value == selected, role = Role.RadioButton, onClick = { onSelect(value) }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                RadioButton(selected = value == selected, onClick = null)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun SettingsPreview() = SettingsContent(
    themeMode = ThemeMode.DARK,
    language = AppLanguage.SYSTEM,
    onThemeMode = {},
    onLanguage = {},
    onBack = {},
)

@Preview(name = "Claro", showBackground = true)
@Composable
private fun SettingsLight() = ApexAppTheme(darkTheme = false) { SettingsPreview() }

@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsDark() = ApexAppTheme(darkTheme = true) { SettingsPreview() }

@Preview(name = "Fuente 1.3", showBackground = true, fontScale = 1.3f)
@Composable
private fun SettingsLargeFont() = ApexAppTheme(darkTheme = false) { SettingsPreview() }
