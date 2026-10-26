package app.luxion.apexapp.ui.ring

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.domain.challenge.SessionConfig
import app.luxion.apexapp.ui.challenge.ChallengeSessionScreen
import app.luxion.apexapp.ui.components.PrimaryAction
import app.luxion.apexapp.ui.components.TimeDisplay
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme

@Composable
fun RingScreen(onFinished: () -> Unit, viewModel: RingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onFinished() }
    val alarm = state.alarm ?: return
    val challenges = state.challenges
    if (challenges == null) {
        RingContent(alarm, state.canDismiss, viewModel::dismiss, state.canSnooze, viewModel::snooze)
    } else {
        // Al superar la sesión el gate la reconoce y dismiss() apaga la alarma; sin superarla no hay salida.
        RingChallenges(
            alarm = alarm,
            config = challenges,
            sessionId = state.sessionId ?: "ring-${alarm.id}",
            onPassed = viewModel::dismiss,
            canSnooze = state.canSnooze,
            onSnooze = viewModel::snooze,
        )
    }
}

/** Retos de la alarma que suena, con posponer disponible si la política lo permite. */
@Composable
fun RingChallenges(
    alarm: Alarm,
    config: SessionConfig,
    sessionId: String,
    onPassed: () -> Unit,
    canSnooze: Boolean,
    onSnooze: () -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ChallengeSessionScreen(
                config = config,
                sessionId = sessionId,
                onFinished = onPassed,
                modifier = Modifier.weight(1f),
                alarmId = alarm.id,
            )
            if (canSnooze) {
                SnoozeButton(
                    alarm,
                    onSnooze,
                    Modifier.fillMaxWidth().padding(horizontal = ApexTheme.spacing.screen).padding(bottom = ApexTheme.spacing.xl),
                )
            }
        }
    }
}

@Composable
private fun SnoozeButton(alarm: Alarm, onSnooze: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onSnooze,
        modifier = modifier.defaultMinSize(minHeight = ApexTheme.spacing.xxxl),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(stringResource(R.string.ring_snooze, alarm.antiCheat.snooze.minutes), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun RingContent(
    alarm: Alarm,
    canDismiss: Boolean,
    onDismiss: () -> Unit,
    canSnooze: Boolean = false,
    onSnooze: () -> Unit = {},
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        // La hora manda arriba, anclada a la izquierda; las acciones quedan al alcance del pulgar.
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = ApexTheme.spacing.screen, vertical = ApexTheme.spacing.xl),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    alarm.label ?: stringResource(R.string.alarm_default_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                TimeDisplay(alarm.hour, alarm.minute, large = true)
            }
            Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.md)) {
                if (canDismiss) {
                    PrimaryAction(
                        text = stringResource(R.string.ring_dismiss),
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (canSnooze) {
                    SnoozeButton(alarm, onSnooze, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun RingPreview() = RingContent(
    alarm = Alarm(id = 1, hour = 6, minute = 30, label = "Gimnasio"),
    canDismiss = true,
    onDismiss = {},
    canSnooze = true,
)

@Preview(name = "Claro", showBackground = true)
@Composable
private fun RingLight() = ApexAppTheme(darkTheme = false) { RingPreview() }

@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RingDark() = ApexAppTheme(darkTheme = true) { RingPreview() }

@Preview(name = "Fuente 1.3", showBackground = true, fontScale = 1.3f)
@Composable
private fun RingLargeFont() = ApexAppTheme(darkTheme = false) { RingPreview() }
