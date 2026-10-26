package app.luxion.apexapp.ui.reliability

import androidx.annotation.StringRes
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.alarm.IssueSeverity
import app.luxion.apexapp.alarm.ReliabilityIssue
import app.luxion.apexapp.alarm.ReliabilityState
import app.luxion.apexapp.alarm.ScheduleResult
import app.luxion.apexapp.ui.components.PrimaryAction
import app.luxion.apexapp.ui.components.SectionHeader
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme

@Composable
fun ReliabilityScreen(onBack: () -> Unit, viewModel: ReliabilityViewModel = hiltViewModel()) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    // El usuario sale a Ajustes y vuelve: se vuelve a comprobar cada vez que la pantalla se reanuda.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    ReliabilityContent(
        ui = ui,
        onBack = onBack,
        onOpen = viewModel::open,
        onConfirmGuide = viewModel::confirmGuide,
        onRunTest = viewModel::runTest,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReliabilityContent(
    ui: ReliabilityUiState,
    onBack: () -> Unit,
    onOpen: (ReliabilityIssue) -> Unit,
    onConfirmGuide: () -> Unit,
    onRunTest: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reliability_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg),
        ) {
            Text(
                stringResource(
                    when {
                        ui.state.isReliable -> R.string.reliability_summary_ok
                        ui.state.hasCritical -> R.string.reliability_summary_critical
                        else -> R.string.reliability_summary_recommended
                    },
                ),
                modifier = Modifier.padding(horizontal = ApexTheme.spacing.screen),
                style = MaterialTheme.typography.bodyLarge,
                color = if (ui.state.hasCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )

            SectionHeader(stringResource(R.string.reliability_section_checks))
            CHECKS.forEach { check ->
                CheckRow(
                    title = stringResource(check.title),
                    description = stringResource(check.description),
                    pending = check.issue in ui.state.issues,
                    critical = check.issue.severity == IssueSeverity.Critical,
                    onFix = { onOpen(check.issue) },
                )
            }

            ui.guide?.let { guide ->
                SectionHeader(stringResource(R.string.reliability_section_manufacturer, stringResource(guide.brandRes)))
                val pending = ReliabilityIssue.ManufacturerSetup in ui.state.issues
                Column(
                    Modifier.padding(horizontal = ApexTheme.spacing.screen),
                    verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm),
                ) {
                    Text(stringResource(guide.stepsRes), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
                        OutlinedButton(onClick = { onOpen(ReliabilityIssue.ManufacturerSetup) }, shape = MaterialTheme.shapes.small) {
                            Text(stringResource(R.string.reliability_open_settings))
                        }
                        if (pending) {
                            OutlinedButton(onClick = onConfirmGuide, shape = MaterialTheme.shapes.small) { Text(stringResource(R.string.reliability_guide_done)) }
                        }
                    }
                }
            }

            SectionHeader(stringResource(R.string.reliability_section_test))
            Column(
                Modifier.padding(horizontal = ApexTheme.spacing.screen).padding(bottom = ApexTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm),
            ) {
                Text(stringResource(R.string.reliability_test_hint), style = MaterialTheme.typography.bodyMedium)
                PrimaryAction(stringResource(R.string.reliability_test_button), onRunTest, Modifier.fillMaxWidth())
                when (ui.testResult) {
                    ScheduleResult.Scheduled -> Text(stringResource(R.string.reliability_test_scheduled), color = MaterialTheme.colorScheme.tertiary)
                    ScheduleResult.PermissionMissing -> Text(
                        stringResource(R.string.reliability_test_permission_missing),
                        color = MaterialTheme.colorScheme.error,
                    )

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun CheckRow(title: String, description: String, pending: Boolean, critical: Boolean, onFix: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = ApexTheme.spacing.screen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                if (pending) description else stringResource(R.string.reliability_done),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    !pending -> MaterialTheme.colorScheme.tertiary
                    critical -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (pending) {
            OutlinedButton(onClick = onFix, shape = MaterialTheme.shapes.small) { Text(stringResource(R.string.reliability_fix)) }
        }
    }
}

private data class Check(val issue: ReliabilityIssue, @StringRes val title: Int, @StringRes val description: Int)

private val CHECKS = listOf(
    Check(ReliabilityIssue.ExactAlarmDenied, R.string.reliability_exact_title, R.string.reliability_exact_desc),
    Check(ReliabilityIssue.NotificationsDisabled, R.string.reliability_notifications_title, R.string.reliability_notifications_desc),
    Check(ReliabilityIssue.RingingChannelMuted, R.string.reliability_channel_title, R.string.reliability_channel_desc),
    Check(ReliabilityIssue.FullScreenIntentDenied, R.string.reliability_fullscreen_title, R.string.reliability_fullscreen_desc),
    Check(ReliabilityIssue.BatteryOptimized, R.string.reliability_battery_title, R.string.reliability_battery_desc),
)

@Preview(name = "Claro", showBackground = true, heightDp = 1200)
@Composable
private fun ReliabilityLight() = ApexAppTheme(darkTheme = false) { ReliabilityContent(ReliabilityUiState(state = ReliabilityState(setOf(ReliabilityIssue.ExactAlarmDenied, ReliabilityIssue.BatteryOptimized)), testResult = ScheduleResult.Scheduled), onBack = {}, onOpen = {}, onConfirmGuide = {}, onRunTest = {}) }

@Preview(name = "Oscuro", showBackground = true, heightDp = 1200, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReliabilityDark() = ApexAppTheme(darkTheme = true) { ReliabilityContent(ReliabilityUiState(state = ReliabilityState(setOf(ReliabilityIssue.ExactAlarmDenied, ReliabilityIssue.BatteryOptimized)), testResult = ScheduleResult.Scheduled), onBack = {}, onOpen = {}, onConfirmGuide = {}, onRunTest = {}) }

@Preview(name = "Fuente 1.3", showBackground = true, heightDp = 1500, fontScale = 1.3f)
@Composable
private fun ReliabilityLargeFont() = ApexAppTheme(darkTheme = false) { ReliabilityContent(ReliabilityUiState(state = ReliabilityState(setOf(ReliabilityIssue.ExactAlarmDenied, ReliabilityIssue.BatteryOptimized)), testResult = ScheduleResult.Scheduled), onBack = {}, onOpen = {}, onConfirmGuide = {}, onRunTest = {}) }
