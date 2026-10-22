package app.luxion.apexapp.ui.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.SessionConfig
import app.luxion.apexapp.domain.challenge.SessionState
import app.luxion.apexapp.ui.theme.ApexTheme

/**
 * Ejecuta una sesión de retos. Es reutilizable: la pantalla de prueba la usa ahora y el flujo real de
 * alarma la alojará después, pasando el [sessionId] que crea AlarmFiringHandler. [onFinished] se
 * llama al pulsar el botón de la pantalla de sesión superada.
 */
@Composable
fun ChallengeSessionScreen(
    config: SessionConfig,
    sessionId: String,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    alarmId: Long? = null,
    // Una sesión por sessionId: al empezar otra se crea un ViewModel nuevo.
    viewModel: ChallengeSessionViewModel = hiltViewModel(key = sessionId),
) {
    LaunchedEffect(config, sessionId) { viewModel.start(config, sessionId, alarmId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    state?.let {
        ChallengeSessionContent(
            state = it,
            rendererFor = viewModel::rendererFor,
            onAnswer = viewModel::submit,
            onFinished = onFinished,
            modifier = modifier,
        )
    }
}

@Composable
fun ChallengeSessionContent(
    state: SessionState,
    rendererFor: (ChallengeType) -> ChallengeRenderer?,
    onAnswer: (ChallengeAnswer) -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ApexTheme.spacing.screen, vertical = ApexTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xl),
    ) {
        val current = state.current
        if (state.passed || current == null) {
            Box(Modifier.fillMaxWidth().padding(vertical = ApexTheme.spacing.xxl), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.challenge_passed), style = MaterialTheme.typography.headlineMedium)
            }
            Button(onClick = onFinished, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                Text(stringResource(R.string.challenge_done))
            }
        } else {
            Text(
                stringResource(R.string.challenge_progress, state.solved + 1, state.total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { state.solved.toFloat() / state.total },
                modifier = Modifier.fillMaxWidth(),
            )
            rendererFor(current.type)?.Render(current, state.lastAnswerWrong, onAnswer)
        }
    }
}
