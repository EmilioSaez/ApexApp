package app.luxion.apexapp.ui.anticheat

import androidx.annotation.StringRes
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.luxion.apexapp.R
import app.luxion.apexapp.anticheat.Capabilities
import app.luxion.apexapp.domain.AntiCheatLevel
import app.luxion.apexapp.domain.AntiCheatPolicy
import app.luxion.apexapp.ui.components.SectionHeader
import app.luxion.apexapp.ui.theme.ApexAppTheme
import app.luxion.apexapp.ui.theme.ApexTheme

/** Controles del editor de alarma: nivel anti-trampas y política de snooze. */
@Composable
fun AntiCheatSection(
    policy: AntiCheatPolicy,
    onChange: (AntiCheatPolicy) -> Unit,
    onOpenStatus: () -> Unit,
    modifier: Modifier = Modifier,
    capabilities: Capabilities = Capabilities(overlay = true, deviceOwner = true),
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
        SectionHeader(stringResource(R.string.anticheat_section), horizontalPadding = 0.dp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ApexTheme.spacing.sm)) {
            AntiCheatLevel.entries.forEach { level ->
                FilterChip(
                    selected = policy.level == level,
                    onClick = { onChange(policy.copy(level = level)) },
                    shape = MaterialTheme.shapes.small,
                    label = { Text(stringResource(level.labelRes())) },
                )
            }
        }
        Text(
            stringResource(policy.level.hintRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (policy.level != AntiCheatLevel.OFF && !capabilities.overlay) {
            MissingNote(R.string.anticheat_missing_overlay)
        }
        Row(
            Modifier.fillMaxWidth().toggleable(
                value = policy.snooze.allowed,
                role = Role.Switch,
                onValueChange = { onChange(policy.copy(snooze = policy.snooze.copy(allowed = it))) },
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.anticheat_snooze_allowed), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = policy.snooze.allowed, onCheckedChange = null)
        }
        if (policy.snooze.allowed) {
            Text(stringResource(R.string.anticheat_snooze_minutes, policy.snooze.minutes), style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = policy.snooze.minutes.toFloat(),
                onValueChange = { onChange(policy.copy(snooze = policy.snooze.copy(minutes = it.toInt()))) },
                valueRange = 1f..30f,
            )
            Text(stringResource(R.string.anticheat_snooze_max, policy.snooze.maxCount), style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = policy.snooze.maxCount.toFloat(),
                onValueChange = { onChange(policy.copy(snooze = policy.snooze.copy(maxCount = it.toInt()))) },
                valueRange = 1f..10f,
            )
        }
        TextButton(onClick = onOpenStatus) { Text(stringResource(R.string.anticheat_open_status)) }
    }
}

@StringRes
private fun AntiCheatLevel.labelRes() = when (this) {
    AntiCheatLevel.OFF -> R.string.anticheat_level_off
    AntiCheatLevel.STANDARD -> R.string.anticheat_level_standard
}

@StringRes
private fun AntiCheatLevel.hintRes() = when (this) {
    AntiCheatLevel.OFF -> R.string.anticheat_level_hint_off
    AntiCheatLevel.STANDARD -> R.string.anticheat_level_hint_standard
}

@Composable
private fun MissingNote(@StringRes text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}

@Preview(name = "Claro", showBackground = true, heightDp = 700)
@Composable
private fun AntiCheatLight() = ApexAppTheme(darkTheme = false) { Surface { AntiCheatSection(policy = AntiCheatPolicy(level = AntiCheatLevel.STANDARD), onChange = {}, onOpenStatus = {}, capabilities = Capabilities(overlay = false, deviceOwner = false), modifier = Modifier.padding(ApexTheme.spacing.screen)) } }

@Preview(name = "Oscuro", showBackground = true, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AntiCheatDark() = ApexAppTheme(darkTheme = true) { Surface { AntiCheatSection(policy = AntiCheatPolicy(level = AntiCheatLevel.STANDARD), onChange = {}, onOpenStatus = {}, capabilities = Capabilities(overlay = false, deviceOwner = false), modifier = Modifier.padding(ApexTheme.spacing.screen)) } }

@Preview(name = "Fuente 1.3", showBackground = true, heightDp = 700, fontScale = 1.3f)
@Composable
private fun AntiCheatLargeFont() = ApexAppTheme(darkTheme = false) { Surface { AntiCheatSection(policy = AntiCheatPolicy(level = AntiCheatLevel.STANDARD), onChange = {}, onOpenStatus = {}, capabilities = Capabilities(overlay = false, deviceOwner = false), modifier = Modifier.padding(ApexTheme.spacing.screen)) } }
