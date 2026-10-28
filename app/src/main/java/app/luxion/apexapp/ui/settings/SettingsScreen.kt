package app.luxion.apexapp.ui.settings

import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.AppIconVariant
import app.luxion.apexapp.domain.CustomThemeColors
import app.luxion.apexapp.domain.ThemeMode
import app.luxion.apexapp.ui.components.SectionHeader
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val customColors by viewModel.customColors.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val appIcon by viewModel.appIcon.collectAsStateWithLifecycle()
    SettingsContent(
        themeMode = themeMode,
        customColors = customColors,
        appIcon = appIcon,
        language = language,
        onThemeMode = viewModel::setThemeMode,
        onCustomColors = viewModel::setCustomColors,
        onAppIcon = viewModel::setAppIcon,
        onLanguage = viewModel::setLanguage,
        onBack = onBack,
    )
}

/** Otros cambios añaden sus secciones dentro de la columna con [SectionHeader] + contenido. */
@Composable
fun SettingsContent(
    themeMode: ThemeMode,
    customColors: CustomThemeColors,
    appIcon: AppIconVariant,
    language: AppLanguage,
    onThemeMode: (ThemeMode) -> Unit,
    onCustomColors: (CustomThemeColors) -> Unit,
    onAppIcon: (AppIconVariant) -> Unit,
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
                    ThemeMode.CUSTOM to stringResource(R.string.settings_theme_custom),
                ),
                selected = themeMode,
                onSelect = onThemeMode,
            )
            if (themeMode == ThemeMode.CUSTOM) {
                CustomColorRows(customColors, onCustomColors)
            }
            if (themeMode == ThemeMode.CUSTOM || appIcon != AppIconVariant.DEFAULT) {
                AppIconRows(
                    current = appIcon,
                    suggested = if (themeMode == ThemeMode.CUSTOM) AppIconVariant.nearestTo(customColors.primary) else null,
                    onApply = onAppIcon,
                )
            }
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

private enum class CustomColorSlot(val label: Int) {
    PRIMARY(R.string.settings_custom_primary), SECONDARY(R.string.settings_custom_secondary);

    fun of(colors: CustomThemeColors): Int = if (this == PRIMARY) colors.primary else colors.secondary

    fun with(colors: CustomThemeColors, argb: Int): CustomThemeColors =
        if (this == PRIMARY) colors.copy(primary = argb) else colors.copy(secondary = argb)
}

/** Una fila por color con su muestra; tocarla abre el selector de ese color. */
@Composable
private fun CustomColorRows(colors: CustomThemeColors, onChange: (CustomThemeColors) -> Unit) {
    var editing by remember { mutableStateOf<CustomColorSlot?>(null) }
    Column(Modifier.fillMaxWidth().padding(horizontal = ApexTheme.spacing.screen)) {
        CustomColorSlot.entries.forEach { slot ->
            Row(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
                    .clickable(role = Role.Button) { editing = slot },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(slot.label), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(Color(slot.of(colors)))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
    editing?.let { slot ->
        ColorPickerDialog(
            title = stringResource(slot.label),
            initial = slot.of(colors),
            onDismiss = { editing = null },
            onApply = { argb ->
                onChange(slot.with(colors, argb))
                editing = null
            },
        )
    }
}

private fun AppIconVariant.foreground(): Int = when (this) {
    AppIconVariant.DEFAULT -> R.drawable.ic_launcher_foreground
    AppIconVariant.ORANGE -> R.drawable.ic_launcher_foreground_orange
    AppIconVariant.AMBER -> R.drawable.ic_launcher_foreground_amber
    AppIconVariant.GREEN -> R.drawable.ic_launcher_foreground_green
    AppIconVariant.TEAL -> R.drawable.ic_launcher_foreground_teal
    AppIconVariant.BLUE -> R.drawable.ic_launcher_foreground_blue
    AppIconVariant.PURPLE -> R.drawable.ic_launcher_foreground_purple
    AppIconVariant.PINK -> R.drawable.ic_launcher_foreground_pink
    AppIconVariant.NEUTRAL -> R.drawable.ic_launcher_foreground_neutral
}

/**
 * Icono del launcher: con Personalizado ofrece la variante más cercana al color primario y, si ya hay otra
 * aplicada, permite volver al icono original. Aplicar es explícito: cambiar de alias puede cerrar la app.
 */
@Composable
private fun AppIconRows(current: AppIconVariant, suggested: AppIconVariant?, onApply: (AppIconVariant) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = ApexTheme.spacing.screen)) {
        if (suggested != null) {
            AppIconRow(
                variant = suggested,
                title = stringResource(R.string.settings_icon_themed),
                supporting = stringResource(
                    if (suggested == current) R.string.settings_icon_applied else R.string.settings_icon_suggested,
                ),
                action = stringResource(R.string.settings_icon_apply),
                actionEnabled = suggested != current,
                onAction = { onApply(suggested) },
            )
        }
        if (current != AppIconVariant.DEFAULT && suggested != AppIconVariant.DEFAULT) {
            AppIconRow(
                variant = AppIconVariant.DEFAULT,
                title = stringResource(R.string.settings_icon_original),
                supporting = null,
                action = stringResource(R.string.settings_icon_restore),
                actionEnabled = true,
                onAction = { onApply(AppIconVariant.DEFAULT) },
            )
        }
        if (suggested != null) {
            Text(
                stringResource(R.string.settings_icon_note),
                modifier = Modifier.padding(vertical = ApexTheme.spacing.sm),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppIconRow(
    variant: AppIconVariant,
    title: String,
    supporting: String?,
    action: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md),
    ) {
        Image(
            painter = painterResource(variant.foreground()),
            contentDescription = null,
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF0B0B0B)),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TextButton(onClick = onAction, enabled = actionEnabled) { Text(action) }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** Selector HSV con tres deslizadores; el contraste lo garantiza el tema al generar el esquema, no el selector. */
@Composable
private fun ColorPickerDialog(title: String, initial: Int, onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    val start = remember { FloatArray(3).also { AndroidColor.colorToHSV(initial, it) } }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var saturation by remember { mutableFloatStateOf(start[1]) }
    var brightness by remember { mutableFloatStateOf(start[2]) }
    val current = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
                Box(Modifier.fillMaxWidth().height(48.dp).clip(MaterialTheme.shapes.small).background(Color(current)))
                ColorSlider(stringResource(R.string.settings_color_hue), hue, 0f..360f) { hue = it }
                ColorSlider(stringResource(R.string.settings_color_saturation), saturation, 0f..1f) { saturation = it }
                ColorSlider(stringResource(R.string.settings_color_brightness), brightness, 0f..1f) { brightness = it }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(current) }) { Text(stringResource(R.string.settings_color_apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_color_cancel)) } },
    )
}

@Composable
private fun ColorSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = value, onValueChange = onChange, valueRange = range)
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
    themeMode = ThemeMode.CUSTOM,
    customColors = CustomThemeColors.Default,
    appIcon = AppIconVariant.DEFAULT,
    language = AppLanguage.SYSTEM,
    onThemeMode = {},
    onCustomColors = {},
    onAppIcon = {},
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
