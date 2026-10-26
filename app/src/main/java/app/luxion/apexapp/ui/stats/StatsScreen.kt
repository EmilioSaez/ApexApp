package app.luxion.apexapp.ui.stats

import android.content.res.Configuration
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
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
                StatsRange.entries.forEach { r ->
                    FilterChip(
                        selected = r == state.range,
                        onClick = { onSelectRange(r) },
                        shape = MaterialTheme.shapes.small,
                        label = { Text(stringResource(R.string.stats_range_days, r.days)) },
                    )
                }
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
            Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
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
        // Dato clave primero: la racha como cifra grande, no como frase.
        val streakDescription = stringResource(R.string.stats_streak, state.streak.current, state.streak.best)
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = streakDescription },
            horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xxl),
        ) {
            StreakFigure(state.streak.current, stringResource(R.string.streak_card_current), MaterialTheme.colorScheme.primary)
            StreakFigure(state.streak.best, stringResource(R.string.streak_card_best), MaterialTheme.colorScheme.onSurface)
        }
        TextButton(onClick = onShareStreak) { Text(stringResource(R.string.streak_share_open)) }
        val wake = stats.averageTimeToWakeMs?.let { formatDuration(it) } ?: stringResource(R.string.stats_no_data)
        BarChart(
            title = stringResource(R.string.stats_wake_time),
            bars = state.days.map { Bar(it.timeToWakeMs?.toFloat()) },
            description = stringResource(R.string.stats_wake_time_avg, wake),
        )
        BarChart(
            title = stringResource(R.string.stats_snoozes),
            bars = state.days.map { Bar(it.snoozes.toFloat()) },
            description = pluralStringResource(R.plurals.stats_total, stats.totalSnoozes, stats.totalSnoozes),
        )
        BarChart(
            title = stringResource(R.string.stats_fails),
            bars = state.days.map { Bar(it.fails.toFloat()) },
            description = pluralStringResource(R.plurals.stats_total, stats.totalFails, stats.totalFails),
            barColor = MaterialTheme.colorScheme.error,
        )
        val punctual = stats.punctualRate?.let { "${(it * 100).toInt()} %" } ?: stringResource(R.string.stats_no_data)
        BarChart(
            title = stringResource(R.string.stats_punctuality),
            bars = state.days.map { Bar(it.punctualRate?.toFloat()) },
            description = stringResource(R.string.stats_punctuality_value, punctual),
            barColor = MaterialTheme.colorScheme.tertiary,
        )
        val estimated = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
        val sleepByDate = state.nights.associateBy { it.wakeDate }
        val avgSleep = state.averageSleepMs?.let { formatDuration(it, hours = true) } ?: stringResource(R.string.stats_no_data)
        BarChart(
            title = stringResource(R.string.stats_sleep),
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

@Composable
private fun StreakFigure(value: Int, label: String, color: Color) {
    Column {
        Text(value.toString(), style = MaterialTheme.typography.displayMedium, color = color)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
