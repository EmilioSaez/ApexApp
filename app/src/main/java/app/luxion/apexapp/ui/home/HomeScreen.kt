package app.luxion.apexapp.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import app.luxion.apexapp.ui.theme.ApexTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReliability: () -> Unit,
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

@OptIn(ExperimentalMaterial3Api::class)
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
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge) },
                actions = { TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.settings_title)) } },
            )
        },
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
            PermissionBanners(state.permissions, onRequestNotifications, onOpenExactAlarmSettings)
            ReliabilityBanner(state.reliability, onOpenReliability)
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.alarms.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        message = stringResource(R.string.home_empty),
                        hint = stringResource(R.string.home_empty_hint),
                    )
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = ApexTheme.spacing.screen,
                        vertical = ApexTheme.spacing.lg,
                    ),
                    verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xl),
                ) {
                    items(state.alarms, key = { it.id }) { alarm -> AlarmRow(alarm, onToggle, onEditAlarm, onDeleteAlarm) }
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

@Composable
private fun Banner(message: String, action: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            Button(onClick = onAction, shape = MaterialTheme.shapes.small) { Text(action) }
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
    // Fila sin tarjeta: la hora manda y el resto queda subordinado.
    Row(
        Modifier.fillMaxWidth().clickable { onEdit(alarm.id) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg),
    ) {
        Column(Modifier.weight(1f)) {
            TimeDisplay(alarm.hour, alarm.minute)
            Text(
                listOfNotNull(alarm.label, daysSummary(alarm.repeatDays)).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = { onDelete(alarm.id) }) { Text(stringResource(R.string.home_delete)) }
        Switch(checked = alarm.enabled, onCheckedChange = { onToggle(alarm.id, it) })
    }
}

@Composable
private fun daysSummary(days: Set<DayOfWeek>): String = when {
    days.isEmpty() -> stringResource(R.string.alarm_once)
    days.size == 7 -> stringResource(R.string.alarm_every_day)
    else -> DayOfWeek.entries.filter { it in days }
        .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}
