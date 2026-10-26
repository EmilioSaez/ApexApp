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
import app.luxion.apexapp.ui.components.PrimaryAction
import app.luxion.apexapp.ui.components.TimeDisplay
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme

@Composable
fun RingScreen(onFinished: () -> Unit, viewModel: RingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onFinished() }
    state.alarm?.let {
        RingContent(it, state.canDismiss, viewModel::dismiss, state.canSnooze, viewModel::snooze)
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
                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = ApexTheme.spacing.xxxl),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(stringResource(R.string.ring_snooze, alarm.antiCheat.snooze.minutes), style = MaterialTheme.typography.labelLarge)
                    }
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
