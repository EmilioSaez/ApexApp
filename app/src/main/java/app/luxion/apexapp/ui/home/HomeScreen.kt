package app.luxion.apexapp.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import app.luxion.apexapp.domain.Alarm
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    onAddAlarm: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshPermissions()
    }

    HomeContent(
        state = state,
        onAddAlarm = onAddAlarm,
        onToggle = viewModel::setEnabled,
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
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAlarm) { Text("+") }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PermissionBanners(state.permissions, onRequestNotifications, onOpenExactAlarmSettings)
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.alarms.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.home_empty), style = MaterialTheme.typography.bodyLarge)
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.alarms, key = { it.id }) { alarm -> AlarmRow(alarm, onToggle) }
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
private fun Banner(message: String, action: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            Button(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun AlarmRow(alarm: Alarm, onToggle: (Long, Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(formatTime(alarm), style = MaterialTheme.typography.headlineMedium)
                Text(
                    listOfNotNull(alarm.label, daysSummary(alarm.repeatDays)).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(checked = alarm.enabled, onCheckedChange = { onToggle(alarm.id, it) })
        }
    }
}

private fun formatTime(alarm: Alarm) = "%02d:%02d".format(alarm.hour, alarm.minute)

@Composable
private fun daysSummary(days: Set<DayOfWeek>): String = when {
    days.isEmpty() -> stringResource(R.string.alarm_once)
    days.size == 7 -> stringResource(R.string.alarm_every_day)
    else -> DayOfWeek.entries.filter { it in days }
        .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}
