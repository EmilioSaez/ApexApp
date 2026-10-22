package app.luxion.apexapp.ui.editor

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.alarm.ScheduleResult
import app.luxion.apexapp.ui.components.PrimaryAction
import app.luxion.apexapp.ui.theme.ApexTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AlarmEditorScreen(onBack: () -> Unit, viewModel: AlarmEditorViewModel = hiltViewModel()) {
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorContent(
    state: EditorUiState,
    onSave: (hour: Int, minute: Int) -> Unit,
    onBack: () -> Unit,
    onLabelChange: (String) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    saveEnabled: Boolean = true,
    extraContent: @Composable ColumnScope.() -> Unit = {},
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isNew) R.string.editor_title else R.string.editor_title_edit))
                },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val timeState = rememberTimePickerState(state.hour, state.minute, is24Hour = true)
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = ApexTheme.spacing.screen),
            verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimePicker(timeState)
            OutlinedTextField(
                value = state.label,
                onValueChange = onLabelChange,
                label = { Text(stringResource(R.string.editor_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.editor_repeat),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = ApexTheme.spacing.sm),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in state.repeatDays,
                            onClick = { onToggleDay(day) },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                        )
                    }
                }
            }
            extraContent()
            PrimaryAction(
                text = stringResource(R.string.editor_save),
                onClick = { onSave(timeState.hour, timeState.minute) },
                modifier = Modifier.fillMaxWidth(),
                enabled = saveEnabled,
            )
        }
    }
}

@Composable
private fun remainingLabel(duration: java.time.Duration): String {
    val t = duration.toRemainingTime()
    return when {
        t.days > 0 -> stringResource(R.string.editor_remaining_dhm, t.days, t.hours, t.minutes)
        t.hours > 0 -> stringResource(R.string.editor_remaining_hm, t.hours, t.minutes)
        else -> stringResource(R.string.editor_remaining_m, t.minutes)
    }
}
