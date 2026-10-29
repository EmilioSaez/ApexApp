package app.luxion.apexapp.ui.editor

import android.content.res.Configuration
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.alarm.ScheduleResult
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.nextOccurrence
import app.luxion.apexapp.ui.components.TimeWheelPicker
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AlarmEditorScreen(onBack: () -> Unit, onShare: (Long) -> Unit = {}, viewModel: AlarmEditorViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val saved = state.saved
    val permissionMissing = stringResource(R.string.permission_exact_missing)
    val savedIn = saved?.remaining?.let { stringResource(R.string.editor_saved_in, remainingLabel(it)) }
    LaunchedEffect(saved) {
        if (saved == null) return@LaunchedEffect
        val message = if (saved.schedule == ScheduleResult.PermissionMissing) permissionMissing else savedIn
        message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        onBack()
    }

    AlarmEditorContent(
        state = state,
        onSave = viewModel::save,
        onBack = onBack,
        onShare = { onShare(state.draft.id) },
        onLabelChange = viewModel::setLabel,
        onToggleDay = viewModel::toggleDay,
        saveEnabled = viewModel.canSave(state),
        extraContent = {
            viewModel.sections.forEach { it.Content(state.draft, viewModel::updateDraft) }
        },
    )
}

/**
 * Contenido sin estado del editor. `extraContent` es el hueco bajo los días de repetición donde
 * otros cambios (p. ej. anti-trampa: nivel y snooze) añaden sus controles.
 */
@Composable
fun AlarmEditorContent(
    state: EditorUiState,
    onSave: (hour: Int, minute: Int) -> Unit,
    onBack: () -> Unit,
    onLabelChange: (String) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    saveEnabled: Boolean = true,
    is24Hour: Boolean = DateFormat.is24HourFormat(LocalContext.current),
    onShare: (() -> Unit)? = null,
    extraContent: @Composable ColumnScope.() -> Unit = {},
) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    // El selector se crea ya cargada la alarma: así arranca con su hora y no con la de por defecto.
    var hour by rememberSaveable { mutableIntStateOf(state.hour) }
    var minute by rememberSaveable { mutableIntStateOf(state.minute) }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            Box(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = ApexTheme.spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(56.dp)) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.back))
                }
                Text(
                    stringResource(if (state.isNew) R.string.editor_title else R.string.editor_title_edit),
                    style = MaterialTheme.typography.titleLarge,
                )
                if (!state.isNew && onShare != null) {
                    TextButton(onClick = onShare, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Text(stringResource(R.string.share_open))
                    }
                }
            }
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Button(
                    onClick = { onSave(hour, minute) },
                    enabled = saveEnabled,
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
                    Icon(painterResource(R.drawable.ic_check), contentDescription = null)
                    Spacer(Modifier.width(ApexTheme.spacing.sm))
                    Text(stringResource(R.string.editor_save), style = MaterialTheme.typography.labelLarge)
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = ApexTheme.spacing.screen, vertical = ApexTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg),
        ) {
            TimeCard(hour, minute, { h, m -> hour = h; minute = m }, state.repeatDays, is24Hour)
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md)) {
                EditorHeader(stringResource(R.string.editor_repeat))
                DayToggles(state.repeatDays, onToggleDay)
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md)) {
                EditorHeader(stringResource(R.string.editor_params))
                OutlinedTextField(
                    value = state.label,
                    onValueChange = onLabelChange,
                    label = { Text(stringResource(R.string.editor_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            extraContent()
        }
    }
}

@Composable
private fun EditorHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** La hora manda: las ruedas en una caja propia y, encima, cuánto falta para que suene. */
@Composable
private fun TimeCard(hour: Int, minute: Int, onTimeChange: (Int, Int) -> Unit, repeatDays: Set<DayOfWeek>, is24Hour: Boolean) {
    val clock = remember { Clock.systemDefaultZone() }
    val remaining = remember(hour, minute, repeatDays) {
        Alarm(hour = hour, minute = minute, repeatDays = repeatDays).nextOccurrence(clock)
            ?.let { Duration.between(ZonedDateTime.now(clock), it) }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(vertical = ApexTheme.spacing.xl, horizontal = ApexTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg),
        ) {
            if (remaining != null) {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Row(
                        Modifier.padding(horizontal = ApexTheme.spacing.md, vertical = ApexTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm),
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_schedule),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(R.string.home_next_in, remainingLabel(remaining)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            TimeWheelPicker(hour, minute, onTimeChange, is24Hour = is24Hour)
            Text(
                stringResource(R.string.time_wheel_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Siete casillas de igual ancho: la elegida lleva borde y punto de acento. */
@Composable
private fun DayToggles(days: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
        DayOfWeek.entries.forEach { day ->
            val on = day in days
            Surface(
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 56.dp).toggleable(
                    value = on,
                    role = Role.Checkbox,
                    onValueChange = { onToggle(day) },
                ),
                shape = MaterialTheme.shapes.small,
                color = if (on) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    Modifier.padding(vertical = ApexTheme.spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xs),
                ) {
                    Text(
                        day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                        Modifier.size(4.dp).background(
                            if (on) MaterialTheme.colorScheme.primary else Color.Transparent,
                            CircleShape,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
internal fun remainingLabel(duration: java.time.Duration): String {
    val t = duration.toRemainingTime()
    return when {
        t.days > 0 -> stringResource(R.string.editor_remaining_dhm, t.days, t.hours, t.minutes)
        t.hours > 0 -> stringResource(R.string.editor_remaining_hm, t.hours, t.minutes)
        else -> stringResource(R.string.editor_remaining_m, t.minutes)
    }
}

@Composable
private fun EditorPreview() = AlarmEditorContent(
    state = EditorUiState(isLoading = false, isNew = false, hour = 6, minute = 30, repeatDays = DayOfWeek.entries.take(5).toSet(), label = "Gimnasio"),
    onSave = { _, _ -> },
    onBack = {},
    onLabelChange = {},
    onToggleDay = {},
    onShare = {},
    is24Hour = true,
)

@Preview(name = "Claro", showBackground = true, heightDp = 900)
@Composable
private fun EditorLight() = ApexAppTheme(darkTheme = false) { EditorPreview() }

@Preview(name = "Oscuro", showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditorDark() = ApexAppTheme(darkTheme = true) { EditorPreview() }

@Preview(name = "Fuente 1.3", showBackground = true, heightDp = 900, fontScale = 1.3f)
@Composable
private fun EditorLargeFont() = ApexAppTheme(darkTheme = false) { EditorPreview() }
