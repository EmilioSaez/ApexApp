package app.luxion.apexapp.ui.stats

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.BedtimeSource
import app.luxion.apexapp.ui.components.EmptyState
import app.luxion.apexapp.ui.components.ScreenTopBar
import app.luxion.apexapp.domain.Streak
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    onShareStreak: () -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsContent(
        state = state,
        onBack = onBack,
        onShareStreak = onShareStreak,
        onSelectRange = viewModel::selectRange,
        onSetLastNightBedtime = viewModel::setLastNightBedtime,
        onSetUsualBedtime = viewModel::setUsualBedtime,
    )
}

private enum class BedtimeDialog { LAST_NIGHT, USUAL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsContent(
    state: StatsUiState,
    onBack: () -> Unit,
    onSelectRange: (StatsRange) -> Unit,
    onSetLastNightBedtime: (LocalTime) -> Unit,
    onSetUsualBedtime: (LocalTime?) -> Unit,
    onShareStreak: () -> Unit = {},
) {
    var dialog by rememberSaveable { mutableStateOf<BedtimeDialog?>(null) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ScreenTopBar(stringResource(R.string.stats_title), onBack)
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ApexTheme.spacing.screen, vertical = ApexTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xl),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
                StatsRange.entries.forEach { r -> RangeTab(r.days, r == state.range) { onSelectRange(r) } }
            }
            when {
                state.isLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                !state.hasData -> Box(Modifier.fillMaxWidth().padding(top = ApexTheme.spacing.xxl), contentAlignment = Alignment.Center) {
                    EmptyState(
                        message = stringResource(R.string.stats_empty),
                        hint = stringResource(R.string.stats_empty_hint),
                    )
                }

                else -> StatsCharts(state, onShareStreak)
            }
            Surface(
                Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
            Column(Modifier.padding(ApexTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
                Text(stringResource(R.string.stats_sleep_title), style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = { dialog = BedtimeDialog.LAST_NIGHT },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(stringResource(R.string.stats_set_bedtime))
                }
                OutlinedButton(
                    onClick = { dialog = BedtimeDialog.USUAL },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        state.usualBedtime?.let { stringResource(R.string.stats_usual_bedtime_value, it.toString()) }
                            ?: stringResource(R.string.stats_set_usual_bedtime),
                    )
                }
            }
            }
        }
    }

    dialog?.let { which ->
        BedtimePickerDialog(
            initial = if (which == BedtimeDialog.USUAL) state.usualBedtime ?: LocalTime.of(23, 0) else LocalTime.of(23, 0),
            onDismiss = { dialog = null },
            onConfirm = {
                if (which == BedtimeDialog.USUAL) onSetUsualBedtime(it) else onSetLastNightBedtime(it)
                dialog = null
            },
        )
    }
}

@Composable
private fun StatsCharts(state: StatsUiState, onShareStreak: () -> Unit) {
    val stats = state.stats
    Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xl)) {
        // Dato clave primero: tres cifras (racha, puntualidad, tiempo medio) como tipografia en cajas planas.
        val streakDescription = stringResource(R.string.stats_streak, state.streak.current, state.streak.best)
        val punctualKpi = stats.punctualRate?.let { "${(it * 100).toInt()}%" }
        val wakeKpi = stats.averageTimeToWakeMs?.let { ms ->
            if (ms < 60_000L) stringResource(R.string.stats_kpi_seconds, ms / 1000) else formatDuration(ms)
        }
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Max).semantics(mergeDescendants = true) { contentDescription = streakDescription },
            horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm),
        ) {
            Kpi(
                stringResource(R.string.streak_card_current), state.streak.current.toString(), MaterialTheme.colorScheme.primary,
                Modifier.weight(1f).fillMaxHeight(), unit = stringResource(R.string.stats_kpi_days),
            )
            Kpi(stringResource(R.string.stats_punctuality), punctualKpi, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f).fillMaxHeight())
            Kpi(stringResource(R.string.stats_kpi_wake), wakeKpi, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f).fillMaxHeight())
        }
        TextButton(onClick = onShareStreak) { Text(stringResource(R.string.streak_share_open)) }
        val wake = stats.averageTimeToWakeMs?.let { formatDuration(it) } ?: stringResource(R.string.stats_no_data)
        val dayLabels = state.days.map { it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
        BarChart(
            title = stringResource(R.string.stats_wake_time),
            labels = dayLabels,
            bars = state.days.map { Bar(it.timeToWakeMs?.toFloat()) },
            description = stringResource(R.string.stats_wake_time_avg, wake),
        )
        BarChart(
            title = stringResource(R.string.stats_snoozes),
            labels = dayLabels,
            bars = state.days.map { Bar(it.snoozes.toFloat()) },
            description = pluralStringResource(R.plurals.stats_total, stats.totalSnoozes, stats.totalSnoozes),
        )
        BarChart(
            title = stringResource(R.string.stats_fails),
            labels = dayLabels,
            bars = state.days.map { Bar(it.fails.toFloat()) },
            description = pluralStringResource(R.plurals.stats_total, stats.totalFails, stats.totalFails),
            barColor = MaterialTheme.colorScheme.error,
        )
        val punctual = stats.punctualRate?.let { "${(it * 100).toInt()} %" } ?: stringResource(R.string.stats_no_data)
        BarChart(
            title = stringResource(R.string.stats_punctuality),
            labels = dayLabels,
            bars = state.days.map { Bar(it.punctualRate?.toFloat()) },
            description = stringResource(R.string.stats_punctuality_value, punctual),
            barColor = MaterialTheme.colorScheme.tertiary,
        )
        val estimated = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
        val sleepByDate = state.nights.associateBy { it.wakeDate }
        val avgSleep = state.averageSleepMs?.let { formatDuration(it, hours = true) } ?: stringResource(R.string.stats_no_data)
        BarChart(
            title = stringResource(R.string.stats_sleep),
            labels = dayLabels,
            bars = state.days.map { day ->
                sleepByDate[day.date]?.let { Bar(it.sleepMs.toFloat(), if (it.source == BedtimeSource.ESTIMATED) estimated else null) }
                    ?: Bar(null)
            },
            description = stringResource(R.string.stats_sleep_avg, avgSleep),
            barColor = MaterialTheme.colorScheme.secondary,
        )
        Text(
            stringResource(R.string.stats_sleep_legend),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Kpi(label: String, value: String?, color: Color, modifier: Modifier = Modifier, unit: String? = null) {
    Surface(
        modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(ApexTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xs)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(verticalArrangement = Arrangement.Bottom, horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xs)) {
                Text(
                    value ?: stringResource(R.string.stats_no_data),
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else color,
                )
                if (unit != null) {
                    Text(unit, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Pestana de rango: la elegida lleva caja de nivel superior y texto en acento. */
@Composable
private fun RangeTab(days: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.defaultMinSize(minHeight = 48.dp).selectable(selected = selected, role = Role.Tab, onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
    ) {
        Box(Modifier.padding(horizontal = ApexTheme.spacing.lg), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.stats_range_days, days),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun formatDuration(ms: Long, hours: Boolean = false): String {
    val totalSeconds = ms / 1000
    return if (hours) {
        stringResource(R.string.stats_duration_hm, totalSeconds / 3600, totalSeconds % 3600 / 60)
    } else {
        stringResource(R.string.stats_duration_ms, totalSeconds / 60, totalSeconds % 60)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BedtimePickerDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val picker = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(picker.hour, picker.minute)) }) {
                Text(stringResource(R.string.stats_dialog_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.stats_dialog_cancel)) } },
        text = { TimePicker(state = picker) },
    )
}

private val PreviewState = StatsUiState(
    isLoading = false,
    hasData = true,
    streak = Streak(5, 12),
    days = (0 until 7).map {
        DayPoint(
            date = LocalDate.of(2026, 10, 19).plusDays(it.toLong()),
            timeToWakeMs = (60_000L + it * 45_000L).takeIf { _ -> it != 3 },
            snoozes = it % 3,
            fails = if (it == 4) 2 else 0,
            punctualRate = if (it == 3) null else 1.0 - it * 0.1,
        )
    },
)

@Composable
private fun StatsPreview() = StatsContent(PreviewState, onBack = {}, onSelectRange = {}, onSetLastNightBedtime = {}, onSetUsualBedtime = {})

@Preview(name = "Claro", showBackground = true, heightDp = 1400)
@Composable
private fun StatsLight() = ApexAppTheme(darkTheme = false) { StatsPreview() }

@Preview(name = "Oscuro", showBackground = true, heightDp = 1400, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatsDark() = ApexAppTheme(darkTheme = true) { StatsPreview() }

@Preview(name = "Fuente 1.3", showBackground = true, heightDp = 1600, fontScale = 1.3f)
@Composable
private fun StatsLargeFont() = ApexAppTheme(darkTheme = false) { StatsPreview() }
