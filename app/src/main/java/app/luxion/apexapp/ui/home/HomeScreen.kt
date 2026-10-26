package app.luxion.apexapp.ui.home

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.alarm.PermissionState
import app.luxion.apexapp.alarm.ReliabilityState
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.ChallengeConfig
import app.luxion.apexapp.domain.ChallengeSpec
import app.luxion.apexapp.domain.nextOccurrence
import app.luxion.apexapp.ui.components.EmptyState
import app.luxion.apexapp.ui.components.challengeTypesLabel
import app.luxion.apexapp.ui.editor.remainingLabel
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

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
        bottomBar = { HomeNavigationBar(onOpenChallengeTest, onOpenStats) },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            HomeTopBar(onOpenSettings)
            PermissionBanners(state.permissions, onRequestNotifications, onOpenExactAlarmSettings)
            ReliabilityBanner(state.reliability, onOpenReliability)
            Box(Modifier.weight(1f).fillMaxWidth()) {
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

                    else -> AlarmList(state.alarms, onToggle, onEditAlarm, onDeleteAlarm, onOpenImport)
                }
            }
            if (!state.isLoading) AddAlarmButton(onAddAlarm)
        }
    }
}

@Composable
private fun AlarmList(
    alarms: List<Alarm>,
    onToggle: (Long, Boolean) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onOpenImport: () -> Unit,
) {
    // La cuenta atrás se recalcula cada minuto mientras la lista está en pantalla.
    val minute by produceState(0) {
        var ticks = 0
        while (true) {
            delay(MinuteMillis)
            value = ++ticks
        }
    }
    val next = remember(alarms, minute) { nextAlarm(alarms, Clock.systemDefaultZone()) }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = ApexTheme.spacing.screen, vertical = ApexTheme.spacing.lg),
    ) {
        if (next != null) {
            item(key = "next") { NextAlarmCard(next.first, next.second) }
        }
        item(key = "header") {
            Text(
                stringResource(R.string.home_scheduled).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = ApexTheme.spacing.xl, bottom = ApexTheme.spacing.sm),
            )
        }
        items(alarms, key = { it.id }) { alarm ->
            AlarmRow(alarm, onToggle, onEdit, onDelete)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        item(key = "import") { TextButton(onClick = onOpenImport) { Text(stringResource(R.string.import_action)) } }
    }
}

/** Próxima alarma activa y el tiempo que falta para que suene. */
internal fun nextAlarm(alarms: List<Alarm>, clock: Clock): Pair<Alarm, Duration>? {
    val now = ZonedDateTime.now(clock)
    return alarms.mapNotNull { alarm -> alarm.nextOccurrence(clock)?.let { alarm to Duration.between(now, it) } }
        .minByOrNull { it.second }
}

@Composable
private fun NextAlarmCard(alarm: Alarm, remaining: Duration) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag(NextAlarmTag),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(ApexTheme.spacing.xl), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xs)) {
            Text(
                stringResource(R.string.home_next_in, remainingLabel(remaining)).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "%02d:%02d".format(alarm.hour, alarm.minute),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            val details = listOfNotNull(alarm.label?.takeIf { it.isNotBlank() }, challengeLine(alarm)).joinToString(" · ")
            if (details.isNotEmpty()) {
                Text(details, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun challengeLine(alarm: Alarm): String? =
    challengeTypesLabel(alarm.challenges)?.let { stringResource(R.string.home_challenge_line, it) }

@Composable
private fun AddAlarmButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ApexTheme.spacing.screen, vertical = ApexTheme.spacing.lg)
            .defaultMinSize(minHeight = 56.dp),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = null)
        Spacer(Modifier.width(ApexTheme.spacing.sm))
        Text(stringResource(R.string.home_add_alarm).uppercase(), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun HomeNavigationBar(onOpenChallengeTest: () -> Unit, onOpenStats: () -> Unit) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = Color.Transparent,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(painterResource(R.drawable.ic_alarm), contentDescription = null) },
            label = { Text(stringResource(R.string.home_nav_alarms), style = MaterialTheme.typography.labelLarge) },
            colors = colors,
        )
        NavigationBarItem(
            selected = false,
            onClick = onOpenChallengeTest,
            icon = { Icon(painterResource(R.drawable.ic_flag), contentDescription = null) },
            label = { Text(stringResource(R.string.challenge_test_action), style = MaterialTheme.typography.labelLarge) },
            colors = colors,
        )
        NavigationBarItem(
            selected = false,
            onClick = onOpenStats,
            icon = { Icon(painterResource(R.drawable.ic_history), contentDescription = null) },
            label = { Text(stringResource(R.string.stats_open), style = MaterialTheme.typography.labelLarge) },
            colors = colors,
        )
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
private fun HomeTopBar(onOpenSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = ApexTheme.spacing.screen, end = ApexTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_alarm), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            stringResource(R.string.home_wordmark),
            modifier = Modifier.padding(start = ApexTheme.spacing.md).weight(1f),
            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 2.sp),
            color = MaterialTheme.colorScheme.primary,
        )
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(56.dp)) {
            Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings_title))
        }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AlarmRow(
    alarm: Alarm,
    onToggle: (Long, Boolean) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    // Deslizar hacia la izquierda borra y la lista ofrece "Deshacer"; no hay botón fijo por fila.
    val deleteLabel = stringResource(R.string.home_delete)
    val dismiss = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete(alarm.id)
            false
        },
    )
    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = false,
        modifier = Modifier.testTag(AlarmRowTag + alarm.id).semantics {
            customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(alarm.id); true })
        },
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = ApexTheme.spacing.lg),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(deleteLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        // Fila sin tarjeta: la hora manda y el resto queda subordinado. Apagada, se atenúa.
        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).clickable { onEdit(alarm.id) }
                .padding(vertical = ApexTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xs),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).alpha(if (alarm.enabled) 1f else DisabledAlpha),
                    horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md),
                ) {
                    Text(
                        "%02d:%02d".format(alarm.hour, alarm.minute),
                        modifier = Modifier.alignByBaseline(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    alarm.label?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            modifier = Modifier.alignByBaseline().weight(1f, fill = false),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(checked = alarm.enabled, onCheckedChange = { onToggle(alarm.id, it) })
            }
            FlowRow(
                Modifier.fillMaxWidth().alpha(if (alarm.enabled) 1f else DisabledAlpha),
                horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xs),
            ) {
                DayLetters(alarm.repeatDays)
                challengeLine(alarm)?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** Siete letras (L M X J V S D): las del repetido, en acento; las demás, atenuadas. Sin repetir, "Una vez". */
@Composable
private fun DayLetters(days: Set<DayOfWeek>) {
    if (days.isEmpty()) {
        Text(stringResource(R.string.alarm_once), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md)) {
        DayOfWeek.entries.forEach { day ->
            val on = day in days
            Text(
                day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
                color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** Resumen de texto de los días de repetición; lo usa también la pantalla de importar. */
@Composable
internal fun daysSummary(days: Set<DayOfWeek>): String = when {
    days.isEmpty() -> stringResource(R.string.alarm_once)
    days.size == 7 -> stringResource(R.string.alarm_every_day)
    else -> DayOfWeek.entries.filter { it in days }
        .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

private const val DisabledAlpha = 0.5f
private const val MinuteMillis = 60_000L
internal const val NextAlarmTag = "next_alarm"
internal const val AlarmRowTag = "alarm_row_"

private val PreviewAlarms = listOf(
    Alarm(
        id = 1, hour = 6, minute = 45, repeatDays = DayOfWeek.entries.take(5).toSet(), label = "Trabajo",
        challenges = ChallengeConfig(challenges = listOf(ChallengeSpec("qr"))),
    ),
    Alarm(
        id = 2, hour = 7, minute = 30, repeatDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
        label = "Entrenamiento", challenges = ChallengeConfig(challenges = listOf(ChallengeSpec("math"))),
    ),
    Alarm(id = 3, hour = 9, minute = 0, repeatDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), enabled = false, label = "Fin de semana"),
)

@Composable
private fun HomePreview() = HomeContent(
    state = HomeUiState(isLoading = false, alarms = PreviewAlarms),
    onAddAlarm = {},
    onToggle = { _, _ -> },
    onRequestNotifications = {},
    onOpenExactAlarmSettings = {},
)

@Preview(name = "Claro", showBackground = true, heightDp = 780)
@Composable
private fun HomeLight() = ApexAppTheme(darkTheme = false) { HomePreview() }

@Preview(name = "Oscuro", showBackground = true, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeDark() = ApexAppTheme(darkTheme = true) { HomePreview() }

@Preview(name = "Fuente 1.3", showBackground = true, heightDp = 780, fontScale = 1.3f)
@Composable
private fun HomeLargeFont() = ApexAppTheme(darkTheme = false) { HomePreview() }
