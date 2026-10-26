package app.luxion.apexapp.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.alarm.PermissionState
import app.luxion.apexapp.alarm.ReliabilityState
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.ui.components.EmptyState
import app.luxion.apexapp.ui.components.TimeDisplay
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme
import android.content.res.Configuration
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReliability: () -> Unit,
    onOpenChallengeTest: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenImport: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshPermissions()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val deletedMessage = stringResource(R.string.home_deleted)
    val undoLabel = stringResource(R.string.home_undo)
    LaunchedEffect(state.deleted) {
        if (state.deleted == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(deletedMessage, actionLabel = undoLabel, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.consumeDeleted()
    }

    HomeContent(
        state = state,
        onAddAlarm = onAddAlarm,
        onOpenSettings = onOpenSettings,
        onOpenReliability = onOpenReliability,
        onOpenChallengeTest = onOpenChallengeTest,
        onOpenStats = onOpenStats,
        onOpenImport = onOpenImport,
        onToggle = viewModel::setEnabled,
        onEditAlarm = onEditAlarm,
        onDeleteAlarm = viewModel::delete,
        snackbarHostState = snackbarHostState,
        onRequestNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onOpenExactAlarmSettings = { viewModel.exactAlarmSettingsIntent()?.let(context::startActivity) },
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onAddAlarm: () -> Unit,
    onToggle: (id: Long, enabled: Boolean) -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onEditAlarm: (Long) -> Unit = {},
    onDeleteAlarm: (Long) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onOpenReliability: () -> Unit = {},
    onOpenChallengeTest: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenImport: () -> Unit = {},
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddAlarm,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Text(stringResource(R.string.home_add_alarm), style = MaterialTheme.typography.labelLarge) }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            HomeHeader(onOpenChallengeTest, onOpenStats, onOpenSettings)
            PermissionBanners(state.permissions, onRequestNotifications, onOpenExactAlarmSettings)
            ReliabilityBanner(state.reliability, onOpenReliability)
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.alarms.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        EmptyState(
                            message = stringResource(R.string.home_empty),
                            hint = stringResource(R.string.home_empty_hint),
                        )
                        TextButton(onClick = onOpenImport) { Text(stringResource(R.string.import_action)) }
                    }
                }

                else -> LazyColumn(
                    // Margen inferior para que el botón flotante no tape la última fila.
                    contentPadding = PaddingValues(
                        start = ApexTheme.spacing.screen,
                        top = ApexTheme.spacing.lg,
                        end = ApexTheme.spacing.screen,
                        bottom = ApexTheme.spacing.xxxl * 2,
                    ),
                    verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xl),
                ) {
                    items(state.alarms, key = { it.id }) { alarm -> AlarmRow(alarm, onToggle, onEditAlarm, onDeleteAlarm) }
                    item { TextButton(onClick = onOpenImport) { Text(stringResource(R.string.import_action)) } }
                }
            }
        }
    }
}

@Composable
private fun PermissionBanners(
    permissions: PermissionState,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
) {
    if (!permissions.notificationsGranted) {
        Banner(stringResource(R.string.permission_notifications_missing), stringResource(R.string.permission_grant), onRequestNotifications)
    }
    if (!permissions.exactAlarmsGranted) {
        Banner(stringResource(R.string.permission_exact_missing), stringResource(R.string.permission_open_settings), onOpenExactAlarmSettings)
    }
}

@Composable
private fun ReliabilityBanner(reliability: ReliabilityState, onOpen: () -> Unit) {
    if (reliability.isReliable) return
    Banner(
        stringResource(if (reliability.hasCritical) R.string.home_reliability_critical else R.string.home_reliability_recommended),
        stringResource(R.string.home_reliability_open),
        onOpen,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeHeader(onOpenChallengeTest: () -> Unit, onOpenStats: () -> Unit, onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = ApexTheme.spacing.screen, vertical = ApexTheme.spacing.lg)) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
        // Navegación secundaria: enlaces discretos que saltan de línea con fuente grande.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
            HeaderLink(stringResource(R.string.challenge_test_action), onOpenChallengeTest)
            HeaderLink(stringResource(R.string.stats_open), onOpenStats)
            HeaderLink(stringResource(R.string.settings_title), onOpenSettings)
        }
    }
}

@Composable
private fun HeaderLink(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Banner(message: String, action: String, onAction: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ApexTheme.spacing.screen, vertical = ApexTheme.spacing.xs),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(Modifier.padding(ApexTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xs)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onAction, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                Text(action, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    onToggle: (Long, Boolean) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    // Fila sin tarjeta: la hora manda y el resto queda subordinado. Apagada, se atenúa.
    Row(
        Modifier.fillMaxWidth().clickable { onEdit(alarm.id) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg),
    ) {
        Column(Modifier.weight(1f).alpha(if (alarm.enabled) 1f else DisabledAlpha)) {
            TimeDisplay(alarm.hour, alarm.minute)
            Text(
                listOfNotNull(alarm.label, daysSummary(alarm.repeatDays)).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Switch(checked = alarm.enabled, onCheckedChange = { onToggle(alarm.id, it) })
            TextButton(
                onClick = { onDelete(alarm.id) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            ) { Text(stringResource(R.string.home_delete), style = MaterialTheme.typography.labelLarge) }
        }
    }
}

private const val DisabledAlpha = 0.5f

@Composable
internal fun daysSummary(days: Set<DayOfWeek>): String = when {
    days.isEmpty() -> stringResource(R.string.alarm_once)
    days.size == 7 -> stringResource(R.string.alarm_every_day)
    else -> DayOfWeek.entries.filter { it in days }
        .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

private val PreviewAlarms = listOf(
    Alarm(id = 1, hour = 6, minute = 30, repeatDays = DayOfWeek.entries.take(5).toSet(), label = "Gimnasio"),
    Alarm(id = 2, hour = 8, minute = 0, repeatDays = DayOfWeek.entries.toSet(), enabled = false),
    Alarm(id = 3, hour = 23, minute = 15),
)

@Composable
private fun HomePreview() = HomeContent(
    state = HomeUiState(isLoading = false, alarms = PreviewAlarms),
    onAddAlarm = {},
    onToggle = { _, _ -> },
    onRequestNotifications = {},
    onOpenExactAlarmSettings = {},
)

@Preview(name = "Claro", showBackground = true)
@Composable
private fun HomeLight() = ApexAppTheme(darkTheme = false) { HomePreview() }

@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeDark() = ApexAppTheme(darkTheme = true) { HomePreview() }

@Preview(name = "Fuente 1.3", showBackground = true, fontScale = 1.3f)
@Composable
private fun HomeLargeFont() = ApexAppTheme(darkTheme = false) { HomePreview() }
