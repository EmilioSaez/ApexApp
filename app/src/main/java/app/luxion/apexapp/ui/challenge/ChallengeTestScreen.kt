package app.luxion.apexapp.ui.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.SessionConfig
import app.luxion.apexapp.ui.components.SectionHeader
import app.luxion.apexapp.ui.theme.ApexTheme

/**
 * Pantalla de prueba: configura y ejecuta una sesión de retos sin alarma real. No usa el repositorio
 * ni el programador de alarmas, así que no puede crear ni programar ninguna.
 */
@Composable
fun ChallengeTestScreen(onBack: () -> Unit, viewModel: ChallengeTestViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val running by viewModel.running.collectAsStateWithLifecycle()
    val config = running
    if (config == null) {
        ChallengeTestContent(
            form = form,
            availableTypes = viewModel.availableTypes,
            onDifficulty = viewModel::setDifficulty,
            onCount = viewModel::setCount,
            onMode = viewModel::setMode,
            onToggleType = viewModel::toggleType,
            onStart = viewModel::start,
            onBack = onBack,
        )
    } else {
        ChallengeRunScaffold(onBack = viewModel::stop) {
            ChallengeSessionScreen(
                config = config.session,
                sessionId = config.sessionId,
                onFinished = viewModel::stop,
                modifier = it,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChallengeRunScaffold(onBack: () -> Unit, content: @Composable (Modifier) -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.challenge_test_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
            )
        },
    ) { padding -> content(Modifier.padding(padding)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeTestContent(
    form: ChallengeTestForm,
    availableTypes: List<ChallengeType>,
    onDifficulty: (Int) -> Unit,
    onCount: (Int) -> Unit,
    onMode: (ChallengeMode) -> Unit,
    onToggleType: (ChallengeType) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.challenge_test_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm),
        ) {
            SectionHeader(stringResource(R.string.challenge_test_difficulty))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = ApexTheme.spacing.screen)) {
                (SessionConfig.MIN_DIFFICULTY..SessionConfig.MAX_DIFFICULTY).forEachIndexed { index, level ->
                    SegmentedButton(
                        selected = level == form.difficulty,
                        onClick = { onDifficulty(level) },
                        shape = SegmentedButtonDefaults.itemShape(index, SessionConfig.MAX_DIFFICULTY),
                    ) { Text(level.toString()) }
                }
            }

            SectionHeader(stringResource(R.string.challenge_test_count))
            Row(
                Modifier.padding(horizontal = ApexTheme.spacing.screen),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.lg),
            ) {
                OutlinedIconButton(
                    onClick = { onCount(form.count - 1) },
                    enabled = form.count > SessionConfig.MIN_COUNT,
                ) { Text("-") }
                Text(form.count.toString(), style = MaterialTheme.typography.titleLarge)
                OutlinedIconButton(
                    onClick = { onCount(form.count + 1) },
                    enabled = form.count < SessionConfig.MAX_COUNT,
                ) { Text("+") }
            }

            SectionHeader(stringResource(R.string.challenge_test_mode))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = ApexTheme.spacing.screen)) {
                listOf(
                    ChallengeMode.CHAINED to stringResource(R.string.challenge_mode_chained),
                    ChallengeMode.RANDOM to stringResource(R.string.challenge_mode_random),
                ).forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = mode == form.mode,
                        onClick = { onMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                    ) { Text(label) }
                }
            }

            SectionHeader(stringResource(R.string.challenge_test_types))
            Row(
                Modifier.padding(horizontal = ApexTheme.spacing.screen),
                horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm),
            ) {
                availableTypes.forEach { type ->
                    FilterChip(
                        selected = type in form.types,
                        onClick = { onToggleType(type) },
                        label = { Text(typeLabel(type)) },
                    )
                }
            }

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().padding(horizontal = ApexTheme.spacing.screen, vertical = ApexTheme.spacing.xl),
                shape = MaterialTheme.shapes.small,
            ) { Text(stringResource(R.string.challenge_test_start)) }
        }
    }
}

@Composable
private fun typeLabel(type: ChallengeType): String = when (type) {
    ChallengeType.MATH -> stringResource(R.string.challenge_type_math)
    else -> type.id
}
