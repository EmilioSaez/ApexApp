package app.luxion.apexapp.ui.challenge

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.challenge.ChallengeAnswer
import app.luxion.apexapp.domain.challenge.ChallengeInstance
import app.luxion.apexapp.domain.challenge.ChallengeType
import app.luxion.apexapp.domain.challenge.SessionConfig
import app.luxion.apexapp.domain.challenge.SessionState
import app.luxion.apexapp.ui.adaptive.AdaptiveLevelNotice
import app.luxion.apexapp.ui.adaptive.AdaptiveNoticeViewModel
import app.luxion.apexapp.ui.components.PrimaryAction
import app.luxion.apexapp.ui.theme.ApexAppTheme
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
    val noticeViewModel: AdaptiveNoticeViewModel = hiltViewModel(key = "adaptive-$sessionId")
    LaunchedEffect(alarmId) { noticeViewModel.load(alarmId) }
    val notice by noticeViewModel.notice.collectAsStateWithLifecycle()
    state?.let {
        ChallengeSessionContent(
            state = it,
            rendererFor = viewModel::rendererFor,
            onAnswer = viewModel::submit,
            onUnavailable = viewModel::reportUnavailable,
            onFinished = onFinished,
            modifier = modifier,
            header = { notice?.let { n -> AdaptiveLevelNotice(n) } },
        )
    }
}

@Composable
fun ChallengeSessionContent(
    state: SessionState,
    rendererFor: (ChallengeType) -> ChallengeRenderer?,
    onAnswer: (ChallengeAnswer) -> Unit,
    onUnavailable: (reason: String) -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ApexTheme.spacing.screen, vertical = ApexTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.xl),
    ) {
        header()
        val current = state.current
        if (state.passed || current == null) {
            Box(Modifier.fillMaxWidth().padding(vertical = ApexTheme.spacing.xxl), contentAlignment = Alignment.Center) {
                // Éxito en el color semántico, no en el acento.
                Text(
                    stringResource(R.string.challenge_passed),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            PrimaryAction(
                text = stringResource(R.string.challenge_done),
                onClick = onFinished,
                modifier = Modifier.fillMaxWidth(),
            )
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
            rendererFor(current.type)?.Render(current, state.lastAnswerWrong, onAnswer, onUnavailable)
        }
    }
}

private val PreviewInstance = object : ChallengeInstance {
    override val type = ChallengeType.MATH
    override val level = 3
    override val prompt = "47 + 38"
    override fun check(answer: ChallengeAnswer) = false
}

@Composable
private fun SessionPreview(state: SessionState) = ChallengeSessionContent(
    state = state,
    rendererFor = { MathRenderer() },
    onAnswer = {},
    onUnavailable = {},
    onFinished = {},
)

@Preview(name = "Claro", showBackground = true, heightDp = 700)
@Composable
private fun ChallengeActiveLight() = ApexAppTheme(darkTheme = false) { SessionPreview(SessionState(solved = 1, total = 3, current = PreviewInstance, passed = false, lastAnswerWrong = true)) }

@Preview(name = "Oscuro", showBackground = true, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChallengeActiveDark() = ApexAppTheme(darkTheme = true) { SessionPreview(SessionState(solved = 1, total = 3, current = PreviewInstance, passed = false, lastAnswerWrong = true)) }

@Preview(name = "Fuente 1.3", showBackground = true, heightDp = 700, fontScale = 1.3f)
@Composable
private fun ChallengeActiveLargeFont() = ApexAppTheme(darkTheme = false) { SessionPreview(SessionState(solved = 1, total = 3, current = PreviewInstance, passed = false, lastAnswerWrong = true)) }

@Preview(name = "Claro", showBackground = true)
@Composable
private fun ChallengePassedLight() = ApexAppTheme(darkTheme = false) { SessionPreview(SessionState(solved = 3, total = 3, current = null, passed = true)) }

@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChallengePassedDark() = ApexAppTheme(darkTheme = true) { SessionPreview(SessionState(solved = 3, total = 3, current = null, passed = true)) }

@Preview(name = "Fuente 1.3", showBackground = true, fontScale = 1.3f)
@Composable
private fun ChallengePassedLargeFont() = ApexAppTheme(darkTheme = false) { SessionPreview(SessionState(solved = 3, total = 3, current = null, passed = true)) }
