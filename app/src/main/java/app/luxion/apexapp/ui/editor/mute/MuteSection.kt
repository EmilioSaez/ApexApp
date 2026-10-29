package app.luxion.apexapp.ui.editor.mute

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.luxion.apexapp.R
import app.luxion.apexapp.domain.Alarm
import app.luxion.apexapp.ui.components.SectionHeader
import app.luxion.apexapp.ui.editor.EditorSection
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme
import javax.inject.Inject

/** Sección «Silencio en los retos» del editor; va entre Sonido (200) y Anti-trampas (300). */
class MuteSection @Inject constructor() : EditorSection {
    override val order = 250

    @Composable
    override fun Content(draft: Alarm, onChange: (Alarm) -> Unit) {
        // Sin retos no hay nada que resolver: la opción no tendría efecto.
        if (draft.challenges.challengesEnabled || draft.challenges.challenges.isNotEmpty()) {
            MuteSettingsSection(draft, onChange)
        }
    }
}

private val InactivityOptions = listOf(15, 30, 60, 120)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MuteSettingsSection(alarm: Alarm, onChange: (Alarm) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
        SectionHeader(stringResource(R.string.mute_section), horizontalPadding = 0.dp)
        Row(
            Modifier.fillMaxWidth().toggleable(
                value = alarm.muteDuringChallenges,
                role = Role.Switch,
                onValueChange = { onChange(alarm.copy(muteDuringChallenges = it)) },
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.mute_toggle), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = alarm.muteDuringChallenges, onCheckedChange = null)
        }
        Text(
            stringResource(R.string.mute_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AnimatedVisibility(alarm.muteDuringChallenges) {
            Column(verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
                Text(stringResource(R.string.mute_inactivity), style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    InactivityOptions.forEachIndexed { index, seconds ->
                        SegmentedButton(
                            selected = alarm.muteInactivitySeconds == seconds,
                            onClick = { onChange(alarm.copy(muteInactivitySeconds = seconds)) },
                            shape = SegmentedButtonDefaults.itemShape(index, InactivityOptions.size, MaterialTheme.shapes.small),
                        ) { Text(stringResource(R.string.mute_inactivity_option, seconds)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Sample(on: Boolean) = ApexAppTheme {
    MuteSettingsSection(Alarm(hour = 7, minute = 0, muteDuringChallenges = on), onChange = {})
}

@Preview(name = "Activa", showBackground = true)
@Composable
private fun MuteOnPreview() = Sample(true)

@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MuteOffDarkPreview() = Sample(false)

@Preview(name = "Fuente 1.3", showBackground = true, fontScale = 1.3f)
@Composable
private fun MuteLargeFontPreview() = Sample(true)
