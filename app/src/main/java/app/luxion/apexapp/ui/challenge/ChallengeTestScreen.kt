package app.luxion.apexapp.ui.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.ChallengeMode
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.SessionConfig
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
            typeLabel = { viewModel.rendererFor(it)?.label() ?: it.id },
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
    typeLabel: @Composable (ChallengeType) -> String,
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
            DifficultySelector(form.difficulty, onChange = onDifficulty)
            CountStepper(form.count, onChange = onCount, minCount = SessionConfig.minCount(form.types.size))
            ModeSelector(form.mode, onChange = onMode)
            TypeChips(availableTypes, form.types, typeLabel, onToggleType)

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().padding(horizontal = ApexTheme.spacing.screen, vertical = ApexTheme.spacing.xl),
                shape = MaterialTheme.shapes.small,
            ) { Text(stringResource(R.string.challenge_test_start)) }
        }
    }
}
