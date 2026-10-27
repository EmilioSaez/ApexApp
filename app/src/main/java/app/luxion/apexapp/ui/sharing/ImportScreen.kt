package app.luxion.apexapp.ui.sharing

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.AntiCheatLevel
import app.luxion.apexapp.domain.challenge.flappy.FlappyChallenge
import app.luxion.apexapp.domain.challenge.sensor.MotionKind
import app.luxion.apexapp.domain.challenge.touch.TapTargetsChallenge
import app.luxion.apexapp.scanner.BarcodeScanner
import app.luxion.apexapp.scanner.CameraPermissionGate
import app.luxion.apexapp.sharing.ImportError
import app.luxion.apexapp.sharing.ImportIntents
import app.luxion.apexapp.sharing.ImportNote
import app.luxion.apexapp.sharing.ImportPreview
import app.luxion.apexapp.ui.components.PrimaryAction
import app.luxion.apexapp.ui.components.SectionHeader
import app.luxion.apexapp.ui.components.TimeDisplay
import app.luxion.apexapp.ui.home.daysSummary
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme

@Composable
fun ImportScreen(onBack: () -> Unit, viewModel: ImportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var scanning by rememberSaveable { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = ImportIntents.readBounded(context.contentResolver, uri)
        if (bytes == null) viewModel.onFileUnreadable() else viewModel.onFile(bytes)
    }

    val importedMessage = stringResource(R.string.import_imported)
    LaunchedEffect(state) {
        if (state != ImportUiState.Imported) return@LaunchedEffect
        Toast.makeText(context, importedMessage, Toast.LENGTH_LONG).show()
        onBack()
    }
    LaunchedEffect(state) { if (state !is ImportUiState.Entry) scanning = false }

    ImportContent(
        state = state,
        scanning = scanning,
        onBack = onBack,
        onScan = { scanning = true },
        onStopScan = { scanning = false },
        onScanned = viewModel::onLink,
        onPickFile = { filePicker.launch(arrayOf("*/*")) },
        onAccept = viewModel::accept,
        onDiscard = viewModel::reset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportContent(
    state: ImportUiState,
    scanning: Boolean,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onStopScan: () -> Unit,
    onScanned: (String) -> Unit,
    onPickFile: () -> Unit,
    onAccept: () -> Unit,
    onDiscard: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = ApexTheme.spacing.screen, vertical = ApexTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xl),
        ) {
            when {
                state is ImportUiState.Preview ->
                    PreviewSection(state.preview, onAccept = onAccept, onDiscard = onDiscard)

                state is ImportUiState.Failed -> {
                    Text(
                        stringResource(state.error.messageRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(onClick = onDiscard, shape = MaterialTheme.shapes.small) {
                        Text(stringResource(R.string.import_discard))
                    }
                }

                state is ImportUiState.Saving || state is ImportUiState.Imported -> Unit

                scanning -> ScannerSection(onScanned = onScanned, onStop = onStopScan)

                else -> {
                    Text(
                        stringResource(R.string.import_entry_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md)) {
                        PrimaryAction(
                            text = stringResource(R.string.import_scan),
                            onClick = onScan,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedButton(
                            onClick = onPickFile,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                        ) { Text(stringResource(R.string.import_pick_file)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerSection(onScanned: (String) -> Unit, onStop: () -> Unit) {
    Text(
        stringResource(R.string.import_scan_hint),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    CameraPermissionGate(Modifier.fillMaxWidth()) {
        BarcodeScanner(onScanned = { onScanned(it.value) }, modifier = Modifier.fillMaxWidth())
    }
    TextButton(onClick = onStop) { Text(stringResource(R.string.import_discard)) }
}

@Composable
private fun PreviewSection(preview: ImportPreview, onAccept: () -> Unit, onDiscard: () -> Unit) {
    val alarm = preview.alarm
    Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xs)) {
        SectionHeader(stringResource(R.string.import_preview_heading))
        TimeDisplay(alarm.hour, alarm.minute)
        Text(
            listOfNotNull(alarm.label, daysSummary(alarm.repeatDays)).joinToString(" · "),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
        DetailLine(challengesSummary(preview))
        DetailLine(stringResource(R.string.import_sound_default, alarm.soundConfig.volume))
        DetailLine(snoozeSummary(preview))
        DetailLine(
            stringResource(R.string.import_anticheat, stringResource(alarm.antiCheat.level.labelRes())),
        )
    }
    preview.notes.forEach { note ->
        Text(
            stringResource(note.messageRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
    Text(
        stringResource(R.string.import_disabled_note),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
        PrimaryAction(
            text = stringResource(R.string.import_accept),
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_discard))
        }
    }
}

@Composable
private fun DetailLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun challengesSummary(preview: ImportPreview): String {
    val config = preview.alarm.challenges
    if (config.challenges.isEmpty()) return stringResource(R.string.import_challenges_default)
    val names = config.challenges.map { spec ->
        challengeLabelRes(spec.type)?.let { stringResource(it) } ?: spec.type
    }.joinToString(", ")
    val difficulty = if (config.difficulty == 0) 3 else config.difficulty
    return stringResource(R.string.import_challenges, names, difficulty, config.count)
}

@Composable
private fun snoozeSummary(preview: ImportPreview): String {
    val snooze = preview.alarm.antiCheat.snooze
    return if (snooze.allowed) {
        stringResource(R.string.import_snooze_allowed, snooze.minutes, snooze.maxCount)
    } else {
        stringResource(R.string.import_snooze_denied)
    }
}

@StringRes
private fun challengeLabelRes(type: String): Int? = when (type) {
    "math" -> R.string.challenge_type_math
    MotionKind.SHAKE.typeId -> R.string.challenge_type_shake
    MotionKind.STEPS.typeId -> R.string.challenge_type_steps
    TapTargetsChallenge.TYPE.id -> R.string.challenge_type_tap_targets
    FlappyChallenge.TYPE.id -> R.string.challenge_type_flappy
    "memory" -> R.string.challenge_type_memory
    else -> null
}

@StringRes
private fun AntiCheatLevel.labelRes() = when (this) {
    AntiCheatLevel.OFF -> R.string.anticheat_level_off
    AntiCheatLevel.STANDARD -> R.string.anticheat_level_standard
    AntiCheatLevel.STRICT -> R.string.anticheat_level_strict
}

@StringRes
private fun ImportNote.messageRes() = when (this) {
    ImportNote.QR_CHALLENGE_OMITTED -> R.string.import_note_qr
    ImportNote.UNKNOWN_CHALLENGE_OMITTED -> R.string.import_note_challenge
    ImportNote.UNKNOWN_SOUND_DEFAULTED -> R.string.import_note_sound
}

@StringRes
private fun ImportError?.messageRes() = when (this) {
    ImportError.MALFORMED -> R.string.import_error_malformed
    ImportError.TOO_LARGE -> R.string.import_error_too_large
    ImportError.UNSUPPORTED_VERSION -> R.string.import_error_version
    ImportError.INVALID -> R.string.import_error_invalid
    null -> R.string.import_error_file
}

@Composable
private fun ImportPreviewScreen(state: ImportUiState) = ImportContent(
    state = state,
    scanning = false,
    onBack = {},
    onScan = {},
    onStopScan = {},
    onScanned = {},
    onPickFile = {},
    onAccept = {},
    onDiscard = {},
)

private val PreviewImport = ImportUiState.Preview(
    ImportPreview(
        alarm = Alarm(hour = 6, minute = 30, repeatDays = java.time.DayOfWeek.entries.take(5).toSet(), label = "Gimnasio"),
        soundKey = null,
        notes = setOf(ImportNote.QR_CHALLENGE_OMITTED),
    ),
)

@Preview(name = "Claro", showBackground = true)
@Composable
private fun ImportEntryLight() = ApexAppTheme(darkTheme = false) { ImportPreviewScreen(ImportUiState.Entry) }

@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImportEntryDark() = ApexAppTheme(darkTheme = true) { ImportPreviewScreen(ImportUiState.Entry) }

@Preview(name = "Fuente 1.3", showBackground = true, fontScale = 1.3f)
@Composable
private fun ImportEntryLargeFont() = ApexAppTheme(darkTheme = false) { ImportPreviewScreen(ImportUiState.Entry) }

@Preview(name = "Claro", showBackground = true, heightDp = 900)
@Composable
private fun ImportConfirmLight() = ApexAppTheme(darkTheme = false) { ImportPreviewScreen(PreviewImport) }

@Preview(name = "Oscuro", showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImportConfirmDark() = ApexAppTheme(darkTheme = true) { ImportPreviewScreen(PreviewImport) }

@Preview(name = "Fuente 1.3", showBackground = true, heightDp = 900, fontScale = 1.3f)
@Composable
private fun ImportConfirmLargeFont() = ApexAppTheme(darkTheme = false) { ImportPreviewScreen(PreviewImport) }
